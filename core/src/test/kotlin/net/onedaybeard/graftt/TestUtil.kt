package net.onedaybeard.graftt

import com.github.michaelbull.result.getError
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import kotlin.reflect.KClass
import kotlin.test.assertEquals


infix fun ClassNode.bytecodeEquals(kClass: KClass<*>) {
    assertEquals(
        null,
        verify(this).getError())

    assertEquals(
        classNode(kClass).apply { sourceFile = null }.toDebugString(),
        toDebugString())
}

data class FieldObserver<T>(
    val name: String,
    val original: T?,
    val updated: T?)


fun <T> observeField(name: String, oldToNew: Pair<T, T>): FieldObserver<T> {
    return FieldObserver(name, oldToNew.first, oldToNew.second)
}

fun <T> Any.method(name: String, expected: T? = null): T? {
    val actual = this::class.java
        .declaredMethods
        .first { it.name == name }
        .invoke(this)

    if (expected != null)
        assertEquals(expected, actual)

    @Suppress("UNCHECKED_CAST")
    return actual as? T
}

inline fun <reified T> Any.invokeMethod(name: String, vararg observers: FieldObserver<*>): T? {
    observers.forEach { assertFieldValue(it.name, it.original) }
    val t = method<T>(name)
    observers.forEach { assertFieldValue(it.name, it.updated) }

    return t
}

fun Any.assertMethodExists(name: String): Boolean {
    return this::class.java
        .declaredMethods
        .find { it.name == name } != null
}

fun <T> Any.assertFieldValue(name: String, expected: T? = null) {
    val actual = this::class.java
        .declaredFields
        .first { it.name == name }
        .get(this)

    if (expected != null)
        assertEquals(expected, actual)
}

fun instantiate(cn: ClassNode, f: Any.() -> Unit): Any {
    val instance = ByteClassLoader().loadClass(cn).newInstance()
    f(instance)
    return instance
}

fun loadClassNode(type: Type) = resultOf {
    GraftTest::class.java
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
