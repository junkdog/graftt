package net.onedaybeard.graftt.agent

import java.io.File
import java.lang.instrument.Instrumentation


fun premain(agentArgs: String?, inst: Instrumentation) {
    GraftTransformer().apply {
        inst.addTransformer(this)

        // transplants from agent args
        parseArgs(agentArgs)
            .also(::validate)
            .let { args -> args["classpath"] ?: args["cp"] ?: listOf() }
            .map(::File)
            .forEach(::register)

        // transplants from /graftt.index files
        registerTransplantIndices()
    }
}

private fun validate(args: Map<String, List<String>>) {
    val valid = listOf("classpath", "cp")

    val invalid = args.filterKeys { it !in valid }
    if (invalid.isNotEmpty())
        throw IllegalArgumentException("$valid are valid parameters, found: ${invalid.keys}")
}

/** argument format: `param1=value1,value2;param2=value3` */
private fun parseArgs(rawArgs: String?): Map<String, List<String>> {
    if (rawArgs == null) return mapOf()

    fun String.token(index: Int) = split("=")[index]

    return rawArgs
        .split(";")
        .associate { s -> s.token(0) to s.token(1).split(",")  }
}

