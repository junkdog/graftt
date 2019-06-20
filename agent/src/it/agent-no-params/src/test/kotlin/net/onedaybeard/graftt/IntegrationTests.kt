package net.onedaybeard.graftt

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// instantiate the transplants to have them registered by the agent
fun registerTransplants() {
    RetrofitInterface.FooTransplant()
    RetrofitInterface2.FooTransplant()
    MeasureExecutionTime.FooTransplant()
}

class IntegrationTests {
    companion object {
        init { registerTransplants() } // do this as early as possible
    }

    @Test
    fun `retrofit interface`() {
        assertEquals(0xf00, (RetrofitInterface.Foo() as RetrofitInterface.Bar).hi())
    }

    @Test
    fun `retrofit interface when method already exists`() {
        assertNotNull(RetrofitInterface2.Foo() as? RetrofitInterface2.Bar)
    }

    @Test
    fun `measure method execution time`() {
        var timerCalled = false

        MeasureExecutionTime.TimerHolder.timer = MeasureExecutionTime.Timer { name, ms ->
            assertEquals("expensiveOperation", name)
            assertEquals(5, (ms + 50) / 100)
            timerCalled = true
        }

        MeasureExecutionTime.Foo().expensiveOperation()

        assertTrue(timerCalled)
    }
}