package net.onedaybeard.graftt

import com.github.michaelbull.result.*
import java.lang.Exception

fun <T> resultOf(f: () -> T): Result<T, Msg> {
    return try {
        Ok(f())
    } catch (e: Exception) {
        Err(Msg.Error(e))
    }
}

/**
 * Creates a [Result] from [f]. To chain with other related results,
 * [require] the successful completion of previous results; any error
 * [Msg]:s propagate to this result.
 */
fun <T> resultOf(vararg require: Result<*, Msg>, f: () -> T): Result<T, Msg> {
    return when (val e = require.find { it is Err<Msg> }) {
        null -> resultOf(f)
        else -> Err(e.unwrapError()).andThen { resultOf(f) }
    }
}

@Suppress("UNCHECKED_CAST")
/** Transforms [Msg] unless it is [Msg.Error] */
fun <T, U> Result<T, Msg>.mapSafeError(f: (Msg) -> U): Result<T, U> {
    return when (val e = getError()) {
        null         -> this
        is Msg.Error -> this
        else         -> Err(f(e))
    } as Result<T, U>
}

@Suppress("UNCHECKED_CAST")
/** Transforms [Msg] unless it is [Msg.Error] */
fun <T> Result<T, Msg>.safeRecover(f: (Msg) -> T): Result<T, Msg> {
    return when (val e = getError()) {
        null        -> this
        is Msg.None -> Ok(f(e))
        else        -> this
    }
}

/** Lazily chains multiple [Result]s into a list of all values or [Err] */
fun <V, E> combine(vararg results: () -> Result<V, E>): Result<List<V>, E> {
    return Ok(results.map { f ->
        when (val result = f()) {
            is Ok -> result.value
            is Err -> return result
        }
    })
}