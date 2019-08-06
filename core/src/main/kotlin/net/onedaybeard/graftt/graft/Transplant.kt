package net.onedaybeard.graftt.graft

import net.onedaybeard.graftt.asm.annotations
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

sealed class Transplant {
    data class Field(
        val donor: String,
        val node: FieldNode
    ) : Transplant()

    data class Method(
        val donor: String,
        val node: MethodNode,
        val transplantLookup: Remapper
    ) : Transplant()

    fun annotations(): List<AnnotationNode> = when (this) {
        is Field  -> node.annotations()
        is Method -> node.annotations()
    }
}
