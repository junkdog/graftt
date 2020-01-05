package net.onedaybeard.graftt

import com.github.michaelbull.result.*
import java.lang.Exception

/** Reference-friendly [Ok] with correct [Err] type */
fun <T> ok(t: T): Result<T, Msg> = Ok(t)

/** Reference-friendly [Result] creation */
fun <T> resultOf(f: () -> T): Result<T, Msg> {
    return try {
        Ok(f())
    } catch (e: Exception) {
        Err(Msg.Error(e))
    }
}

/** Transforms [Msg] unless it is [Msg.Error] */
fun <T> Result<T, Msg>.mapSafeError(f: (Msg) -> Msg): Result<T, Msg> {
    return when (val e = getError()) {
        null         -> this
        is Msg.Error -> this
        else         -> Err(f(e))
    }
}

@Suppress("UNCHECKED_CAST")
/** Transforms [Msg] unless it is [Msg.None] or [Msg.NoSuchAnnotation] */
fun <T> Result<T, Msg>.safeRecover(f: (Msg) -> T): Result<T, Msg> {
    return when (val e = getError()) {
        null                    -> this
        is Msg.None             -> Ok(f(e))
        is Msg.NoSuchAnnotation -> Ok(f(e))
        else                    -> this
    }
}