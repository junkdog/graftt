package net.onedaybeard.graftt

import org.junit.Test
import kotlin.test.assertEquals

class IntegrationTests {

    @Test
    fun `something with bad toString`() {
        assertEquals("3: hello", SomethingWithBadToString().toString())
    }

    @Test
    fun `counting invocations`() {
        fun Any.invocations(): Int = CountingInvocations::class.java
            .declaredFields
            .first { it.name == "count" }
            .get(this) as Int

        val o = CountingInvocations()
        assertEquals(0, o.invocations())
        o.callMe()
        o.callMe()
        assertEquals(2, o.invocations())
    }

    @Test
    fun `never throw from method`() {
        val o = NeverThrowFromMethod()
        o.maybeDangerous()
    }
}