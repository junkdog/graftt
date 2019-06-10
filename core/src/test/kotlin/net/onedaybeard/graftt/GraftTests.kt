package net.onedaybeard.graftt

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
                invokeMethod<String>("withPrependField", listOf("birb")))
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
}

