package net.onedaybeard.graftt.asm

import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.FieldNode


fun FieldNode.annotations(): List<AnnotationNode> =
    (invisibleAnnotations ?: listOf()) + (visibleAnnotations ?: listOf())

fun FieldNode.hasAnnotation(type: Type) =
    type in visibleAnnotations || type in invisibleAnnotations

fun FieldNode.copy(): FieldNode {
    val fn = FieldNode(access, name, desc, signature, value)
    fn.visibleAnnotations = ArrayList((visibleAnnotations ?: listOf()))
    fn.invisibleAnnotations = ArrayList((invisibleAnnotations ?: listOf()))

    return fn
}

fun FieldNode.signatureEquals(other: FieldNode): Boolean {
    return name      == other.name
        && desc      == other.desc
        && signature == other.signature
}

