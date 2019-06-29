package net.onedaybeard.graftt

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.util.CheckClassAdapter
import org.objectweb.asm.util.TraceClassVisitor
import java.io.File
import java.io.FileNotFoundException
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.IllegalStateException
import java.lang.RuntimeException
import java.util.zip.ZipFile

@Suppress("NonAsciiCharacters", "ObjectPropertyName", "unused")
val `(╯°□°）╯︵ ┻━┻`: (Msg) -> Nothing = { throw it.toException() }

fun <T> anyOf(vararg predicates: (T) -> Boolean): (T) -> Boolean = { t -> predicates.any { it(t) } }
fun <T> allOf(vararg predicates: (T) -> Boolean): (T) -> Boolean = { t -> predicates.all { it(t) } }
fun <T> noneOf(vararg predicates: (T) -> Boolean): (T) -> Boolean = { t -> predicates.none { it(t) } }


fun ClassNode.toDebugString(): String {
    val sw = StringWriter()
    val pw = PrintWriter(sw)

    classReader(toBytes())
        .accept(TraceClassVisitor(pw), ClassReader.EXPAND_FRAMES)

    return sw.toString()
}

fun verify(cn: ClassNode, classLoader: ClassLoader? = null): Result<ClassNode, Msg> {
    val sw = StringWriter()
    val cr = ClassReader(cn.toBytes())
    CheckClassAdapter.verify(cr, classLoader, false, PrintWriter(sw))

    return when (val error = sw.toString()) {
        ""   -> Ok(cn)
        else -> Err(Msg.ClassVerificationError(cn.qualifiedName, error))
    }
}

/** reads all classes, where [root] points to a root directory or jar file */
fun classNodes(root: File): List<ClassNode> = when {
    root.exists().not()     -> throw FileNotFoundException(root.path)
    root.isDirectory        -> classesDir(root)
    root.extension == "jar" -> classesJar(root)
    else                    -> throw IllegalStateException(root.path)
}


private fun classesJar(root: File): List<ClassNode> {
    return ZipFile(root).use { archive ->
        archive.entries()
            .asSequence()
            .filter { it.name.endsWith(".class") }
            .map(archive::getInputStream)
            .map(::classNode)
            .toList()
    }
}

private fun classesDir(root: File): List<ClassNode> {
    return root.walk()
        .filter { it.extension == "class" }
        .map(::classNode)
        .toList()
}

class GraftException(message: String, cause: Throwable? = null)
    : RuntimeException(message, cause) {

    constructor(message: Msg, cause: Throwable? = null)
        : this(message.toString(), cause)
}

