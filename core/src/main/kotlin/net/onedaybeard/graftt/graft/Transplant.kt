package net.onedaybeard.graftt.graft

import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

sealed class Transplant {
    data class Field(
        val donor: String,
        val node: FieldNode
    ) : Transplant()

    data class Method(
        val donor: String,
        val node: MethodNode
    ) : Transplant() {
        fun copy() = Method(donor, MethodNode().apply {
            access = node.access
            name = node.name
            desc = node.desc
            signature = node.signature
            exceptions = node.exceptions

            node.accept(this)
        })
    }
}