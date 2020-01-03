package net.onedaybeard.graftt.asm

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.Type.getInternalName
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.InputStream
import kotlin.reflect.KClass

val KClass<*>.internalName: String
    get() = java.internalName
val Class<*>.internalName: String
    get() = getInternalName(this)

fun classReader(stream: InputStream) = stream.use(::ClassReader)
fun classReader(klazz: KClass<*>) = classReader(klazz.java)
fun classReader(file: File) = classReader(file.inputStream().buffered())
fun classReader(bytes: ByteArray) = ClassReader(bytes)
fun classReader(klazz: Class<*>) =
    classReader(klazz.getResourceAsStream("/${getInternalName(klazz)}.class"))

inline fun <reified T> classNode() = classNode(T::class)
fun classNode(cr: ClassReader) = ClassNode().apply { cr.accept(this, 0) }
fun classNode(bytes: ByteArray) = classNode(classReader(bytes))
fun classNode(klazz: KClass<*>) = classNode(klazz.java)
fun classNode(klazz: Class<*>) = classNode(classReader(klazz))
fun classNode(stream: InputStream) = classNode(classReader(stream))
fun classNode(file: File) = classNode(classReader(file))

val ClassNode.type: Type
    get() = Type.getType("L$name;")
val AnnotationNode.type: Type
    get() = Type.getType(desc)

inline fun <reified T> type() = type(T::class)
fun type(cls: KClass<*>) = Type.getType(cls.java)!!

