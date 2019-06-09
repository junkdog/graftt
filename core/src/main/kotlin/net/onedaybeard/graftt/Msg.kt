package net.onedaybeard.graftt

import org.objectweb.asm.tree.ClassNode
import kotlin.reflect.KClass

/** enumerates all possible failure states */
sealed class Msg {
    data class ClassVerificationError(
        val name: String,
        val error: String) : Msg()

    data class Error(val e: Throwable) : Msg()

    data class MissingGraftTargetAnnotation(val cn: ClassNode) : Msg()

    object None : Msg()
    data class NoSuchKey(val key: String) : Msg()

    data class WrongTypeT(
        val expected: KClass<*>,
        val actual: KClass<*>) : Msg()
}
