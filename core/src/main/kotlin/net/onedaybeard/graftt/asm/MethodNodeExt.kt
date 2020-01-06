package net.onedaybeard.graftt.asm

import net.onedaybeard.graftt.combine
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode


fun MethodNode.annotations(): MutableIterable<AnnotationNode> =
    combine(invisibleAnnotations, visibleAnnotations)

fun MethodNode.hasAnnotation(type: Type) =
    type in visibleAnnotations || type in invisibleAnnotations

fun InsnList.asSequence() = Iterable { iterator() }.asSequence()
fun MethodNode.asSequence() = instructions.asSequence()

fun MethodNode.copy(copyInsn: Boolean = true) = MethodNode(
    access, name, desc, signature, exceptions?.toTypedArray()
).also { if (copyInsn) accept(it) }

fun MethodNode.signatureEquals(other: MethodNode): Boolean {
    return name      == other.name
        && desc      == other.desc
        && signature == other.signature
}

fun MethodNode.signatureEquals(other: MethodInsnNode): Boolean {
    return name      == other.name
        && desc      == other.desc
}
