package net.onedaybeard.graftt

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.graft.transplant
import org.junit.Test
import kotlin.test.assertEquals

class GraftTests {

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
        resultOf { classNode<DeclaredFieldBrokenFieldTransplant>() } // donor
            .andThen { donor -> transplant(donor, ::loadClassNode) } // to recipient
            .assertErr(Msg.FieldDefaultValueNotSupported(
                "net/onedaybeard/graftt/DeclaredFieldBrokenFieldTransplant", "name"))
    }

    @Test
    fun `transplanted must not extend any base class`() {
        resultOf { classNode<DeclaredFieldBrokenParentTransplant>() } // donor
            .andThen { donor -> transplant(donor, ::loadClassNode) } // to recipient
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
        resultOf { classNode<AlreadyHaveInterfaceTransplant>() }     // donor
            .andThen { donor -> transplant(donor, ::loadClassNode) } // to recipient
            .assertErr(Msg.InterfaceAlreadyExists(
                "net/onedaybeard/graftt/AlreadyHaveInterface",
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
}


