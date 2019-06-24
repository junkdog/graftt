package net.onedaybeard.graftt

import mu.KLogger
import mu.KotlinLogging

inline fun <reified T : Any> T.makeLogger() =
    KotlinLogging.logger(T::class.java.simpleName!!.split(".").last())

fun makeLogger(name: String) = KotlinLogging.logger(name)

fun KLogger.push(msg: Msg) = when (msg) {
    is Msg.Error -> error(msg.e.message, msg.e)
    is Msg.None  -> trace(toString())
    else         -> warn(msg.toString())
}
