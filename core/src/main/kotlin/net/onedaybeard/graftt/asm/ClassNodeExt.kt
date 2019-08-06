package net.onedaybeard.graftt.asm

import net.onedaybeard.graftt.graft.Transplant
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

/** compatible with [Class.forName] */
val ClassNode.qualifiedName: String
    get() = type.className

val ClassNode.shortName: String
    get() = qualifiedName.substringAfterLast(".")

val ClassNode.type: Type
    get() = Type.getType("L$name;")

operator fun ClassNode.contains(t: Transplant.Method): Boolean {
    return methods.find { it.signatureEquals(t.node) } != null
}

fun ClassNode.annotations(): List<AnnotationNode> =
    (invisibleAnnotations ?: listOf()) + (visibleAnnotations ?: listOf())

fun ClassNode.hasAnnotation(type: Type) =
    type in visibleAnnotations || type in invisibleAnnotations

fun ClassNode.copy() = ClassNode(Opcodes.ASM7).also(::accept)

fun ClassNode.toBytes(): ByteArray {
    val cw = ClassWriter(0)
    accept(cw)
    return cw.toByteArray()
}





