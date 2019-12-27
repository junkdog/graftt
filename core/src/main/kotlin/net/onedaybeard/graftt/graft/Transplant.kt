package net.onedaybeard.graftt.graft

import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.map
import com.github.michaelbull.result.toResultOr
import net.onedaybeard.graftt.Graft
import net.onedaybeard.graftt.Msg
import net.onedaybeard.graftt.asm.annotations
import net.onedaybeard.graftt.asm.get
import net.onedaybeard.graftt.asm.hasAnnotation
import net.onedaybeard.graftt.asm.type
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

sealed class Transplant {
    data class Field(
        val donor: String,
        val node: FieldNode,
        val transplantLookup: Remapper
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

    /** returns value of [Graft.Annotations.overwrite] or `false` if not present */
    fun overwriteAnnotations(): Boolean {
        val type = type<Graft.Annotations>()
        return annotations()
            .find { it.desc == type.descriptor }
            .toResultOr { Msg.NoSuchAnnotation(type.className) }
            .andThen { it.get<Boolean>("overwrite") }
            .fold(success = { it }, failure = { false })
    }
}
