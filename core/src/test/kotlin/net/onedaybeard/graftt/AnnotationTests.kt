package net.onedaybeard.graftt

import net.onedaybeard.graftt.asm.*
import net.onedaybeard.graftt.graft.Transplant
import net.onedaybeard.graftt.graft.annotationsToRemove
import net.onedaybeard.graftt.graft.overwriteAnnotations
import org.junit.Test
import org.objectweb.asm.commons.SimpleRemapper
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnnotationTests {

    @Test
    fun `read types from Graft|@Annotations#remove`() {
        val types = classNode<AnnotationFusing.FooTransplant>()
            .let { it.methods.first { f -> f.name == "a" } }
            .annotationsToRemove()

        assertEquals(
            setOf(type<AnnotationFusing.MyAnno>(), type<AnnotationFusing.MyAnnoRt>()),
            types.toSet())
    }

    @Test
    fun `check transplant overwriteAnnotations`() {
        val overwrite = classNode<AnnotationFusing.FooTransplant>()
            .let { it.methods.first { f -> f.name == "b" } }
            .let { Transplant.Method("yolo", it, SimpleRemapper(mapOf())) }
            .node.overwriteAnnotations

        assertTrue(overwrite)
    }
}
