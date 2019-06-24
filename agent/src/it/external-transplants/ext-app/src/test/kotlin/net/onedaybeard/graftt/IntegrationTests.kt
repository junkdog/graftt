package net.onedaybeard.graftt

import com.badlogic.gdx.math.Vector2
import org.junit.Test
import kotlin.test.assertEquals

class IntegrationTests {

    @Test
    fun `transplanting gdx Vector2f`() {
        val v = makeVector(1f, 2f)
        assertEquals("overloaded: (1.0,2.0)", v.toString())
    }

}

fun makeVector(x: Float, y: Float) = Vector2(x, y)
