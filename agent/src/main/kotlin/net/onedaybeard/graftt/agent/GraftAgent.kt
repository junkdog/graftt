package net.onedaybeard.graftt.agent

import com.github.michaelbull.result.map
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.unwrap
import net.onedaybeard.graftt.`(╯°□°）╯︵ ┻━┻`
import net.onedaybeard.graftt.classNode
import net.onedaybeard.graftt.graft.performGraft
import net.onedaybeard.graftt.graft.readRecipientType
import net.onedaybeard.graftt.toBytes
import org.objectweb.asm.tree.ClassNode
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain

fun premain(agentArgs: String?, inst: Instrumentation) {
    inst.addTransformer(object : ClassFileTransformer {
        val transplants: MutableMap<String, ClassNode> = mutableMapOf()

        init {
            println("graftt agent preparing for surgery...")
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
                .onSuccess { transplants[it.internalName] = cn }

            return transplants[className]?.let { donor ->
                performGraft(donor, classNode(classfileBuffer))
                    .map(ClassNode::toBytes)
                    .onFailure(`(╯°□°）╯︵ ┻━┻`)
                    .unwrap()
            } ?: classfileBuffer
        }
    })
}
