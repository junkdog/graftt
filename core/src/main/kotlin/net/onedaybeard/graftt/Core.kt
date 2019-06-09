package net.onedaybeard.graftt

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.util.CheckClassAdapter
import org.objectweb.asm.util.TraceClassVisitor
import java.io.PrintWriter
import java.io.StringWriter

@Suppress("NonAsciiCharacters")
internal val `(╯°□°）╯︵ ┻━┻`: (String) -> Nothing = ::TODO

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
