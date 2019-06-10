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

    val fusedMethods = resultOf(recipient) { donor }
        .map(ClassNode::graftableMethods)
        .map { fns -> fns.map { Transplant.Method(donor.name, it) } }
        .mapAll { recipient.unwrap().fuse(it) }

    val fusedFields = resultOf(fusedMethods) { donor }
        .map(ClassNode::graftableFields)
        .map { f -> f.map { Transplant.Field(donor.name, it) } }
        .mapAll { recipient.unwrap().fuse(it) }

    return fusedFields.andThen { verify(recipient.unwrap()) }
}

fun ClassNode.graft(method: Transplant.Method) {
    methods.add(graft(name, method))
}

fun ClassNode.graft(field: Transplant.Field) {
    fields.add(field.node.copy())
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


fun ClassNode.fuse(transplant: Transplant.Field): Result<ClassNode, Msg> {
    if (methods.any { it.name == transplant.node.name })
        return Err(Msg.FieldAlreadyExists(transplant.node))

    graft(transplant)

    return Ok(this)
}

fun ClassNode.fuse(transplant: Transplant.Method): Result<ClassNode, Msg> {
    val original = methods.find { it.signatureEquals(transplant.node) }
    if (original != null)
        original.name += "\$original"

    val t = transplant.copy(node = transplant.node.copy())
    val doFuse = t.node.hasAnnotation(type<Graft.Fuse>())
    val canFuse = original != null

    val operation: Result<ClassNode, Msg> = when {
        !doFuse && canFuse -> Err(Msg.MethodAlreadyExists(t.node))
        doFuse && !canFuse -> Err(Msg.WrongFuseSignature(t.node))
        doFuse && canFuse  -> {
            t.node.asSequence()
                .mapNotNull { insn -> insn as? MethodInsnNode }
                .filter { insn -> t.node.signatureEquals(insn) }
                .forEach { it.name += "\$original" }
            Ok(this)
        }
        else               -> Ok(this)
    }

    t.node.asSequence()

    return operation
        .andThen { resultOf { graft(t) } }
        .map { this }
}


fun ClassNode.graftableMethods() = methods
        .filterNot { it.hasAnnotation(type<Graft.Mock>()) }
        .filterNot { "<init>" in it.name }
        .filterNot { "<clinit>" in it.name }

fun ClassNode.graftableFields() = fields
        .filterNot { it.hasAnnotation(type<Graft.Mock>()) }

fun readTargetType(donor: ClassNode): Result<Type, Msg> {
    return donor
        .invisibleAnnotations.toResultOr { Msg.None }
        .andThen { it.findAnnotation<Graft.Recipient>() }
        .andThen { it.get<Type>("value") }
        .mapSafeError { Msg.MissingGraftTargetAnnotation(donor) }
}

