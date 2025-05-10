package kr.toxicity.model.util

import kr.toxicity.model.BetterModelPluginImpl
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.util.LogUtil
import kr.toxicity.model.api.util.PackUtil
import kr.toxicity.model.manager.ConfigManagerImpl

val PLUGIN
    get() = BetterModel.inst() as BetterModelPluginImpl
val DATA_FOLDER
    get() = PLUGIN.dataFolder

fun info(vararg message: String) = PLUGIN.logger().info(*message)
fun warn(vararg message: String) = PLUGIN.logger().warn(*message)
fun debugPack(lazyMessage: () -> String) {
    if (ConfigManagerImpl.debug().pack) info("[${Thread.currentThread().name}] ${lazyMessage()}")
}

fun Throwable.handleException(message: String) = LogUtil.handleException(message, this)

fun <T> Result<T>.handleFailure(lazyMessage: () -> String) = onFailure {
    it.handleException(lazyMessage())
}

fun String.toPackName() = PackUtil.toPackName(this)