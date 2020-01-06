package net.onedaybeard.graftt.collections

import java.lang.IllegalStateException


internal class MutableMultiIterable<T>(
    private val iterables: List<MutableIterable<T>?>
) : MutableIterable<T> {

    @Suppress("UNCHECKED_CAST")
    override fun iterator(): MutableIterator<T> = iterables.safeIterators()
        ?.let { MutableMultiIterator(it as Iterator<MutableIterator<T>>) }
        ?: NopIterator()
}

internal class MultiIterable<T>(
    private val iterables: List<Iterable<T>?>
) : Iterable<T> {

    override fun iterator(): Iterator<T> = iterables.safeIterators()
        ?.let { MultiIterator(it) }
        ?: NopIterator()
}

private fun <T> List<Iterable<T>?>.safeIterators(): Iterator<Iterator<T>>? {
    return filterNotNull()
        .filter(Iterable<T>::any)
        .map(Iterable<T>::iterator)
        .iterator()
        .takeIf(Iterator<Iterator<T>>::hasNext)
}

private open class MultiIterator<T>(
    open val iterators: Iterator<Iterator<T>>,
    var current: Iterator<T> = iterators.next()
) : Iterator<T> {

    override fun hasNext() = current.hasNext() || iterators.hasNext()

    override fun next(): T {
        if (!current.hasNext())
            current = iterators.next()

        return current.next()
    }
}

private class MutableMultiIterator<T>(
    override val iterators: Iterator<MutableIterator<T>>
) : MultiIterator<T>(iterators), MutableIterator<T> {
    override fun remove() = (current as MutableIterator<T>).remove()
}

private class NopIterator<T> : MutableIterator<T> {
    override fun hasNext() = false
    override fun next(): T = throw IllegalStateException()
    override fun remove()  = throw IllegalStateException()
}
