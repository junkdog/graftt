package net.onedaybeard.graftt.graft

import com.github.michaelbull.result.get
import net.onedaybeard.graftt.Graft
import net.onedaybeard.graftt.asm.*
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

sealed class Transplant<T> {
    data class Field(
        override val donor: String,
        override val node: FieldNode,
        override val transplantLookup: Remapper
    ) : Transplant<FieldNode>()

    data class Method(
        override val donor: String,
        override val node: MethodNode,
        override val transplantLookup: Remapper
    ) : Transplant<MethodNode>()

    /** used for mutating annotations on recipient */
    data class Class(
        override val donor: String,
        override val node: ClassNode,
        override val transplantLookup: Remapper
    ) : Transplant<ClassNode>()

    abstract val donor: String
    abstract val node: T
    abstract val transplantLookup: Remapper

    fun annotations(): List<AnnotationNode> = when (this) {
        is Field  -> node.annotations()
        is Method -> node.annotations()
        is Class  -> node.annotations()
    }

    val overwriteAnnotations: Boolean
        get() = annotations().read(Graft.Annotations::overwrite).get() ?: false

    /** returns types declared in [Graft.Annotations.remove] or an empty list */
    fun annotationsToRemove(): TypeList =
        annotations().readTypes(Graft.Annotations::remove).get() ?: TypeList()

    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    fun findMatchingNode(other: ClassNode): T? = when (this) {
        is Field  -> other.fields.find { it.signatureEquals(node) }
        is Method -> other.methods.find { it.signatureEquals(node) }
        is Class  -> other
    } as T?
}

