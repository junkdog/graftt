package net.onedaybeard.graftt.classloader

import org.objectweb.asm.tree.ClassNode

interface StubberStrategy {
    fun stubClass(name: String, nodes: Map<String, ClassNode>): ByteArray
}

class IdentityStubber : StubberStrategy {
    override fun stubClass(name: String, nodes: Map<String, ClassNode>): ByteArray {
        TODO()
    }
}
