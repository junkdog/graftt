package net.onedaybeard.graftt

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import net.onedaybeard.graftt.asm.classNode
import net.onedaybeard.graftt.asm.classReader
import net.onedaybeard.graftt.asm.toBytes
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.AnalyzerException
import org.objectweb.asm.tree.analysis.BasicVerifier
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
    classReader(toBytes())
        .accept(TraceClassVisitor(PrintWriter(sw)), ClassReader.EXPAND_FRAMES)

    return sw.toString()
}

fun verify(source: ClassNode): Result<ClassNode, Msg> {
    val cn = ClassNode()
    CheckClassAdapter(cn, true)
        .also(source::accept)

    val sw = StringWriter()
    val pw = PrintWriter(sw)
    for (mn in cn.methods) {
        try {
            Analyzer(BasicVerifier()).analyze(cn.name, mn)
        } catch (e: AnalyzerException) {
            e.printStackTrace(pw)
        }
    }
    pw.flush()

    return when (val err = sw.toString()) {
        ""   -> Ok(source)
        else -> Err(Msg.ClassVerificationError(source.name, err))
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