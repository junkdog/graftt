package net.onedaybeard.graftt.graft

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.*
import org.objectweb.asm.Type
import org.objectweb.asm.commons.MethodRemapper
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode


fun performGraft(donor: ClassNode, recipient: ClassNode): Result<ClassNode, Msg> {
    fun checkRecipientInterface(iface: String) =
        if (iface !in recipient.interfaces)
            Ok(iface)
        else
            Err(Msg.InterfaceAlreadyExists(iface))

    val fusedInterfaces = donor.interfaces
        .toResultOr { Msg.None }
        .mapAll(::checkRecipientInterface)
        .map { recipient.interfaces.addAll(it) }
        .map { donor }
        .safeRecover { donor }

    val fusedMethods = resultOf(fusedInterfaces) { donor }
        .map(ClassNode::graftableMethods)
        .map { fns -> fns.map { Transplant.Method(donor.name, it) } }
        .mapAll(recipient::fuse)

    val fusedFields = resultOf(fusedMethods) { donor }
        .map(ClassNode::graftableFields)
        .map { f -> f.map { Transplant.Field(donor.name, it) } }
        .mapAll(recipient::fuse)

    return fusedFields
        .andThen { verify(recipient) }
}

fun performGraft(donor: ClassNode,
                 loadClassNode: (Type) -> Result<ClassNode, Msg>): Result<ClassNode, Msg> {
    return resultOf { donor }
        .andThen(::readRecipientType)
        .andThen(loadClassNode)
        .andThen { recipient -> performGraft(donor, recipient) }
}

/** rewrites this [ClassNode] according to [method] transplant */
fun ClassNode.graft(method: Transplant.Method) {
    methods.add(graft(name, method))
}

/** rewrites this [ClassNode] according to [field] transplant */
fun ClassNode.graft(field: Transplant.Field) {
    fields.add(field.node.copy())
}

private fun graft(name: String, transplant: Transplant.Method): MethodNode {
    val original = transplant.node
    val mn = original.copy(copyInsn = false)

    val remapper = SimpleRemapper(transplant.donor, name)
    original.accept(MethodRemapper(mn, remapper))

    return mn
}

/** apply [transplant] to this [ClassNode] */
fun ClassNode.fuse(transplant: Transplant.Field): Result<ClassNode, Msg> {
    if (methods.any { it.name == transplant.node.name })
        return Err(Msg.FieldAlreadyExists(transplant.node))

    graft(transplant)

    return Ok(this)
}

/** apply [transplant] to this [ClassNode] */
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
            val replacesOriginal = t.node.asSequence()
                .mapNotNull { insn -> insn as? MethodInsnNode }
                .filter { insn -> t.node.signatureEquals(insn) }
                .filter { insn -> insn.owner == t.donor }
                .onEach { it.name = original!!.name }
                .count() == 0

            if (replacesOriginal)
                methods.remove(original)

            Ok(this)
        }
        else               -> Ok(this)
    }

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

fun readRecipientType(donor: ClassNode): Result<Type, Msg> {
    return donor
        .invisibleAnnotations.toResultOr { Msg.None }
        .andThen { it.findAnnotation<Graft.Recipient>() }
        .andThen { it.get<Type>("value") }
        .mapSafeError { Msg.MissingGraftTargetAnnotation(donor.name) }
}

