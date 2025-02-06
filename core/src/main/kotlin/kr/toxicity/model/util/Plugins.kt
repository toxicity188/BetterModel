package kr.toxicity.model.util

import kr.toxicity.model.BetterModelPluginImpl
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.util.LogUtil

val PLUGIN
    get() = BetterModel.inst() as BetterModelPluginImpl
val DATA_FOLDER
    get() = PLUGIN.dataFolder

fun info(vararg message: String) = PLUGIN.logger().info(*message)
fun warn(vararg message: String) = PLUGIN.logger().warn(*message)

fun Throwable.handleException(message: String) = LogUtil.handleException(message, this)