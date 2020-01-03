package net.onedaybeard.graftt.graft

import com.github.michaelbull.result.*
import net.onedaybeard.graftt.*
import net.onedaybeard.graftt.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AnnotationRemapper
import org.objectweb.asm.commons.MethodRemapper
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.*
import kotlin.reflect.KMutableProperty

/**
 * Remaps graftable bytecode from [donor] to [recipient]. The
 * [donor] instance is not mutated by this operation.
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

    return Ok(donor.copy())
        .andThen(Surgery(recipient, remapper)::transplant)
        .andThen(::verify)
}

/**
 * Remaps graftable bytecode from [donor] to the [Graft.Recipient] it
 * points to. The recipient is resolved in [loadClassNode].
 *
 * The [donor] instance is not mutated by this operation.
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

/**
 * Copies all elements from this list into [destination]. The destination list
 * is instantiated if it is `null`.
 */
internal fun <T> Iterable<T>.copyIntoNullable(destination: KMutableProperty<MutableList<T>?>) {
    if (destination.getter.call() == null)
        destination.setter.call(mutableListOf<T>())

    destination.getter.call()!!.addAll(this)
}

private fun <T : Transplant<*>> ClassNode.removeRequestedAnnotations(transplant: T): Result<T, Msg> {
    val toRemove = transplant.annotationsToRemove()
    fun remove(annotations: KMutableProperty<MutableList<AnnotationNode>?>) {
        if (annotations.getter.call() == null)
            annotations.setter.call(ArrayList<Type>())

        annotations.getter.call()!!.removeIf { it.type in toRemove }
    }

    when (val n = transplant.findMatchingNode(this)) {
        is ClassNode  -> n::invisibleAnnotations to n::visibleAnnotations
        is MethodNode -> n::invisibleAnnotations to n::visibleAnnotations
        is FieldNode  -> n::invisibleAnnotations to n::visibleAnnotations
        else          -> return Ok(transplant)
    }.toList().forEach(::remove)

    return Ok(transplant)
}

@Suppress("UnnecessaryVariable")
private fun <T : Transplant<*>> ClassNode.fuseAnnotations(transplant: T): Result<T, Msg> {
    when (val t = transplant.takeIf { it.findMatchingNode(this) != null }) {
        is Transplant.Class  -> {
            // NB: Transplant.Field and Transplant.Method copies from the original,
            // but we can't do this here as we're mutating the recipient directly
            val original = t.findMatchingNode(this)!!
            t.node.invisibleAnnotations?.removeIf(AnnotationNode::isGraftAnnotation)
            t.node.invisibleAnnotations?.copyIntoNullable(original::invisibleAnnotations)
            t.node.visibleAnnotations?.copyIntoNullable(original::visibleAnnotations)
        }
        is Transplant.Field  -> {
            val original = t.findMatchingNode(this)!!
            (original.invisibleAnnotations ?: listOf())
                .filterNot(AnnotationNode::isGraftAnnotation)
                .copyIntoNullable(t.node::invisibleAnnotations)
            original.visibleAnnotations?.copyIntoNullable(t.node::visibleAnnotations)
        }
        is Transplant.Method -> {
            val original = t.findMatchingNode(this)!!
            (original.invisibleAnnotations ?: listOf())
                .filterNot(AnnotationNode::isGraftAnnotation)
                .copyIntoNullable(t.node::invisibleAnnotations)
            original.visibleAnnotations?.copyIntoNullable(t.node::visibleAnnotations)
        }
        null                 -> Unit
        else                 -> throw Error("$t")
    }

    return Ok(transplant)
}

private fun ClassNode.validateField(transplant: Transplant.Field): Result<Transplant.Field, Msg> {
    val field = transplant.node
    val doFuse = field.hasAnnotation(type<Graft.Fuse>())
    val canFuse = fields.find(field::signatureEquals) != null

    return when {
        doFuse && !canFuse -> Err(Msg.WrongFuseSignature(transplant.donor, field.name))
        !doFuse && canFuse -> Err(Msg.FieldAlreadyExists(transplant.donor, field.name))
        else               -> Ok(transplant)
    }
}

/** ensure annotations on [transplant] don't clash with annotations on recipient  */
fun <T : Transplant<*>> ClassNode.validateAnnotations(
    transplant: T
): Result<T, Msg> {

    // abort early if the method/field is new
    val annotationsToRemove = transplant.annotationsToRemove()
    val recipientNode = transplant.findMatchingNode(this)
        ?: return if (annotationsToRemove.none())
            Ok(transplant)
        else
            Err(Msg.NoSuchAnnotation(annotationsToRemove.joinToString { it.className }))

    val originalAnnotations = when (recipientNode) {
        is ClassNode  -> recipientNode.annotations()
        is FieldNode  -> recipientNode.annotations()
        is MethodNode -> recipientNode.annotations()
        else          -> throw Error("$recipientNode")
    }
    val unableToRemove = annotationsToRemove - originalAnnotations.asTypes()
    if (unableToRemove.isNotEmpty()) {
        return Err(Msg.UnableToRemoveAnnotation(
            transplant.donor, transplant.name, unableToRemove.joinToString()))
    }

    if (!transplant.overwriteAnnotations) {
        val clashing = transplant.annotations()
            .filterNot(AnnotationNode::isGraftAnnotation)
            .asTypes()
            .intersect(originalAnnotations.asTypes() - annotationsToRemove)
            .map { it.internalName }

        if (clashing.isNotEmpty()) {
            return Err(Msg.AnnotationAlreadyExists(
                transplant.donor, transplant.name, clashing.joinToString())
            )
        }
    }

    return Ok(transplant)
}

private fun ClassNode.updateOriginalMethod(transplant: Transplant<MethodNode>): Result<Transplant.Method, Msg> {
    val method = transplant.node
    val original = methods.find(method::signatureEquals)
        ?.apply { name += "\$original" }

    val doFuse = method.hasAnnotation(type<Graft.Fuse>())
    val canFuse = original != null

    val donor = transplant.donor
    return when {
        !doFuse && canFuse  -> Err(Msg.MethodAlreadyExists(donor, method.name))
        doFuse && !canFuse  -> Err(Msg.WrongFuseSignature(donor, method.name))
        !doFuse && !canFuse -> Ok(transplant as Transplant.Method)
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

            Ok(transplant as Transplant.Method)
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

private fun substituteTransplants(transplant: Transplant.Field): Result<Transplant.Field, Msg> {
    val fn = transplant.node.copy()
    val remapper = transplant.transplantLookup

    fn.desc = remapper.mapDesc(fn.desc)
    if (fn.signature != null)
        fn.signature = remapper.mapSignature(fn.signature, true)

    return Ok(transplant.copy(node = fn))
}

/** Returns a copy with all transplant annotation types resolved to recipient types */
@Suppress("UNCHECKED_CAST")
private fun <T : Transplant<*>> T.substituteAnnotations(): Result<T, Msg> {
    return Ok((copy() as T).apply {
        annotations().forEach { an ->
            val updated = AnnotationNode(an.desc)
            an.accept(AnnotationRemapper(updated, transplantLookup))
            an.values = updated.values
        }
    })
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
        .andThen { it.readType(Graft.Recipient::value) }
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

    fun <T : Transplant<*>> validateAndFuseAnnotations(transplant: T): Result<T, Msg> = Ok(transplant)
        .andThen(Transplant<*>::substituteAnnotations)
        .andThen(recipient::validateAnnotations)
        .andThen(recipient::removeRequestedAnnotations)
        .andThen(recipient::fuseAnnotations) as Result<T, Msg>

    fun classAnnotations(donor: ClassNode): Result<ClassNode, Msg> {
        return Ok(Transplant.Class(donor.name, donor, remapper))
            .andThen { validateAndFuseAnnotations(it) }
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
            .mapAll { f -> Ok(Transplant.Field(donor.name, f.copy(), remapper)) }
            .mapAll(::substituteTransplants)
            .mapAll(recipient::validateField)
            .mapAll(::validateAndFuseAnnotations)
            .mapAll { f -> recipient.graft(f as Transplant.Field) }
            .map { donor }
    }

    fun methods(donor: ClassNode): Result<ClassNode, Msg> {
        return Ok(donor)
            .map(ClassNode::graftableMethods)
            .mapAll { fn -> Ok(Transplant.Method(donor.name, fn, remapper)) }
            .mapAll { fn -> substituteTransplants(fn) }
            .mapAll { fn -> validateAndFuseAnnotations(fn) }
            .mapAll(recipient::updateOriginalMethod)
            .mapAll(recipient::graft)
            .map { donor }
    }
}