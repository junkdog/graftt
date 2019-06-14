package net.onedaybeard.graftt.agent

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.classNode
import net.onedaybeard.graftt.graft.*
import net.onedaybeard.graftt.resultOf
import net.onedaybeard.graftt.toBytes
import net.onedaybeard.graftt.verify
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
            transplant(donor, classNode(classfileBuffer))
        } ?: classfileBuffer
    }

    fun transplant(donor: ClassNode, recipient: ClassNode): ByteArray {
        val fusedMethods = resultOf { donor }
            .map(ClassNode::graftableMethods)
            .map { fns -> fns.map { Transplant.Method(donor.name, it) } }
            .mapAll(recipient::fuse)

        val fusedFields = resultOf(fusedMethods) { donor }
            .map(ClassNode::graftableFields)
            .map { f -> f.map { Transplant.Field(donor.name, it) } }
            .mapAll(recipient::fuse)

        return fusedFields
            .andThen { verify(recipient) }
            .map(ClassNode::toBytes)
            .onFailure(::println)
            .fold({ it }, { recipient.toBytes() }) //
    }
}