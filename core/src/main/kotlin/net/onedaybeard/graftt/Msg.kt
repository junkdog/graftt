package net.onedaybeard.graftt

import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import kotlin.reflect.KClass

/** enumerates all possible failure states */
sealed class Msg {
    data class ClassVerificationError(
        val name: String,
        val error: String) : Msg()

    data class Error(val e: Throwable) : Msg()

    data class MissingGraftTargetAnnotation(val name: String) : Msg()

    object None : Msg()
    data class NoSuchKey(val key: String) : Msg()

    data class FieldAlreadyExists(val node: FieldNode) : Msg()
    data class InterfaceAlreadyExists(val name: String) : Msg()
    data class MethodAlreadyExists(val node: MethodNode) : Msg()
    data class WrongFuseSignature(val node: MethodNode) : Msg()

    data class TransplantMustNotExtendClass(val name: String) : Msg()
    data class FieldDefaultValueNotSupported(val name: String, val field: String) : Msg()

    data class WrongTypeT(
        val expected: KClass<*>,
        val actual: KClass<*>) : Msg()

    fun toException() = when(this) {
        is Error -> e
        else     -> GraftException(this)
    }
}

