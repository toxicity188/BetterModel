package kr.toxicity.model.util

import kr.toxicity.model.api.ModelRenderer

val PLUGIN
    get() = ModelRenderer.inst()
val DATA_FOLDER
    get() = PLUGIN.dataFolder

fun info(vararg message: String) {
    val logger = PLUGIN.logger
    synchronized(logger) {
        for (s in message) {
            logger.info(s)
        }
    }
}
fun warn(vararg message: String) {
    val logger = PLUGIN.logger
    synchronized(logger) {
        for (s in message) {
            logger.warning(s)
        }
    }
}