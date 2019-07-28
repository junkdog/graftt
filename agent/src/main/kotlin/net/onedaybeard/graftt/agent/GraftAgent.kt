package net.onedaybeard.graftt.agent

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.*
import net.onedaybeard.graftt.graft.isTransplant
import net.onedaybeard.graftt.graft.transplant
import net.onedaybeard.graftt.graft.readRecipientType
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain


fun premain(agentArgs: String?, inst: Instrumentation) {

    inst.addTransformer(object : ClassFileTransformer {
        val log = makeLogger()

        val transplants: MutableMap<String, ClassNode> = mutableMapOf()
        val transplantToRecipient: MutableMap<Type, Type> = mutableMapOf()

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

            transplantNodes.associateByTo(transplants) { cn ->
                readRecipientType(cn)
                    .unwrap()
                    .internalName
                    .also { log.debug { "found transplant: $it" } }
            }

            transplantNodes.associateByTo(transplantToRecipient, ClassNode::type) { cn ->
                readRecipientType(cn).unwrap()
            }
        }

        override fun transform(
            loader: ClassLoader?,
            className: String,
            classBeingRedefined: Class<*>?,
            protectionDomain: ProtectionDomain?,
            classfileBuffer: ByteArray
        ): ByteArray {
            val cn = classNode(classfileBuffer)

            readRecipientType(cn)
                .onSuccess { log.debug { "classloader touching transplant: ${it.internalName}" } }
                .onSuccess { type -> transplantToRecipient[cn.type] = type }
                .onSuccess { type -> transplants[type.internalName] = cn }

            return transplants[className]?.let { donor ->
                transplant(donor, classNode(classfileBuffer), transplantToRecipient)
                    .map(ClassNode::toBytes)
                    .onFailure(`(╯°□°）╯︵ ┻━┻`)
                    .onSuccess { log.info { "transplant complete: $donor" } }
                    .unwrap()
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

    fun String.token(index: Int) = split(":")[index]

    return rawArgs
        .split(",")
        .associate { s -> s.token(0) to s.token(1).split(";")  }
}
