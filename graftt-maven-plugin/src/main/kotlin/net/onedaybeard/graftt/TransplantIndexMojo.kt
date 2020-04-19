package net.onedaybeard.graftt

import net.onedaybeard.graftt.asm.qualifiedName
import net.onedaybeard.graftt.graft.isTransplant
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase.*
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope.COMPILE
import org.objectweb.asm.tree.ClassNode
import java.io.File


/**
 * Generates a record of all transplants and writes it to`graftt.index`.
 * The `agent` reads all indices on start-up to ensure registering the transplants
 * prior to any recipient classes have been invoked.
 */
@Mojo(
    name = "generate-index",
    defaultPhase = PREPARE_PACKAGE,
    requiresDependencyResolution = COMPILE)
class GenerateIndexMojo : AbstractMojo() {

    @Parameter(property = "project.build.outputDirectory", readonly = true)
    private lateinit var classDir: File

    override fun execute() {
        classNodes(classDir)
            .filter(ClassNode::isTransplant)
            .map(ClassNode::qualifiedName)
            .joinToString("\n")
            .takeIf(String::isNotEmpty)
            ?.let { File(classDir, "graftt.index").writeText(it) }
    }
}
