package net.onedaybeard.graftt.classloader

import net.onedaybeard.graftt.toBytes
import org.objectweb.asm.tree.ClassNode

interface AsmStrategy {
    fun stubClass(name: String): ByteArray
}

