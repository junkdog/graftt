package net.onedaybeard.graftt

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
        fail()
    }

    @Test
    fun `mocked fields are ignored during transplant`() {
        fail()
    }

    @Test
    fun `declared methods are transplanted to recipient`() {
        fail()
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

