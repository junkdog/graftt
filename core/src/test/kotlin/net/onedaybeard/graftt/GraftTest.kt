package net.onedaybeard.graftt

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.graft.*
import org.junit.Test
import kotlin.test.fail

class GraftTest {

    @Test
    fun `fuse no args methods`() {
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
}
