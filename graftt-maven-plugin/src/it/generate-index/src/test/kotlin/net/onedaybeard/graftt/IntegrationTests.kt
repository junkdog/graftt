package net.onedaybeard.graftt

import org.junit.Test
import kotlin.test.assertEquals

class IntegrationTests {

    @Test
    fun `reading transplants from index`() {
        val found = IntegrationTests::class.java
            .getResourceAsStream("/graftt.transplants")
            .readBytes()
            .let { String(it) }
            .lines()
            .sorted()

        val expected = listOf(
            BarTransplant::class.qualifiedName,
            FooTransplant::class.qualifiedName
        )

        assertEquals(expected, found)
    }
}