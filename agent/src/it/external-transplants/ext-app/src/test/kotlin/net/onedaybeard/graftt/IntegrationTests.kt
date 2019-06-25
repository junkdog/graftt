package net.onedaybeard.graftt

import com.badlogic.gdx.math.Vector2
import org.junit.Test
import kotlin.test.assertEquals

class IntegrationTests {

    @Test
    fun `transplanting gdx Vector2f`() {
        assertEquals(
            "overloaded: (1.0,2.0)",
            Vector2(1f, 2f).toString())
    }
}
