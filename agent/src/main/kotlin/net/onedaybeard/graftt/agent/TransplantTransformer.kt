package net.onedaybeard.graftt.agent

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.*
import net.onedaybeard.graftt.graft.*
import org.objectweb.asm.tree.ClassNode
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

class TransplantTransformer : ClassFileTransformer {
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
}