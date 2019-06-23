package net.onedaybeard.graftt

import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import net.onedaybeard.graftt.graft.performGraft
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

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

        // N.B. field values are only copied over for primitive types
        // TODO: support injecting into initializer
        instantiate(recipient) {
            assertEquals("william null", invokeMethod("yolo")!!)
        }
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
    fun `interfaces from transplant are added to transplant`() {
        val recipient = transplant<WantInterfacesTransplant>()
        val p = instantiate(recipient) as Point
        assertEquals(1, p.x())
        assertEquals(2, p.y())
    }

    @Test
    fun `generic interfaces transplanted to recipient`() {
        val recipient = transplant<RetrofitGenericInterface.FooTransplant>()
        val p = instantiate(recipient) as InterfaceT<Boolean>
        assertEquals(true, p.helloT())
    }

    @Test
    fun `fail when transplanting interfaces already on recipient class`() {
        resultOf { classNode<AlreadyHaveInterfaceTransplant>() }       // donor
            .andThen { donor -> performGraft(donor, ::loadClassNode) } // to recipient
            .onFailure { assertEquals(Msg.InterfaceAlreadyExists::class, it::class) }
            .onSuccess { fail("copying already implemented interfaces to recipient must fail") }
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


