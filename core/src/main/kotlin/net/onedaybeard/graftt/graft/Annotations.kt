package net.onedaybeard.graftt.graft

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.Graft
import net.onedaybeard.graftt.Msg
import net.onedaybeard.graftt.asm.annotation
import net.onedaybeard.graftt.asm.get
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

private typealias TypeList = java.util.ArrayList<Type>


/** returns types declared in [Graft.Annotations.remove] or an empty set if not present */
fun ClassNode.annotationsToRemove(): Set<Type> =
    annotationsToRemove(annotation<Graft.Annotations>())
fun MethodNode.annotationsToRemove(): Set<Type> =
    annotationsToRemove(annotation<Graft.Annotations>())
fun FieldNode.annotationsToRemove(): Set<Type> =
    annotationsToRemove(annotation<Graft.Annotations>())

private fun annotationsToRemove(node: Result<AnnotationNode, Msg>): Set<Type> {
    return node
        .andThen { it.get<TypeList>("remove") }
        .map { it.toSet() }
        .get() ?: setOf()
}

/** returns value of [Graft.Annotations.overwrite] or `false` if not present */
val ClassNode.overwriteAnnotations: Boolean
    get() = overwriteAnnotations(annotation<Graft.Annotations>())
val MethodNode.overwriteAnnotations: Boolean
    get() = overwriteAnnotations(annotation<Graft.Annotations>())
val FieldNode.overwriteAnnotations: Boolean
    get() = overwriteAnnotations(annotation<Graft.Annotations>())

private fun overwriteAnnotations(node: Result<AnnotationNode, Msg>) =
    node.andThen { it.get<Boolean>("overwrite") }.get() ?: false
