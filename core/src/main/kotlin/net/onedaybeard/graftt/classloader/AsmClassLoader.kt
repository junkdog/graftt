package net.onedaybeard.graftt.classloader

import com.github.michaelbull.result.*
import mu.KLogger
import net.onedaybeard.graftt.*
import net.onedaybeard.graftt.graft.loadClassNode
import net.onedaybeard.graftt.graft.performGraft
import net.onedaybeard.graftt.graft.readRecipientType
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.InputStream
import java.lang.RuntimeException
import java.net.URL
import java.net.URLClassLoader

/**
 * Loads classes from supplied [nodes]. Any classes not found
 * in [nodes] - or the classpath - are defined according to
 * to the supplied [StubberStrategy].
 */
@Suppress("MemberVisibilityCanBePrivate")
//class AsmClassLoader(urls: Array<URL>, parent: ClassLoader?) : URLClassLoader(urls, ClassLoader.getSystemClassLoader()) {
class AsmClassLoader(urls: Array<URL>, parent: ClassLoader?) : URLClassLoader(urls, null) {
//    constructor(parent: ClassLoader)
//        : this(classPath(), parent)
    constructor()
        : this(classPath(), null)

//    private val logger: KLogger = makeLogger()

    val transplants: MutableMap<String, ClassNode> = hashMapOf()

    init {
        println("init")
//        logger = makeLogger()
//        logger.info { "initializing" }

        // search directories on classpath for existing transplants.
        // only considers classes having a name ending with "Transplant"
        urls.map { File(it.file) }
            .filter(File::isDirectory)
//            .also { logger.debug("directories on classpath: $it") }
            .flatMap(File::findAllTransplants)
            .forEach { cn -> readRecipientType(cn)
//                .onSuccess { logger.info("transplant: ${cn.qualifiedName} -> ${it.className}") }
                .onSuccess { transplants += it.className to cn }
            }

//        logger.info { "registered ${transplants.size} donor" }
    }

    inline fun <reified T> register() {
        resultOf { classNode<T>() }
            .andThen(::readRecipientType)
            .andThen(::loadClassNode)
            .fold(
                success =  { transplants[it.name] = it },
                failure =  { throw GraftException(Msg.MissingGraftTargetAnnotation(nameOf<T>())) })
    }

    private fun loadFromClassPath(name: String): ClassNode {
        println("loadFromClassPath($name)")

        return getResourceAsStream(name)
            .toResultOr { Msg.None }
            .map { it.use(InputStream::readBytes) }
            .map(::classReader)
            .map(::classNode)
//            .onFailure(logger::push)
            .onFailure { throw ClassNotFoundException(name) }
            .unwrap()
    }

    override fun findClass(name: String): Class<*> {
        println("findClass($name)")
        try {
            val b = transplants[name]
                .toResultOr { Msg.None }
                .andThen(::performGraft)
//                .onFailure(logger::push)
                .fold({ it }, { loadFromClassPath(name) })
                .let(ClassNode::toBytes)

//            logger.debug { "loading $name" }

            return defineClass(name, b, 0, b.size)
        } catch (e: ClassNotFoundException) {
            throw e
        } catch (e: Error) {
            throw ClassNotFoundException(name, e)
        }
    }
}


private fun classPath(): Array<URL> = System.getProperty("java.class.path")
    .split(":")
    .map(::File)
    .map { it.toURI().toURL() }
    .toTypedArray()

class GraftException(type: Msg, cause: Throwable? = null)
    : RuntimeException(type.toString(), cause)

inline fun <reified T> nameOf() = T::class.java.simpleName!!

private fun File.findAllTransplants(): List<ClassNode> {
    val found = mutableListOf<ClassNode>()

    walkBottomUp().forEach { f ->
        if (f.name.endsWith("Transplant.class"))
            found += classNode(f)
    }

    return found
}