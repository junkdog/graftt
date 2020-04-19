package net.onedaybeard.graftt.agent

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.Msg
import net.onedaybeard.graftt.asm.classNode
import net.onedaybeard.graftt.asm.toBytes
import net.onedaybeard.graftt.classNodes
import net.onedaybeard.graftt.graft.isTransplant
import net.onedaybeard.graftt.graft.readRecipientName
import net.onedaybeard.graftt.graft.transplant
import net.onedaybeard.graftt.makeLogger
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.lang.instrument.ClassFileTransformer
import java.net.URL
import java.security.ProtectionDomain

class GraftTransformer : ClassFileTransformer {
    val log = makeLogger()

    val transplants: MutableMap<String, ClassNode> = mutableMapOf()
    val mapping: MutableMap<String, String> = mutableMapOf()

    init {
        log.info { "initializing graftt agent..." }
    }

    fun register(root: File) {
        classNodes(root)
            .filter(ClassNode::isTransplant)
            .forEach(::register)
    }

    fun register(donor: ClassNode) {
        register(donor, readRecipientName(donor).unwrap())
    }

    fun register(donor: ClassNode, recipient: String) {
        log.info { "registering transplant: ${donor.name} -> $recipient" }
        transplants[recipient] = donor
        mapping[donor.name] = recipient
    }

    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray
    ): ByteArray {

        val cn = classNode(classfileBuffer)

        // check if class is in fact a transplant
        readRecipientName(cn)
            .onSuccess { recipient -> register(cn, recipient) }

        return transplants[className]?.let { donor ->
            transplant(donor, cn, SimpleRemapper(mapping))
                .map(ClassNode::toBytes)
                .mapError(Msg::toException)
                .onFailure { log.error(it) { "failed transplant: ${donor.name} -> $className" } }
                .onSuccess { log.info { "transplant complete: ${donor.name} -> $className" } }
                .get()
        } ?: classfileBuffer
    }
}

/** searches classpath for `/graftt.index */
fun registerTransplantIndices() {
    loadResources("graftt.index")
        .map { index -> String(index) }
        .flatMap(String::lines)
        .filterNot { line -> line.startsWith("#") }
        .filter(String::isNotBlank)
        .map { Class.forName(it) } // registers via classloader
}

private fun loadResources(
    name: String,
    classLoader: ClassLoader = ClassLoader.getSystemClassLoader()
): List<ByteArray> {
    return classLoader
        .getResources(name)
        .asSequence()
        .map(URL::readBytes)
        .toList()
}
