package net.onedaybeard.graftt

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.asm.*
import net.onedaybeard.graftt.graft.Transplant
import org.junit.Test
import org.objectweb.asm.Type
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FnTests {

    @Test
    fun `read types from Graft|@Annotations#remove`() {
        val types = classNode<AnnotationFusing.FooTransplant>()
            .let { it.methods.first { f -> f.name == "a" } }
            .annotation<Graft.Annotations>()
            .andThen { it.get<ArrayList<Type>>("remove") }
            .onFailure(`(╯°□°）╯︵ ┻━┻`)
            .unwrap()

        assertEquals(
            setOf(type<AnnotationFusing.MyAnno>(), type<AnnotationFusing.MyAnnoRt>()),
            types.toSet())
    }

    @Test
    fun `check transplant overwriteAnnotations`() {
        val overwrite = classNode<AnnotationFusing.FooTransplant>()
            .let { it.methods.first { f -> f.name == "b" } }
            .let { Transplant.Method("yolo", it, SimpleRemapper(mapOf())) }
            .overwriteAnnotations()

        assertTrue(overwrite)
    }
}
