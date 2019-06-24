package net.onedaybeard.graftt

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.graft.isTransplant
import net.onedaybeard.graftt.graft.transplant
import net.onedaybeard.graftt.graft.readRecipientType
import org.apache.maven.artifact.Artifact
import org.apache.maven.model.Dependency
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.lang.Integer.max
import java.lang.RuntimeException
import org.apache.maven.project.MavenProject
import java.io.FileNotFoundException


private const val LINE_WIDTH = 72

private val Pair<String, String>.length: Int
    get() = first.length + second.length

@Mojo(
    name = "graftt",
    defaultPhase = PROCESS_CLASSES,
    requiresDependencyResolution = COMPILE_PLUS_RUNTIME)
class GrafttMojo : AbstractMojo() {

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject

    @Parameter(property = "project.build.outputDirectory", readonly = true)
    private lateinit var classDir: File

    @Parameter(property = "graftt.enable", defaultValue = "true")
    private var enable: Boolean = false

    @Parameter
    private var paths: List<File> = listOf()

    /** Refrain from removing transplants once applied */
    @Parameter(property = "graftt.keepTransplants", defaultValue = "false")
    private var keepTransplants: Boolean = false

    override fun execute() {
        if (!enable) return

        val transplants = mutableListOf<ClassNode>()

        // resolve ClassNodes from plugin's <dependencies>, if any
        project.buildPlugins
            .first { it.artifactId == "graftt-maven-plugin" }
            .let { it.dependencies ?: listOf() }
            .map { it.toArtifact()}
            .map { it.file ?: throw FileNotFoundException("$it") }
            .onEach { log.debug("donor preparing: ${it.path}") }
            .flatMapTo(transplants, ::classNodes)

        // recursively resolve ClassNodes from //configuration/paths and target/classes
        (paths + classDir)
            .flatMapTo(transplants, ::classNodes)

        // commence surgery
        transplants
            .filter(ClassNode::isTransplant)
            .onEach(this::transplant)
            .let(this::logSummary)

        if (!keepTransplants) {
            classNodes(classDir)
                .filter(ClassNode::isTransplant)
                .map { it.toFile() }
                .forEach { it.delete() }
        }
    }

    private fun logSummary(donated: List<ClassNode>) {
        fun format(kv: Pair<String, String>, delim: Char = '.'): String {
            return "$delim".repeat(max(2, LINE_WIDTH - 2 - kv.length))
                .let { "${kv.first} $it ${kv.second}" }
        }

        fun header(header: String, delim: Char = ' '): String {
            return "$delim".repeat(max(2, LINE_WIDTH - 2 - header.length))
                .let { "$header: $it" }
        }

        log.info(format("graftt surgical summary:" to "${donated.size}", ' '))
        log.info("-".repeat(LINE_WIDTH))
        log.info(header("CONFIG"))
        log.info(format("enable" to "$enable"))
        log.info(format("classDir" to "$classDir"))
        log.info(format("keepTransplants" to "$keepTransplants"))
        log.info("-".repeat(LINE_WIDTH))
        log.info(header("TRANSPLANTS"))
        donated
            .map { cn -> cn to readRecipientType(cn).andThen(this::loadClassNode).unwrap() }
            .map { (d, r) -> format(d.shortName to r.shortName) }
            .forEach(log::info)
        log.info("-".repeat(LINE_WIDTH))
    }


    private fun transplant(donor: ClassNode) {
        resultOf { donor }
            .andThen(::readRecipientType)
            .andThen(this::loadClassNode)
            .andThen { recipient -> transplant(donor, recipient) }
            .andThen(this::save)
            .onFailure(`(╯°□°）╯︵ ┻━┻`)
    }

    private fun loadClassNode(type: Type): Result<ClassNode, Msg> = resultOf {
        File(classDir, "${type.internalName}.class")
            .let(::classNode)
    }

    private fun save(cn: ClassNode) = resultOf {
        cn.toFile().writeBytes(cn.toBytes())
    }

    private fun ClassNode.toFile() = File(classDir, "$name.class").also { f->
        if (!f.exists()) throw RuntimeException("wrong path: ${f.absolutePath}")
    }

    private fun Dependency.toArtifact(): Artifact {
        infix fun Artifact.matching(dependency: Dependency): Boolean {
            return artifactId == dependency.artifactId
                && groupId == dependency.groupId
        }

        return project.artifacts.find { it matching this }
            ?: throw IllegalStateException("unable to resolve dependency: $artifactId")
    }
}
