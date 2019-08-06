package net.onedaybeard.graftt.agent

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.*
import net.onedaybeard.graftt.asm.classNode
import net.onedaybeard.graftt.asm.toBytes
import net.onedaybeard.graftt.graft.isTransplant
import net.onedaybeard.graftt.graft.transplant
import net.onedaybeard.graftt.graft.readRecipientType
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain


fun premain(agentArgs: String?, inst: Instrumentation) {

    inst.addTransformer(object : ClassFileTransformer {
        val log = makeLogger()

        val transplants: MutableMap<String, ClassNode> = mutableMapOf()
        val mapping: MutableMap<String, String> = mutableMapOf()

        init {
            log.info { "graftt agent preparing for surgery..." }

            parseArgs(agentArgs)
                .also(::validate)
                .let { args -> args["classpath"] ?: args["cp"] ?: listOf() }
                .map(::File)
                .forEach(this::register)
        }

        fun register(root: File) {
            log.info { "searching for transplants: $root" }

            val transplantNodes = classNodes(root)
                .filter(ClassNode::isTransplant)

            fun recipientName(cn: ClassNode): String {
                return readRecipientType(cn)
                    .unwrap()
                    .internalName
            }

            transplantNodes.associateByTo(transplants) { cn ->
                recipientName(cn)
                    .also { log.debug { "found transplant: ${cn.name} -> $it" } }
            }

            transplantNodes.associateByTo(mapping, ClassNode::name, ::recipientName)
        }

        override fun transform(
            loader: ClassLoader?,
            className: String,
            classBeingRedefined: Class<*>?,
            protectionDomain: ProtectionDomain?,
            classfileBuffer: ByteArray
        ): ByteArray {
            val cn = classNode(classfileBuffer)

            readRecipientType(cn).map { it.internalName }.onSuccess { name ->
                log.debug { "classloader touching transplant: $name" }
                mapping[cn.name] = name
                transplants[name] = cn
            }

            return transplants[className]?.let { donor ->
                transplant(donor, cn, SimpleRemapper(mapping))
                    .map(ClassNode::toBytes)
                    .mapError(Msg::toException)
                    .onFailure { log.error(it) { "failed transplant: ${donor.name} -> $className" } }
                    .onSuccess { log.info { "transplant complete: ${donor.name} -> $className" } }
                    .fold(success = { it }, failure = { null })
            } ?: classfileBuffer
        }
    })
}


private fun validate(args: Map<String, List<String>>) {
    if (args.isEmpty()) return

    val valid = listOf("classpath", "cp")

    val invalid = args.filterKeys { it !in valid }
    if (invalid.isNotEmpty())
        throw IllegalArgumentException("$valid are valid parameters, found: ${invalid.keys}")
}

private fun parseArgs(rawArgs: String?): Map<String, List<String>> {
    if (rawArgs == null) return mapOf()

    fun String.token(index: Int) = split("=")[index]

    return rawArgs
        .split(";")
        .associate { s -> s.token(0) to s.token(1).split(",")  }
}
