package net.onedaybeard.graftt.graft

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.*
import net.onedaybeard.graftt.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.commons.MethodRemapper
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.*
import kotlin.reflect.KMutableProperty

/**
 * Remaps graftable bytecode from [donor] to [recipient].
 *
 * All interfaces, methods and fields are transplanted, except
 * those annotated with [Graft.Mock]. Methods annotated with
 * [Graft.Fuse] can invoke the original [recipient] method by
 * invoking itself inside the donor's method body.
 *
 * Transplants other than [donor] can be referenced. All referenced
 * transplants are substituted with their recipient types.
 *
 * As the [recipient] is known, [donor] is not checked for
 * [Graft.Recipient].
 */
fun transplant(
    donor: ClassNode,
    recipient: ClassNode,
    remapper: Remapper
): Result<ClassNode, Msg> {

    if (donor.superName != "java/lang/Object")
        return Err(Msg.TransplantMustNotExtendClass(donor.name))

    return Ok(donor)
        .andThen(Surgery(recipient, remapper)::transplant)
        .andThen(::verify)
}

/**
 * Remaps graftable bytecode from [donor] to the [Graft.Recipient] it
 * points to. The recipient is resolved in [loadClassNode].
 */
fun transplant(
    donor: ClassNode,
    loadClassNode: (Type) -> Result<ClassNode, Msg>,
    remapper: Remapper
): Result<ClassNode, Msg> {

    return Ok(donor)
        .andThen(::readRecipientType)
        .andThen(loadClassNode)
        .andThen { recipient -> transplant(donor, recipient, remapper) }
}

/** apply [transplant] to this [ClassNode] */
fun ClassNode.fuse(transplant: Transplant.Field): Result<ClassNode, Msg> {
    return Ok(transplant)
        .andThen { removeAnnotations(it) }
        .andThen { fuseAnnotations(it) }
        .andThen { graft(it) }
}

/** apply [transplant] to this [ClassNode] */
fun ClassNode.fuse(transplant: Transplant.Method): Result<ClassNode, Msg> {

    // transplants aren't re-used once applied, but with regard
    // to the future, better safe than sorry
    return Ok(transplant.copy(node = transplant.node.copy()))
        .andThen { removeAnnotations(it) }
        .andThen { fuseAnnotations(it) }
        .andThen(::substituteTransplants)
        .andThen(::updateOriginalMethod)
        .andThen(::graft)
}


/**
 * Copies all elements from this list into [destination]. The destination list
 * is instantiated if it is `null`.
 */
fun <T> MutableList<T>.copyIntoNullable(destination: KMutableProperty<MutableList<T>?>) {
    if (destination.getter.call() == null)
        destination.setter.call(ArrayList<T>())

    destination.getter.call()!!.addAll(this)
    clear()
}

private fun ClassNode.removeAnnotations(transplant: Transplant.Field): Result<Transplant.Field, Msg> {
    val original = transplant.findMatchingNode(this)
        ?: return Ok(transplant)

    removeAnnotations(transplant.node.annotationsToRemove(),
        original::invisibleAnnotations,
        original::visibleAnnotations)

    return Ok(transplant)
}

private fun ClassNode.removeAnnotations(transplant: Transplant.Method): Result<Transplant.Method, Msg> {
    val original = transplant.findMatchingNode(this)
        ?: return Ok(transplant)

    removeAnnotations(transplant.node.annotationsToRemove(),
        original::invisibleAnnotations,
        original::visibleAnnotations)

    return Ok(transplant)
}

private fun ClassNode.removeAnnotations(transplant: Transplant.Class): Result<Transplant.Class, Msg> {
    val original = transplant.findMatchingNode(this)
        ?: return Ok(transplant)

    removeAnnotations(transplant.node.annotationsToRemove(),
        original::invisibleAnnotations,
        original::visibleAnnotations)

    return Ok(transplant)
}

private fun removeAnnotations(
    toRemove: Set<Type>,
    vararg sources: KMutableProperty<MutableList<AnnotationNode>?>
) {
    sources.forEach { source ->
        if (source.getter.call() == null)
            source.setter.call(ArrayList<Type>())

        source.getter.call()!!.removeIf { Type.getType(it.desc) in toRemove }
    }
}

private fun ClassNode.fuseAnnotations(transplant: Transplant.Method): Result<Transplant.Method, Msg> {
    val method = transplant.node
    val original = transplant.findMatchingNode(this)
        ?: return Ok(transplant)

    original.invisibleAnnotations?.copyIntoNullable(method::invisibleAnnotations)
    original.visibleAnnotations?.copyIntoNullable(method::visibleAnnotations)

    return Ok(transplant)
}

private fun ClassNode.fuseAnnotations(transplant: Transplant.Field): Result<Transplant.Field, Msg> {
    val field = transplant.node
    val original = transplant.findMatchingNode(this)
        ?: return Ok(transplant)

    original.invisibleAnnotations?.copyIntoNullable(field::invisibleAnnotations)
    original.visibleAnnotations?.copyIntoNullable(field::visibleAnnotations)

    return Ok(transplant)
}

private fun ClassNode.fuseAnnotations(transplant: Transplant.Class): Result<Transplant.Class, Msg> {
    val cls = transplant.node
    val original = transplant.findMatchingNode(this)
        ?: return Ok(transplant)

    // NB: Transplant.Field and Transplant.Method copties from the original,
    // but we can't do this for classes as we're mutating the instance directly
    cls.invisibleAnnotations?.copyIntoNullable(original::invisibleAnnotations)
    cls.visibleAnnotations?.copyIntoNullable(original::visibleAnnotations)

    original.invisibleAnnotations
        .removeIf(AnnotationNode::isGraftAnnotation)

    return Ok(transplant)
}


private fun ClassNode.validateField(transplant: Transplant.Field): Result<Transplant.Field, Msg> {
    val field = transplant.node
    val original = fields.find { it.signatureEquals(field) }

    val doFuse = field.hasAnnotation(type<Graft.Fuse>())
    val canFuse = original != null

    return when {
        doFuse && !canFuse -> Err(Msg.WrongFuseSignature(transplant.donor, field.name))
        !doFuse && canFuse -> Err(Msg.FieldAlreadyExists(transplant.donor, field.name))
        else               -> Ok(transplant)
    }
}

private fun ClassNode.validateAnnotations(
    transplant: Transplant.Field
): Result<Transplant.Field, Msg> {

    return validateAnnotations(
        transplant = transplant,
        symbolName = transplant.node.name,
        annotationsToRemove = transplant.node.annotationsToRemove(),
        originalAnnotations = transplant.findMatchingNode(this)?.annotations() ?: listOf()
    ).map { transplant }
}

private fun ClassNode.validateAnnotations(
    transplant: Transplant.Class
): Result<Transplant.Class, Msg> {

    return validateAnnotations(
        transplant = transplant,
        symbolName = transplant.node.shortName,
        annotationsToRemove = transplant.node.annotationsToRemove(),
        originalAnnotations = transplant.findMatchingNode(this)?.annotations() ?: listOf()
    ).map { transplant }
}

private fun ClassNode.validateAnnotations(
    transplant: Transplant.Method
): Result<Transplant.Method, Msg> {

    return validateAnnotations(
        transplant = transplant,
        symbolName = transplant.node.name,
        annotationsToRemove = transplant.node.annotationsToRemove(),
        originalAnnotations = transplant.findMatchingNode(this)?.annotations() ?: listOf()
    ).map { transplant }
}

private fun <T> ClassNode.validateAnnotations(
    transplant: Transplant<T>,
    symbolName: String,
    annotationsToRemove: Set<Type>,
    originalAnnotations: List<AnnotationNode>
): Result<Transplant<T>, Msg> {

    // abort early if the method/field is new
    transplant.findMatchingNode(this)
        ?: return if (annotationsToRemove.isEmpty())
            Ok(transplant)
        else
            Err(Msg.NoSuchAnnotation(annotationsToRemove.joinToString { it.className }))

    val unableToRemove = annotationsToRemove - originalAnnotations.asTypes()
    if (unableToRemove.isNotEmpty()) {
        return Err(Msg.UnableToRemoveAnnotation(
            transplant.donor, symbolName, unableToRemove.joinToString()))
    }

    if (!transplant.overwriteAnnotations) {
        val clashing = transplant.annotations()
            .filterNot(AnnotationNode::isGraftAnnotation)
            .asTypes()
            .intersect(originalAnnotations.asTypes() - annotationsToRemove)
            .map { it.internalName }

        if (clashing.isNotEmpty()) {
            return Err(Msg.AnnotationAlreadyExists(
                transplant.donor, symbolName, clashing.joinToString())
            )
        }
    }

    return Ok(transplant)
}


private fun ClassNode.updateOriginalMethod(transplant: Transplant.Method): Result<Transplant.Method, Msg> {
    val method = transplant.node
    val original = methods.find { it.signatureEquals(method) }
    if (original != null)
        original.name += "\$original"

    val doFuse = method.hasAnnotation(type<Graft.Fuse>())
    val canFuse = original != null

    val donor = transplant.donor
    return when {
        !doFuse && canFuse  -> Err(Msg.MethodAlreadyExists(donor, method.name))
        doFuse && !canFuse  -> Err(Msg.WrongFuseSignature(donor, method.name))
        !doFuse && !canFuse -> Ok(transplant)
        else -> {
            val self = transplant.transplantLookup.mapType(donor)

            val replacesOriginal = method.asSequence()
                .mapNotNull { insn -> insn as? MethodInsnNode }
                .filter { insn -> insn.owner == self }
                .filter(method::signatureEquals)
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

    mn.signature = remapper.mapSignature(mn.signature, true)
    mn.desc = remapper.mapDesc(mn.desc)
    transplant.node.accept(MethodRemapper(mn, remapper))

    return Ok(transplant.copy(node = mn))
}

// TODO: test this - or impl elsewhere (heh)?
private fun substituteTransplants(transplant: Transplant.Field): Result<Transplant.Field, Msg> {
    val fn = transplant.node.copy()
    val remapper = transplant.transplantLookup

    fn.signature = remapper.mapSignature(fn.signature, true)
    fn.desc = remapper.mapDesc(fn.desc)

    return Ok(transplant.copy(node = fn))
}


/** all methods except mocked, constructor and static initializer */
fun ClassNode.graftableMethods() = methods
    .filterNot { it.hasAnnotation(type<Graft.Mock>()) }
    .filterNot(ctorOrStaticInit)

/** all fields except mocked */
fun ClassNode.graftableFields() = fields
    .filterNot { it.hasAnnotation(type<Graft.Mock>()) }

val ClassNode.isTransplant: Boolean
    get() = readRecipientType(this) is Ok

/** returns true for any [Graft] annotations */
fun AnnotationNode.isGraftAnnotation() =
    desc.startsWith("Lnet/onedaybeard/graftt/Graft$")


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
    method.node.invisibleAnnotations?.removeIf(AnnotationNode::isGraftAnnotation)
    methods.add(method.node)
    return Ok(this)
}

/** rewrites this [ClassNode] according to [field] transplant */
private fun ClassNode.graft(field: Transplant.Field): Result<ClassNode, Msg> {
    field.node.invisibleAnnotations?.removeIf(AnnotationNode::isGraftAnnotation)
    fields.removeIf { it.name == field.node.name }
    fields.add(field.node.copy())
    return Ok(this)
}

/** valid [opcodes]: [GETSTATIC], [PUTSTATIC], [GETFIELD], [PUTFIELD] */
private fun MethodNode.fieldInsnNodes(vararg opcodes: Int) = asSequence()
    .mapNotNull { insn -> insn as? FieldInsnNode }
    .filter { fin -> fin.opcode in opcodes }
    .toList()

/** ensure no fields are initialized with a default value in the ctor or static initializer */
private fun verifyFieldNotInitialized(donor: ClassNode): (FieldNode) -> Result<FieldNode, Msg> {

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


/** Convenience class for transplanting to [recipient] */
private class Surgery(val recipient: ClassNode, val remapper: Remapper) {

    fun transplant(donor: ClassNode) = Ok(donor)
        .andThen(::classAnnotations)
        .andThen(::interfaces)
        .andThen(::fields)
        .andThen(::methods)
        .map { recipient }

    fun classAnnotations(donor: ClassNode): Result<ClassNode, Msg> {
        return Ok(Transplant.Class(donor.name, donor, remapper))
            .andThen { recipient.validateAnnotations(it) }
            .andThen { recipient.removeAnnotations(it) }
            .andThen { recipient.fuseAnnotations(it) }
            .map { donor }
    }

    fun interfaces(donor: ClassNode): Result<ClassNode, Msg> {
        fun checkRecipientInterface(iface: String) =
            if (iface !in recipient.interfaces)
                Ok(iface)
            else
                Err(Msg.InterfaceAlreadyExists(donor.name, iface))

        return donor.interfaces
            .toResultOr { Msg.None }
            .mapAll(::checkRecipientInterface)
            .map(recipient.interfaces::addAll)
            .map { donor }
            .safeRecover { donor } // not implementing additional interfaces
    }

    fun fields(donor: ClassNode): Result<ClassNode, Msg> {
        return Ok(donor)
            .map(ClassNode::graftableFields)
            .mapAll(verifyFieldNotInitialized(donor))
            .mapAll { f -> Ok(Transplant.Field(donor.name, f, remapper)) }
            .mapAll(recipient::validateField)
            .mapAll(recipient::validateAnnotations)
            .mapAll { t -> Ok(t.copy(donor = remapper.mapType(donor.name))) }
            .mapAll(recipient::fuse)
            .map { donor }
    }

    fun methods(donor: ClassNode): Result<ClassNode, Msg> {
        return Ok(donor)
            .map(ClassNode::graftableMethods)
            .mapAll { fn -> Ok(Transplant.Method(donor.name, fn, remapper)) }
            .mapAll { fn -> recipient.validateAnnotations(fn) }
            .mapAll { fn -> recipient.fuse(fn) }
            .map { donor }
    }
}

