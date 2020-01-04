package net.onedaybeard.graftt

import net.onedaybeard.graftt.asm.*
import net.onedaybeard.graftt.graft.Transplant
import net.onedaybeard.graftt.graft.graftableMethods
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnnotationTests {

    @Test
    fun `read types from Graft|@Annotations#remove`() {
        val types = classNode<AnnotationFusing.FooTransplant>()
            .graftableMethods()
            .first { it.name == "a" }
            .let { Transplant.Method("", it) }
            .annotationsToRemove()

        assertEquals(
            setOf(type<AnnotationFusing.MyAnno>(), type<AnnotationFusing.MyAnnoRt>()),
            types.toSet())
    }

    @Test
    fun `check transplant overwriteAnnotations`() {
        val overwrite = classNode<AnnotationFusing.FooTransplant>()
            .graftableMethods()
            .first { it.name == "b" }
            .let { Transplant.Method("", it) }
            .overwriteAnnotations

        assertTrue(overwrite)
    }
}
