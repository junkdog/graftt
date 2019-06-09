package net.onedaybeard.graftt.classloader

import net.onedaybeard.graftt.qualifiedName
import net.onedaybeard.graftt.toBytes
import net.onedaybeard.graftt.verify
import org.objectweb.asm.tree.ClassNode
import java.lang.RuntimeException

/**
 * Loads classes from supplied [nodes]. Any classes not found
 * in [nodes] - or the classpath - are defined according to
 * to the supplied [StubberStrategy].
 */
@Suppress("MemberVisibilityCanBePrivate")
class AsmClassLoader(
    val nodes: Map<String, ClassNode>,
    stubber: StubberStrategy
) : ClassLoader(), StubberStrategy by stubber {

    constructor(nodes: List<ClassNode>,
                stubber: StubberStrategy = IdentityStubber())
        : this(nodes.associateBy(ClassNode::qualifiedName), stubber)

    init {
        nodes.forEach { (_, n) -> verify(n, this) }
    }

    private fun loadRelatedClasses(cn: ClassNode) {
        @Suppress("NullableBooleanElvis")
        if (cn.superName?.startsWith("java/lang/") ?: true)
            return

        val parent = cn.superName.replace('/', '.')
        findLoadedClass(parent) ?: findClass(parent)
    }

    override fun findClass(name: String): Class<*> {
        val b = nodes[name]?.let { node ->
//            loadRelatedClasses(node)
            println("(CLS) $name ")
            node.toBytes()
        } ?: let {
            println("(GEN) $name ")
            stubClass(name, nodes)
        }
        try {
            return defineClass(name, b, 0, b.size)
        } catch (e: Error) {
            throw RuntimeException(e)
        }
    }
}