package net.onedaybeard.graftt

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.graft.performGraft
import net.onedaybeard.graftt.graft.readRecipientType
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.lang.RuntimeException

private const val LINE_WIDTH = 72

private val Pair<String, String>.length: Int
    get() = first.length + second.length

private val ClassNode.simpleName: String
    get() = qualifiedName.substringAfterLast(".")

@Mojo(name = "graftt", defaultPhase = PROCESS_CLASSES)
class GrafttMojo : AbstractMojo() {

    @Parameter(property = "project.build.outputDirectory", readonly = true)
    private lateinit var classDirectory: File

    @Parameter(property = "graftt.enable", defaultValue = "true")
    private var enable: Boolean = false

    override fun execute() {
        if (!enable) return

        classDirectory.walk()
            .filter { it.name.endsWith(".class") }
            .map(::classNode)
            .filter { cn -> readRecipientType(cn).get() != null }
            .onEach(this::transplant)
            .toList()
            .let(this::logSummary)
    }

    private fun logSummary(donated: List<ClassNode>) {
        fun format(kv: Pair<String, String>, delim: Char = '.'): String {
            return "$delim".repeat(LINE_WIDTH - 2 - kv.length)
                .let { "${kv.first} $it ${kv.second}" }
        }

        log.info(format("graftt surgical summary:" to "${donated.size}", ' '))
        log.info("-".repeat(LINE_WIDTH))
        donated
            .map { cn -> cn to readRecipientType(cn).andThen(this::loadClassNode).unwrap() }
            .map { (d, r) -> format(d.simpleName to r.simpleName) }
            .forEach(log::info)
        log.info("-".repeat(LINE_WIDTH))
    }


    private fun transplant(donor: ClassNode) {
        resultOf { donor }
            .andThen(::readRecipientType)
            .andThen(this::loadClassNode)
            .andThen { recipient -> performGraft(donor, recipient) }
            .andThen(this::save)
            .onFailure(`(╯°□°）╯︵ ┻━┻`)
    }

    private fun loadClassNode(type: Type): Result<ClassNode, Msg> = resultOf {
        File(classDirectory, "${type.internalName.replace('.', '/')}.class")
            .let(::classNode)
    }

    private fun save(cn: ClassNode) = resultOf {
        val f = File(classDirectory, "${cn.name}.class")
        if (!f.exists()) throw RuntimeException("wrong path: ${f.absolutePath}")

        f.writeBytes(cn.toBytes())
    }
}
