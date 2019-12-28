package net.onedaybeard.graftt

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.asm.*
import org.junit.Test
import org.objectweb.asm.tree.MethodNode
import kotlin.test.assertEquals

class GraftAnnotationTests {

    @Test
    fun `fuse annotations on fields`() {
        transplant<FusedField.FooTransplant>()
            .onFailure(`(╯°□°）╯︵ ┻━┻`)
            .onSuccess { cn ->
                val f = cn.fields.first { it.name == "hmm" }
                assertEquals(
                    setOf(type<FusedField.Yolo>(), type<FusedField.Yolo2>()),
                    f.annotations().asTypes())

                val f2 = cn.fields.first { it.name == "transplantedWithAnnotation" }
                assertEquals(
                    setOf(type<FusedField.Yolo>()),
                    f2.annotations().asTypes())
            }
    }

    @Test
    fun `fuse annotations on method`() {
        val mn = transplant<AnnotationFusing.BarTransplant>()
            .map { cn -> cn.methods.first { it.name == "hmm" } }
            .unwrap()

        assertEquals(
            setOf(type<AnnotationFusing.MyAnno>(), type<AnnotationFusing.MyAnnoRt>()),
            mn.annotations().asTypes())
    }

    @Test
    fun `fuse annotations on class`() {
        TODO()
    }

    @Test
    fun `remove annotation from class`() {
        TODO()
    }

    @Test
    fun `remove annotation from method`() {
        fun MethodNode.readMyAnnoRt(): Int? {
            return annotation<AnnotationFusing.MyAnnoRt>()
                    .andThen { it.get<Int>("value") }
                    .get()
        }

        transplant<AnnotationFusing.FooTransplant>()
            .onFailure(`(╯°□°）╯︵ ┻━┻`)
            .onSuccess { cn ->
                val funA = cn.methods.first { it.name == "a" }
                assertEquals(
                    setOf(type<AnnotationFusing.MyAnnoRt>()),
                    funA.annotations().asTypes())
                assertEquals(1, funA.readMyAnnoRt())

                val funB = cn.methods.first { it.name == "b" }
                assertEquals(1, funB.readMyAnnoRt())
                assertEquals(
                    setOf(type<AnnotationFusing.MyAnnoRt>()),
                    funB.annotations().asTypes())

            }
    }

    @Test
    fun `remove annotation from field`() {
        transplant<FusedField.FooRemoverTransplant>()
            .onFailure(`(╯°□°）╯︵ ┻━┻`)
            .onSuccess { cn ->
                val f = cn.fields.first { it.name == "hmm" }
                assertEquals(
                    setOf(),
                    f.annotations().asTypes())
            }
    }

    @Test
    fun `detect annotation clash on method`() {
        transplant<AnnotationFusing.ClashingMethodTransplant>()
           .assertErr(Msg.AnnotationAlreadyExists(
               name = "net/onedaybeard/graftt/AnnotationFusing\$ClashingMethodTransplant",
               anno = "net/onedaybeard/graftt/AnnotationFusing\$MyAnnoRt",
               symbol = "hmm"))
    }

    @Test
    fun `detect annotation clash on field`() {
        transplant<AnnotationFusing.ClashingFieldTransplant>()
           .assertErr(Msg.AnnotationAlreadyExists(
               name = "net/onedaybeard/graftt/AnnotationFusing\$ClashingFieldTransplant",
               anno = "net/onedaybeard/graftt/AnnotationFusing\$MyAnno",
               symbol = "usch"))
    }

    @Test
    fun `detect annotation clash on class`() {
        TODO()
//        transplant<AnnotationFusing.ClashingClassTransplant>()
//           .assertErr(Msg.AnnotationAlreadyExists(
//               name = "net/onedaybeard/graftt/AnnotationFusing.ClashingClassTransplant",
//               anno = "net/onedaybeard/graftt/AnnotationFusing.MyAnno",
//               symbol = "ClashingClass"))
    }
}
