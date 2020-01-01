package net.onedaybeard.graftt.graft

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.Msg
import net.onedaybeard.graftt.asm.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal typealias TypeList = java.util.ArrayList<Type>


inline fun <reified T, reified R> Iterable<AnnotationNode>.read(
    field: KProperty1<T, R>
): Result<R, Msg> {
    return findAnnotation(type<T>()).andThen { it.get<R>(field.name) }
}

inline fun <reified T> Iterable<AnnotationNode>.readType(
    field: KProperty1<T, KClass<*>>
): Result<Type, Msg> {
    return findAnnotation(type<T>()).andThen { it.get<Type>(field.name) }
}

inline fun <reified T> Iterable<AnnotationNode>.readTypes(
    field: KProperty1<T, Array<KClass<out Annotation>>>
): Result<List<Type>, Msg> {
    return findAnnotation(type<T>()).andThen { it.get<TypeList>(field.name) }
}

