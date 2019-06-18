package net.onedaybeard.graftt

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.graft.performGraft
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import kotlin.test.assertEquals


data class FieldObserver<T>(
    val name: String,
    val original: T?,
    val updated: T?)


inline fun <reified T> transplant(): ClassNode {
    return resultOf { classNode<T>() }                             // donor
        .andThen { donor -> performGraft(donor, ::loadClassNode) } // to recipient
        .onFailure(`(╯°□°）╯︵ ┻━┻`)
        .unwrap()
}


fun <T> observeField(name: String, oldToNew: Pair<T, T>): FieldObserver<T> {
    return FieldObserver(name, oldToNew.first, oldToNew.second)
}

fun <T> Any.method(name: String,
                   params: List<Any?> = listOf(),
                   expected: T? = null): T? {

    val actual = this::class.java
        .declaredMethods
        .find { it.name == name }
        .toResultOr { Msg.NoSuchKey(name) }
        .andThen { resultOf { it.invoke(this, *params.toTypedArray()) } }
        .onFailure(`(╯°□°）╯︵ ┻━┻`)
        .unwrap()

    if (expected != null)
        assertEquals(expected, actual)

    @Suppress("UNCHECKED_CAST")
    return actual as T?
}

inline fun <reified T> Any.invokeMethod(name: String,
                                        vararg observers: FieldObserver<*>): T? {

    return invokeMethod(name, listOf(), *observers)
}

inline fun <reified T> Any.invokeMethod(name: String,
                                        params: List<Any?>,
                                        vararg observers: FieldObserver<*>): T? {

    observers.forEach { assertFieldValue(it.name, it.original) }
    val t = method<T>(name, params)
    observers.forEach { assertFieldValue(it.name, it.updated) }

    return t
}

fun Any.assertMethodExists(name: String): Boolean {
    return this::class.java
        .declaredMethods
        .find { it.name == name } != null
}

fun <T> Any.assertFieldValue(name: String, expected: T? = null) {
    this::class.java
        .declaredFields
        .find { it.name == name }
        .toResultOr { Msg.NoSuchKey(name) }
        .andThen { resultOf { it.get(this) } }
        .onFailure(`(╯°□°）╯︵ ┻━┻`)
        .onSuccess { if (expected != null) assertEquals(expected, it) }
}

fun instantiate(cn: ClassNode, f: Any.() -> Unit = {}): Any {
    val instance = ByteClassLoader().loadClass(cn).newInstance()
    f(instance)
    return instance
}

fun loadClassNode(type: Type) = resultOf {
    GraftTests::class.java
        .getResourceAsStream("/${type.internalName}.class")
        .let(::classNode)
}

private class ByteClassLoader : ClassLoader() {
    fun loadClass(cn: ClassNode): Class<*> {
        val bytes = cn.toBytes()
        val clazz = defineClass(cn.qualifiedName, bytes, 0, bytes.size)
        resolveClass(clazz)
        return clazz
    }
}
