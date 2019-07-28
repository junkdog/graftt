package net.onedaybeard.graftt.graft

import org.objectweb.asm.Type
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
        val transplantLookup: Map<Type, Type>
    ) : Transplant()
}