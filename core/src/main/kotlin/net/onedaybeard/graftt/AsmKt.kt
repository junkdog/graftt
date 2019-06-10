package net.onedaybeard.graftt

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.SourceLanguage.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.ASM7
import org.objectweb.asm.Type
import org.objectweb.asm.Type.getInternalName
import org.objectweb.asm.tree.*
import java.io.File
import java.io.InputStream
import kotlin.reflect.KClass


enum class SourceLanguage {
    JAVA, KOTLIN
}

val KClass<*>.internalName: String
    get() = java.internalName
val Class<*>.internalName: String
    get() = Type.getInternalName(this)

////// classes

/** compatible with [Class.forName] */
val ClassNode.qualifiedName: String
    get() = type.className

val ClassNode.type: Type
    get() = Type.getType("L${name.replace('/', '.')};")

fun ClassNode.toBytes(): ByteArray {
    val cw = ClassWriter(0)
    accept(cw)
    return cw.toByteArray()
}

fun ClassNode.sourceLanguage(): SourceLanguage {
    return visibleAnnotations
        ?.find { it.desc == "Lkotlin/Metadata;" }
        ?.let { KOTLIN } ?: JAVA
}

fun ClassNode.copy() = ClassNode(ASM7).also(::accept)

////// methods

fun MethodNode.signatureEquals(other: MethodNode): Boolean {
    return access    == other.access
        && name      == other.name
        && desc      == other.desc
        && signature == other.signature
}

fun MethodNode.signatureEquals(other: MethodInsnNode): Boolean {
    return name      == other.name
        && desc      == other.desc
}



////// instructions

fun InsnList.asSequence() = Iterable { iterator() }.asSequence()
fun MethodNode.asSequence() = instructions.asSequence()

////// annotations


fun ClassNode.hasAnnotation(type: Type) =
    type in visibleAnnotations || type in invisibleAnnotations

fun MethodNode.hasAnnotation(type: Type) =
    type in visibleAnnotations || type in invisibleAnnotations

fun FieldNode.hasAnnotation(type: Type) =
    type in visibleAnnotations || type in invisibleAnnotations


fun List<AnnotationNode>.findAnnotation(desc: String) =
    find { it.desc == desc }.toResultOr { Msg.None }

fun List<AnnotationNode>.findAnnotation(type: Type) =
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


fun classReader(stream: InputStream) = stream.use(::ClassReader)
fun classReader(klazz: KClass<*>) = classReader(klazz.java)
fun classReader(file: File) = classReader(file.inputStream().buffered())
fun classReader(bytes: ByteArray) = ClassReader(bytes)
fun classReader(klazz: Class<*>) =
    classReader(klazz.getResourceAsStream("/${getInternalName(klazz)}.class"))

inline fun <reified T> classNode() = classNode(T::class)
fun classNode(cr: ClassReader) = ClassNode().apply { cr.accept(this, 0) }
fun classNode(klazz: KClass<*>) = classNode(klazz.java)
fun classNode(klazz: Class<*>) = classNode(classReader(klazz))
fun classNode(stream: InputStream) = classNode(classReader(stream))
fun classNode(file: File) = classNode(classReader(file))

inline fun <reified T> type() = type(T::class)
fun type(cls: KClass<*>) = Type.getType(cls.java)


private operator fun List<AnnotationNode>?.contains(type: Type) = when(this) {
    null -> false
    else -> findAnnotation(type.descriptor).get() != null
}
