package net.onedaybeard.graftt

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.asm.classNode
import net.onedaybeard.graftt.asm.toBytes
import org.junit.Assert
import org.junit.Test
import java.lang.reflect.Field
import kotlin.test.assertEquals

class GraftTests {

    @Test
    fun `referenced _other_ transplants are substituted by recipient type`() {
        val (bar, foo)  = transplantsOf(
            TransplantSubstitution.BarTransplant::class,
            TransplantSubstitution.FooTransplant::class)

        instantiate(bar, foo) { (_, foo) ->
            foo.invokeMethod<Unit>("init")
            foo.invokeMethod<Unit>("checkBars")
        }
    }

    @Test
    fun `putfield, getfield for mocked field denoting other transplant translates to its recipient`() {
        val (mocked, foo)  = transplantsOf(
            MockedFieldOfTransplant.OriginalTransplant::class,
            MockedFieldOfTransplant.FooTransplant::class)

        instantiate(mocked, foo) { (_, foo) ->
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

    @Test
    fun `field modifiers can be changed by fusing`() {
        val recipient = transplant<FieldModifiers.FooTransplant>()

        instantiate(recipient) {
            val fields = this::class.java.declaredFields.associateBy(Field::getName)

            fun fieldModifiers(name: String) = fields[name]!!
                .toString()
                .replace(Regex("[\\w.]*\\$"), "")
                .split(" ")
                .dropLast(1)
                .joinToString(" ")

            assertEquals("volatile int",             fieldModifiers("a"))
            assertEquals("transient int",            fieldModifiers("b"))
            assertEquals("public static Foo",        fieldModifiers("c"))
            assertEquals("private static final int", fieldModifiers("d"))
        }
    }

    @Test
    fun `donor is unchanged by surgery`() {
        val donor = classNode<AnnotationFusing.BarTransplant>()
        transplant(donor)
            .onFailure(`(╯°□°）╯︵ ┻━┻`)
            .onSuccess {
                Assert.assertArrayEquals(
                    classNode<AnnotationFusing.BarTransplant>().toBytes(),
                    donor.toBytes())
            }
    }
}
