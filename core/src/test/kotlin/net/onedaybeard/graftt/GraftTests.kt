package net.onedaybeard.graftt

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.graft.*
import org.junit.Test
import kotlin.test.fail

class GraftTests {

    @Test
    fun `fuse a simple method without arguments`() {
        val donor = classNode<SingleClassMethodTransplant>()

        val recipient = performGraft(donor)
            .onFailure { fail(it.toString()) }

        instantiate(recipient.unwrap()) {
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
        fail()
    }
}
