package kr.toxicity.model.util

import kr.toxicity.model.BetterModelPluginImpl
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.config.DebugConfig
import kr.toxicity.model.api.util.HttpUtil
import kr.toxicity.model.api.util.LogUtil
import kr.toxicity.model.api.util.PackUtil
import java.net.http.HttpClient

val PLUGIN
    get() = BetterModel.plugin() as BetterModelPluginImpl
val CONFIG
    get() = BetterModel.config()
val DATA_FOLDER
    get() = PLUGIN.dataFolder

fun info(vararg message: String) = PLUGIN.logger().info(*message)
fun warn(vararg message: String) = PLUGIN.logger().warn(*message)
inline fun debugPack(lazyMessage: () -> String) {
    if (CONFIG.debug().has(DebugConfig.DebugOption.PACK)) info("[${Thread.currentThread().name}] ${lazyMessage()}")
}

fun Throwable.handleException(message: String) = LogUtil.handleException(message, this)

inline fun <T> Result<T>.handleFailure(lazyMessage: () -> String) = onFailure {
    it.handleException(lazyMessage())
}

fun String.toPackName() = PackUtil.toPackName(this)

fun <T : Any> httpClient(block: HttpClient.() -> T): HttpUtil.Result<T> = HttpUtil.client {
    it.block()
}