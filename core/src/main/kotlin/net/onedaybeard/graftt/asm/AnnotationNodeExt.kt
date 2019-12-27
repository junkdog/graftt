package net.onedaybeard.graftt.asm

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr
import net.onedaybeard.graftt.Msg
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import kotlin.reflect.KClass


operator fun Iterable<AnnotationNode>?.contains(type: Type) = when(this) {
    null -> false
    else -> findAnnotation(type.descriptor) is Ok
}

fun Iterable<AnnotationNode>?.asTypes(): Set<Type> = this?.map { Type.getType(it.desc) }?.toSet() ?: setOf()

fun Iterable<AnnotationNode>.findAnnotation(desc: String): Result<AnnotationNode, Msg.NoSuchAnnotation> {
    return find { it.desc == desc }
        .toResultOr { Msg.NoSuchAnnotation(Type.getType(desc).className) }
}

fun Iterable<AnnotationNode>.findAnnotation(type: Type) =
    findAnnotation(type.descriptor)

inline fun <reified T : Annotation> List<AnnotationNode>.findAnnotation() =
    findAnnotation(type<T>())

inline fun <reified T> AnnotationNode.get(key: String): Result<T, Msg> {
    values ?: return Err(Msg.NoSuchKey(key))

    val index = values
        .filterIndexed { i, _ -> i % 2 == 0 }
        .mapNotNull { it as? String }
        .indexOf(key)

    if (index == -1)
        return Err(Msg.NoSuchKey(key))

    return when (val any = values[index * 2 + 1]) {
        is T -> Ok(any as T)
        else -> Err(Msg.WrongTypeT(any::class, T::class))
    }
}

fun ClassNode.annotation(type: KClass<*>): Result<AnnotationNode, Msg> {
    return annotations().findAnnotation(type(type))
}

fun MethodNode.annotation(type: KClass<*>): Result<AnnotationNode, Msg> {
    return annotations().findAnnotation(type(type))
}

fun FieldNode.annotation(type: KClass<*>): Result<AnnotationNode, Msg> {
    return annotations().findAnnotation(type(type))
}

inline fun <reified T> ClassNode.annotation() = annotation(T::class)
inline fun <reified T> FieldNode.annotation() = annotation(T::class)
inline fun <reified T> MethodNode.annotation() = annotation(T::class)

