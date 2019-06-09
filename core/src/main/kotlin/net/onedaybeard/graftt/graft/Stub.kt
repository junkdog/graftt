package net.onedaybeard.graftt.graft

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.*
import org.objectweb.asm.Type
import org.objectweb.asm.commons.MethodRemapper
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode


// todo: fix proper
private fun loadClassNode(type: Type) = resultOf {
    Msg::class.java
        .getResourceAsStream("/${type.internalName}.class")
        .let(::classNode)
}

// todo: this belongs in test on graft uses the proper classloader
private class ByteClassLoader : ClassLoader() {
    fun loadClass(cn: ClassNode): Class<*> {
        val bytes = cn.toBytes()
        val clazz = defineClass(cn.qualifiedName, bytes, 0, bytes.size)
        resolveClass(clazz)
        return clazz
    }
}

fun performGraft(donor: ClassNode): Result<ClassNode, Msg> {
    val recipient = resultOf { donor }
        .andThen(::readTargetType)
        .andThen(::loadClassNode)

    return resultOf(recipient) { donor }
        .map(ClassNode::graftableMethods)
        .map { fns -> fns.map { Transplant.Method(donor.name, it) } }
        .map { fns -> fns.forEach { recipient.unwrap().fuse(it) } }
        .andThen { verify(recipient.unwrap()) }
}

fun ClassNode.graft(method: Transplant.Method) {
    methods.add(graft(name, method))
}

private fun graft(name: String, transplant: Transplant.Method): MethodNode {
    val original = transplant.node

    val mn = MethodNode(
        original.access,
        original.name,
        original.desc,
        original.signature,
        original.exceptions?.toTypedArray())

    val remapper = SimpleRemapper(transplant.donor, name)
    original.accept(MethodRemapper(mn, remapper))

    return mn
}

fun ClassNode.fuse(transplant: Transplant.Method) {
    val original = methods.find { it.signatureEquals(transplant.node) }
    if (original != null)
        original.name += "\$original"

    val t = transplant.copy()
    t.node.asSequence()
        .mapNotNull { insn -> insn as? MethodInsnNode }
        .filter { insn -> t.node.signatureEquals(insn) }
        .forEach { it.name += "\$original" }

    graft(t)
}


fun ClassNode.graftableMethods() =
    methods.filter { it.hasAnnotation(type<Graft.Fuse>()) }

fun readTargetType(donor: ClassNode): Result<Type, Msg> {
    return donor
        .invisibleAnnotations.toResultOr { Msg.None }
        .andThen { it.findAnnotation<Graft.Target>() }
        .andThen { it.get<Type>("value") }
        .mapSafeError { Msg.MissingGraftTargetAnnotation(donor) }
}

