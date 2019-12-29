package net.onedaybeard.graftt

import com.github.michaelbull.result.getError
import net.onedaybeard.graftt.asm.classNode
import org.objectweb.asm.tree.ClassNode
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNull


infix fun ClassNode.bytecodeEquals(kClass: KClass<*>) {
    assertNull(verify(this).getError())

    assertEquals(
        classNode(kClass).apply { sourceFile = null }.toDebugString(),
        toDebugString())
}
