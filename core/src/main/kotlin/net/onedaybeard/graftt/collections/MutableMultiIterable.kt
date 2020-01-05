package net.onedaybeard.graftt.collections

import java.lang.IllegalStateException

fun <T> mutableIterables(vararg iterables: MutableIterable<T>?): MutableIterable<T> {
    val curated = iterables.filterNotNull().filter(MutableIterable<T>::any)
    return if (curated.isNotEmpty())
        MutableMultiIterable(curated)
    else
        MutableNopIterable()
}

private class MutableMultiIterable<T>(
    iterables: List<MutableIterable<T>>
) : MutableIterable<T> {

    private val iterables: List<MutableIterable<T>> = iterables
        .filter(MutableIterable<T>::any)

    override fun iterator() = object : MutableIterator<T> {
        var iterators = iterables.map(MutableIterable<T>::iterator).iterator()
        var current = iterators.next()

        override fun hasNext() = current.hasNext() || iterators.hasNext()

        override fun next(): T {
            if (!current.hasNext())
                current = iterators.next()

            return current.next()
        }

        override fun remove() = current.remove()
    }
}

private class MutableNopIterable<T> : MutableIterable<T> {
    override fun iterator() = object : MutableIterator<T> {
        override fun hasNext() = false
        override fun next(): T = throw IllegalStateException()
        override fun remove()  = throw IllegalStateException()
    }
}