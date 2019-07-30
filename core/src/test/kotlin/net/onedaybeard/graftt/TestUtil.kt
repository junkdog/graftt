package net.onedaybeard.graftt

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.graft.readRecipientType
import net.onedaybeard.graftt.graft.transplant
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail


data class FieldObserver<T>(
    val name: String,
    val original: T?,
    val updated: T?)


inline fun <reified T> transplant(): Result<ClassNode, Msg> {
    return transplant(T::class)
}


fun transplant(type: KClass<*>, lookup: Map<Type, Type>? = null): Result<ClassNode, Msg> {
    return transplant(classNode(type), lookup)
}

fun transplant(donor: ClassNode, lookup: Map<Type, Type>? = null): Result<ClassNode, Msg> {

    val l = lookup ?: mapOf(donor.type to readRecipientType(donor).unwrap())
    return resultOf { donor }                                       // donor
        .andThen { donor -> transplant(donor, ::loadClassNode, l) } // to recipient
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

fun Any.assertMethodExists(name: String) {
    return this::class.java
        .declaredMethods
        .find { it.name == name }
        .let { assertNotNull(it, "method not found: $name") }
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

fun <V> Result<V, Msg>.assertErr(msg: Msg): Result<V, Msg> {
    when (this) {
        is Ok  -> fail("OK($value), expected failure: $msg")
        is Err -> assertEquals(msg, error)
    }
    return this
}

fun transplantsOf(vararg types: KClass<*>): List<ClassNode> {
    val lookup = transplantLookup(*types)
    return types.map { transplant(it, lookup).unwrap() }
}

fun transplantLookup(vararg transplants: KClass<*>): Map<Type, Type> {
    return transplants
        .associateBy(::type) { readRecipientType(classNode(it)).unwrap() }
}

fun instantiate(cn: ClassNode, f: Any.() -> Unit = {}): Any {
    val instance = ByteClassLoader().loadClass(cn).newInstance()
    f(instance)
    return instance
}
fun instantiate(cn: Result<ClassNode, Msg>, f: Any.() -> Unit = {}): Any {
    return instantiate(cn.unwrap(), f)
}

fun instantiate(vararg cns: ClassNode, f: (List<Any>) -> Unit = {}): List<Any> {
    val cl = ByteClassLoader()
    return cns
        .map(cl::loadClass)
        .map(Class<out Any>::newInstance)
        .also(f)
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

