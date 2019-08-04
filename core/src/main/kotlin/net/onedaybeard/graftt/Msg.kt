package net.onedaybeard.graftt

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

    data class AnnotationAlreadyExists(val name: String, val symbol: String, val anno: String) : Msg()

    data class FieldAlreadyExists(val name: String, val field: String) : Msg()
    data class InterfaceAlreadyExists(val name: String, val iface: String) : Msg()
    data class MethodAlreadyExists(val name: String, val method: String) : Msg()
    data class WrongFuseSignature(val name: String, val method: String) : Msg()

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

