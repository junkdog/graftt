package net.onedaybeard.graftt.asm

import net.onedaybeard.graftt.graft.Transplant
import net.onedaybeard.graftt.collections.mutableIterables
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

/** compatible with [Class.forName] */
val ClassNode.qualifiedName: String
    get() = type.className

val ClassNode.shortName: String
    get() = qualifiedName.substringAfterLast(".")

operator fun ClassNode.contains(t: Transplant.Method): Boolean =
    methods.find(t.node::signatureEquals) != null

fun ClassNode.annotations(): MutableIterable<AnnotationNode> =
    mutableIterables(invisibleAnnotations, visibleAnnotations)

fun ClassNode.hasAnnotation(type: Type) =
    type in visibleAnnotations || type in invisibleAnnotations

fun ClassNode.copy() = ClassNode(Opcodes.ASM7).also(::accept)
fun ClassNode.copy(remapper: Remapper) = ClassNode(Opcodes.ASM7)
    .also { cn -> accept(ClassRemapper(cn, remapper)) }

fun ClassNode.toBytes(): ByteArray {
    val cw = ClassWriter(0)
    accept(cw)
    return cw.toByteArray()
}