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

private val Pair<String, String>.length: Int
    get() = first.length + second.length

private val ClassNode.simpleName: String
    get() = qualifiedName.substringAfterLast(".")

// todo: 1) log/report 2) test
@Mojo(name = "graftt", defaultPhase = PROCESS_CLASSES)
class GrafttMojo : AbstractMojo() {

    @Parameter(property = "project.build.outputDirectory", readonly = true)
    private lateinit var classDirectory: File

    @Parameter(property = "graftt.enable", defaultValue = "true")
    private var enable: Boolean = false

    override fun execute() {
        if (!enable) return

        classDirectory.walk()
            .filter { it.endsWith(".class") }
            .map(::classNode)
            .filter { cn -> readRecipientType(cn).get() != null }
            .onEach(this::transplant)
            .toList()
            .let { donated ->
                val w = 72

                log.info(format("graftt surgical summary:" to "${donated.size}", ' '))
                log.info("-".repeat(w))
                donated
                    .map { cn -> cn to readRecipientType(cn).map(this::loadClassNode).unwrap() }
                    .map { (d, r) -> format(d.simpleName to r.simpleName) }
                    .forEach(log::info)
                log.info("-".repeat(w))

        }

    }

    private fun format(kv: Pair<String, String>, delim: Char = '.'): String {
        return "$delim".repeat(72 - 2 - kv.length)
            .let { "${kv.first} $it ${kv.second}" }
    }

    private fun transplant(donor: ClassNode) {
        resultOf { donor }
            .andThen(::readRecipientType)
            .andThen { type -> resultOf { loadClassNode(type) } }
            .andThen { recipient -> performGraft(donor, recipient) }
            .onFailure(`(╯°□°）╯︵ ┻━┻`)
            .onSuccess(this::save)
    }

    private fun loadClassNode(type: Type): ClassNode {
        return File(classDirectory, "${type.internalName.replace('.', '/')}.class")
            .let(::classNode)
    }

    private fun save(cn: ClassNode) {
        val f = File(classDirectory, "${cn.name}.class")
        if (!f.exists()) throw RuntimeException("wrong path: ${f.absolutePath}")

        f.writeBytes(cn.toBytes())
    }
}
