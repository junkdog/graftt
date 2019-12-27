package net.onedaybeard.graftt

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.asm.*
import org.junit.Test
import kotlin.test.assertEquals

class GraftTests {

    @Test
    fun `annotations from original method are retained on transplanted`() {
        val mn = transplant<AnnotationFusing.BarTransplant>()
            .map { cn -> cn.methods.first { it.name == "hmm" } }
            .unwrap()

        assertEquals(
            setOf(type<AnnotationFusing.MyAnno>(), type<AnnotationFusing.MyAnnoRt>()),
            mn.annotations().asTypes())
    }

    @Test
    fun `fuse annotations on method`() {
        TODO()
    }

    @Test
    fun `fuse fields with annotations`() {
        transplant<FusedField.FooTransplant>()
            .onFailure(`(╯°□°）╯︵ ┻━┻`)
            .onSuccess { cn ->
                val f = cn.fields.first { it.name == "hmm" }
                assertEquals(
                    setOf(type<FusedField.Yolo>(), type<FusedField.Yolo2>()),
                    f.annotations().asTypes())

                val f2 = cn.fields.first { it.name == "transplantedWithAnnotation" }
                assertEquals(
                    setOf(type<FusedField.Yolo>()),
                    f2.annotations().asTypes())
            }
    }

    @Test
    fun `detect annotation clash on method`() {
        transplant<AnnotationFusing.ClashingMethodTransplant>()
           .assertErr(Msg.AnnotationAlreadyExists(
               name = "net/onedaybeard/graftt/AnnotationFusing.ClashingMethodTransplant",
               anno = "net/onedaybeard/graftt/AnnotationFusing.MyAnnoRt",
               symbol = "hmm"))
    }

    @Test
    fun `detect annotation clash on field`() {
        transplant<AnnotationFusing.ClashingFieldTransplant>()
           .assertErr(Msg.AnnotationAlreadyExists(
               name = "net/onedaybeard/graftt/AnnotationFusing\$ClashingFieldTransplant",
               anno = "net/onedaybeard/graftt/AnnotationFusing\$MyAnno",
               symbol = "usch"))
    }

    @Test
    fun `detect annotation clash on class`() {
        TODO()
//        transplant<AnnotationFusing.ClashingClassTransplant>()
//           .assertErr(Msg.AnnotationAlreadyExists(
//               name = "net/onedaybeard/graftt/AnnotationFusing.ClashingClassTransplant",
//               anno = "net/onedaybeard/graftt/AnnotationFusing.MyAnno",
//               symbol = "ClashingClass"))
    }

    @Test
    fun `referenced _other_ transplants are substituted by recipient type`() {
        val (bar, foo)  = transplantsOf(
            TransplantSubstitution.BarTransplant::class,
            TransplantSubstitution.FooTransplant::class)

        instantiate(bar, foo) { (bar, foo) ->
            foo.invokeMethod<Unit>("init")
            foo.invokeMethod<Unit>("checkBars")
        }
    }

    @Test
    fun `putfield, getfield for mocked field denoting other transplant translates to its recipient`() {
        val (mocked, foo)  = transplantsOf(
            MockedFieldOfTransplant.OriginalTransplant::class,
            MockedFieldOfTransplant.FooTransplant::class)

        instantiate(mocked, foo) { (mocked, foo) ->
            val result = foo.invokeMethod<String>("doIt", listOf("String"))
            assertEquals("String: true", result)
        }
    }

    @Test
    fun `fuse a simple method without arguments`() {
        val recipient = transplant<SingleClassMethodTransplant>()

        instantiate(recipient) {
            assertMethodExists("yolo")
            assertMethodExists("yolo\$original")

            invokeMethod<Unit>("yo",
                observeField("invokedWithTransplant", false to true),
                observeField("yoloCalled",            false to true))
        }
    }

    @Test
    fun `declared fields are transplanted to recipient`() {
        val recipient = transplant<DeclaredFieldTransplant>()

        // TODO: support injecting into initializer
        instantiate(recipient) {
            assertEquals("william null", invokeMethod("yolo")!!)
        }
    }

    @Test
    fun `transplanted fields must not be initialized to a value`() {
        transplant<DeclaredFieldBrokenFieldTransplant>()
            .assertErr(Msg.FieldDefaultValueNotSupported(
                "net/onedaybeard/graftt/DeclaredFieldBrokenFieldTransplant", "name"))
    }

    @Test
    fun `transplanted fields must not be initialized to a value in clinit`() {
        transplant<DeclaredFieldBrokenFieldTransplant2>()
            .assertErr(Msg.FieldDefaultValueNotSupported(
                "net/onedaybeard/graftt/DeclaredFieldBrokenFieldTransplant2", "time"))
    }

    @Test
    fun `transplanted static fields can be assigned a simple default value`() {
        val recipient = transplant<DeclaredFieldStaticFieldWithValueTransplant>()
        assertEquals("blake", instantiate(recipient).toString())
    }

    @Test
    fun `transplanted must not extend any base class`() {
        transplant<DeclaredFieldBrokenParentTransplant>()
            .andThen { donor -> transplant(donor) }
            .assertErr(Msg.TransplantMustNotExtendClass(
                "net/onedaybeard/graftt/DeclaredFieldBrokenParentTransplant"))
    }

    @Test
    fun `mocked fields are ignored during transplant`() {
        val recipient = transplant<MockedFieldTransplant>()

        instantiate(recipient) {
            assertEquals(
                "54321 birb",
                invokeMethod("withPrependField", listOf("birb"))!!)
        }
    }

    @Test
    fun `declared methods are transplanted to recipient`() {
        val recipient = transplant<DeclaredMethodTransplant>()

        instantiate(recipient) {
            val omg = invokeMethod<String>("toUpperCase", listOf("omg"))
            assertEquals("OMG!!!", omg)
        }
    }

    @Test
    fun `mocked methods are ignored during transplant`() {
        val recipient = transplant<MockedMethodTransplant>()

        instantiate(recipient) {
            val result = invokeMethod<String>("withMethod", listOf("hello"))
            assertEquals("AAA hello AAA", result)
        }
    }

    @Test
    fun `override final toString`() {
        val recipient = transplant<SomethingWithBadToStringTransplant>()

        instantiate(recipient) {
            val result = invokeMethod<String>("toString")
            assertEquals("3: hello", result)
        }
    }

    @Test
    fun `override method in base class`() {
        val recipient = transplant<BarImplTransplant>()
        instantiate(recipient) {
            assertEquals("hi", toString())
        }
    }

    @Test
    fun `fusing without calling original method deletes it`() {
        val recipient = transplant<ReplaceOriginalTransplant>()
        instantiate(recipient) {
            assertEquals(true, invokeMethod("hmm")!!)
            assertEquals(1, this::class.java.declaredMethods.size)
        }
    }

    @Test
    fun `interfaces from transplant are added to recipient`() {
        val recipient = transplant<WantInterfacesTransplant>()
        val p = instantiate(recipient) as Point
        assertEquals(1, p.x())
        assertEquals(2, p.y())
    }

    @Test
    fun `method visibility changed when retrofitting interface`() {
        val recipient = transplant<PromoteVisibilityWhenRetrofittingInterface.FooTransplant>()
        val p = instantiate(recipient) as Point
        assertEquals(2, p.x())
        assertEquals(4, p.y())
        assertEquals("x: 2, y: 4", p.toString())
    }

    @Test @Suppress("UNCHECKED_CAST")
    fun `generic interfaces transplanted to recipient`() {
        val recipient = transplant<RetrofitGenericInterface.FooTransplant>()
        val p = instantiate(recipient) as InterfaceT<Boolean>
        assertEquals(true, p.helloT())
    }

    @Test
    fun `fail when transplanting interfaces already present on recipient`() {
        transplant<AlreadyHaveInterfaceTransplant>()
            .assertErr(Msg.InterfaceAlreadyExists(
                "net/onedaybeard/graftt/AlreadyHaveInterfaceTransplant",
                "net/onedaybeard/graftt/Point"))
    }

    @Test
    fun `mocks can refer to method in recipients parent class`() {
        val recipient = transplant<MockParentMethodImplTransplant>()
        assertEquals(0xf00.toString(), instantiate(recipient).toString())
    }

    @Test
    fun `mocks can refer to field in recipients parent class`() {
        val recipient = transplant<MockParentFieldImplTransplant>()
        assertEquals(0xba4.toString(), instantiate(recipient).toString())
    }

    @Test
    fun `fail when transplanting already existing field`() {
        transplant<SingleClassFieldAlreadyExistsTransplant>()
            .assertErr(Msg.FieldAlreadyExists(
                "net/onedaybeard/graftt/SingleClassFieldAlreadyExistsTransplant",
                "yoloCalled"))
    }

    @Test
    fun `fail when transplanting already existing method`() {
        transplant<SingleClassMethodAlreadyExistsTransplant>()
            .assertErr(Msg.MethodAlreadyExists(
                "net/onedaybeard/graftt/SingleClassMethodAlreadyExistsTransplant",
                "yolo"))
    }

    @Test
    fun `fail when fused method signature is wrong`() {
        transplant<SingleClassWrongFuseTransplant>()
            .assertErr(Msg.WrongFuseSignature(
                "net/onedaybeard/graftt/SingleClassWrongFuseTransplant",
                "yolo"))
    }

    @Test
    fun `fail when fused field signature is wrong`() {
        transplant<FusedField.FooWrongSigTransplant>()
            .assertErr(Msg.WrongFuseSignature(
                name = "net/onedaybeard/graftt/FusedField\$FooWrongSigTransplant",
                symbol = "ohNo"
            ))
    }
}
