package net.onedaybeard.graftt.asm

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.Msg
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import kotlin.reflect.KProperty1

operator fun Iterable<AnnotationNode>?.contains(type: Type) =
    this?.let { findAnnotation(type) is Ok } ?: false

fun Iterable<AnnotationNode>.asTypes(): List<Type> = map(AnnotationNode::type)

fun Iterable<AnnotationNode>.findAnnotation(type: Type) =
    find { type.descriptor == it.desc }.toResultOr { Msg.NoSuchAnnotation(type.className) }

inline fun <reified T : Annotation> Iterable<AnnotationNode>.findAnnotation() =
    findAnnotation(type<T>())

/** Reads value from annotation property where [R] is a primitive value or string */
inline fun <reified T : Annotation, reified R> Iterable<AnnotationNode>.read(
    field: KProperty1<T, R>
) = findAnnotation<T>() andThen readField<R>(field.name)

/** Reads class value of annotation property as type */
inline fun <reified T : Annotation> Iterable<AnnotationNode>.readType(
    field: KProperty1<T, *>
) = findAnnotation<T>() andThen readField<Type>(field.name)

/** Reads class values of annotation property as types */
inline fun <reified T : Annotation> Iterable<AnnotationNode>.readTypes(
    field: KProperty1<T, Array<*>>
) = findAnnotation<T>() andThen readField<List<Type>>(field.name)

inline fun <reified T> readField(
    name: String
): (AnnotationNode) -> Result<T, Msg> = { an ->
    val values = an.values ?: listOf()
    val index = values
        .filterIndexed { i, _ -> i % 2 == 0 }
        .indexOf(name)

    when (val any = values.getOrNull(index * 2 + 1)) {
        is T -> Ok(any)
        null -> Err(Msg.NoSuchKey(name))
        else -> Err(Msg.WrongTypeT(any::class, T::class))
    }
}