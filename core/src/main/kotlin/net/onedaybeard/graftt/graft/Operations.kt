package net.onedaybeard.graftt.graft

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.commons.MethodRemapper
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.*

/**
 * Remaps graftable bytecode from [donor] to this [recipient].
 *
 * All interfaces, methods and fields are transplanted, except
 * those annotated with [Graft.Mock]. Methods annotated with
 * [Graft.Fuse] can invoke the original [recipient] method by
 * invoking itself inside the donor's method body.
 *
 * As the [recipient] is known, [Graft.Recipient] is not required
 * on [donor].
 */
fun transplant(donor: ClassNode,
               recipient: ClassNode,
               remapper: Remapper): Result<ClassNode, Msg> {
    if (donor.superName != "java/lang/Object")
        return Err(Msg.TransplantMustNotExtendClass(donor.name))

    fun checkRecipientInterface(iface: String) =
        if (iface !in recipient.interfaces)
            Ok(iface)
        else
            Err(Msg.InterfaceAlreadyExists(donor.name, iface))

    val fusedInterfaces = donor.interfaces
        .toResultOr { Msg.None }
        .mapAll(::checkRecipientInterface)
        .map(recipient.interfaces::addAll)
        .safeRecover { donor }

    val fusedFields = resultOf(fusedInterfaces) { donor }
        .map(ClassNode::graftableFields)
        .mapAll(verifyFieldsNotInitialized(donor))
        .mapAll { f -> Ok(Transplant.Field(donor.name, f)) }
        .mapAll(recipient::verifyFieldNotPresent)
        .mapAll { t -> Ok(t.copy(donor = remapper.mapType(donor.name))) }
        .mapAll(recipient::fuse)

    val fusedMethods = resultOf(fusedFields) { donor }
        .map(ClassNode::graftableMethods)
        .mapAll { fn -> Ok(Transplant.Method(donor.name, fn, remapper)) }
        .mapAll(recipient::fuse)

    return fusedMethods.map { recipient }
//        .andThen { verify(recipient) }
}

/**
 * Remaps graftable bytecode from [donor] to the [Graft.Recipient] it
 * points to. The recipient is resolved in [loadClassNode].
 */
fun transplant(donor: ClassNode,
               loadClassNode: (Type) -> Result<ClassNode, Msg>,
               remapper: Remapper
): Result<ClassNode, Msg> {

    return Ok(donor)
        .andThen(::readRecipientType)
        .andThen(loadClassNode)
        .andThen { recipient -> transplant(donor, recipient, remapper) }
}



fun ClassNode.verifyFieldNotPresent(transplant: Transplant.Field): Result<Transplant.Field, Msg> {
    return if (fields.none { it.name == transplant.node.name })
        Ok(transplant)
    else
        Err(Msg.FieldAlreadyExists(transplant.donor, transplant.node.name))
}

/** apply [transplant] to this [ClassNode] */
fun ClassNode.fuse(transplant: Transplant.Field) = graft(transplant)

/** apply [transplant] to this [ClassNode] */
fun ClassNode.fuse(transplant: Transplant.Method): Result<ClassNode, Msg> {

    // transplants aren't re-used once applied, but with regard
    // to the future, better safe than sorry
    return Ok(transplant.copy(node = transplant.node.copy()))
        .andThen(::substituteTransplants)
        .andThen(this::updateAndVerifyMethod)
        .andThen(this::graft)
}

private fun ClassNode.updateAndVerifyMethod(transplant: Transplant.Method): Result<Transplant.Method, Msg> {
    val original = methods.find { it.signatureEquals(transplant.node) }
    if (original != null)
        original.name += "\$original"

    val doFuse = transplant.node.hasAnnotation(type<Graft.Fuse>())
    val canFuse = original != null

    val donor = transplant.donor
    return when {
        !doFuse && canFuse  -> Err(Msg.MethodAlreadyExists(donor, transplant.node.name))
        doFuse && !canFuse  -> Err(Msg.WrongFuseSignature(donor, transplant.node.name))
        !doFuse && !canFuse -> Ok(transplant)
        else -> {
            val self = transplant.transplantLookup.mapType(donor)

            val replacesOriginal = transplant.node.asSequence()
                .mapNotNull { insn -> insn as? MethodInsnNode }
                .filter { insn -> insn.owner == self }
                .filter(transplant.node::signatureEquals)
                .onEach { it.name = original!!.name }
                .count() == 0

            if (replacesOriginal)
                methods.remove(original)

            Ok(transplant)
        }
    }
}

private fun substituteTransplants(transplant: Transplant.Method): Result<Transplant.Method, Msg> {
    val mn = transplant.node.copy(copyInsn = false)
    val remapper = transplant.transplantLookup

    mn.signature = remapper.mapSignature(mn.signature, false)
    mn.desc = remapper.mapDesc(mn.desc)
    transplant.node.accept(MethodRemapper(mn, remapper))

    return Ok(transplant.copy(node = mn))
}


/** all methods except mocked, constructor and static initializer */
fun ClassNode.graftableMethods() = methods
    .filterNot { it.hasAnnotation(type<Graft.Mock>()) }
    .filterNot(ctorOrStaticInit)

/** all fields except mocked */
fun ClassNode.graftableFields() = fields
    .filterNot { it.hasAnnotation(type<Graft.Mock>()) }

val ClassNode.isTransplant: Boolean
    get() = readRecipientType(this).get() != null

/** scans [donor] for [Graft.Recipient] and returns its [Type] */
fun readRecipientType(donor: ClassNode): Result<Type, Msg> {
    return donor
        .invisibleAnnotations.toResultOr { Msg.None }
        .andThen { it.findAnnotation<Graft.Recipient>() }
        .andThen { it.get<Type>("value") }
        .mapSafeError { Msg.MissingGraftTargetAnnotation(donor.name) }
}


/** rewrites this [ClassNode] according to [method] transplant */
private fun ClassNode.graft(method: Transplant.Method): Result<ClassNode, Msg> {
    methods.add(method.node)
    return Ok(this)
}

/** rewrites this [ClassNode] according to [field] transplant */
private fun ClassNode.graft(field: Transplant.Field): Result<ClassNode, Msg> {
    fields.add(field.node.copy())
    return Ok(this)
}

/** valid [opcodes]: [GETSTATIC], [PUTSTATIC], [GETFIELD], [PUTFIELD] */
private fun MethodNode.fieldInsnNodes(vararg opcodes: Int) = asSequence()
    .mapNotNull { insn -> insn as? FieldInsnNode }
    .filter { fin -> fin.opcode in opcodes }
    .toList()

/** ensure no fields are initialized with a default value in the ctor or static initializer */
private fun verifyFieldsNotInitialized(donor: ClassNode): (FieldNode) -> Result<FieldNode, Msg> {

    val initializedByCtor = donor.methods
        .filter(ctorOrStaticInit)
        .flatMap { ctor -> ctor.fieldInsnNodes(PUTFIELD, PUTSTATIC) }
        .map(FieldInsnNode::name)

    return { f: FieldNode ->
        if (f.name !in initializedByCtor)
            Ok(f)
        else
            Err(Msg.FieldDefaultValueNotSupported(donor.name, f.name))
    }
}

private val ctorOrStaticInit: (MethodNode) -> Boolean =
    anyOf({ it.name == "<clinit>" },
          { it.name == "<init>" })
