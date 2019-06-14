package net.onedaybeard.graftt.agent

import java.lang.instrument.Instrumentation

class GraftAgent  {
    companion object {
        @JvmStatic fun premain(agentArgs: String?, inst: Instrumentation) {
            inst.addTransformer(TransplantTransformer())
        }
    }
}