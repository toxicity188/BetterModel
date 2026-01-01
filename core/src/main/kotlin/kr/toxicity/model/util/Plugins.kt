/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.util

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.gson.GsonBuilder
import kr.toxicity.model.BetterModelPluginImpl
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.config.DebugConfig
import kr.toxicity.model.api.util.HttpUtil
import kr.toxicity.model.api.util.LogUtil
import kr.toxicity.model.api.util.PackUtil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.io.InputStream
import java.io.InputStreamReader
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

val PLUGIN
    get() = BetterModel.plugin() as BetterModelPluginImpl
val CONFIG
    get() = BetterModel.config()
val DATA_FOLDER
    get() = PLUGIN.dataFolder

private val LATEST_VERSION_CACHE = Caffeine.newBuilder()
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build<Any, HttpUtil.LatestVersion> { HttpUtil.versionList() }

private val GSON = GsonBuilder().disableHtmlEscaping().create()

val LATEST_VERSION: HttpUtil.LatestVersion get() = LATEST_VERSION_CACHE.get(Unit)

fun info(vararg message: Component) = PLUGIN.logger().info(*message)
fun warn(vararg message: Component) = PLUGIN.logger().warn(*message)
inline fun debugPack(lazyMessage: () -> Component) {
    if (CONFIG.debug().has(DebugConfig.DebugOption.PACK)) info(componentOf(
        "[${Thread.currentThread().name}] ".toComponent(NamedTextColor.YELLOW),
        lazyMessage()
    ))
}

fun Throwable.handleException(message: String) = LogUtil.handleException(message, this)

inline fun <T> Result<T>.handleFailure(lazyMessage: () -> String) = onFailure {
    it.handleException(lazyMessage())
}

fun String.toPackName() = PackUtil.toPackName(this)

fun <T : Any> httpClient(block: HttpClient.() -> T): HttpUtil.Result<T> = HttpUtil.client {
    it.block()
}

fun buildHttpRequest(builder: HttpRequest.Builder.() -> Unit): HttpRequest = HttpRequest.newBuilder().apply(builder).build()


fun <T> HttpResponse<InputStream>.toJson(clazz: Class<T>): T = body().use {
    InputStreamReader(it, StandardCharsets.UTF_8).use { reader -> GSON.fromJson(reader, clazz) }
}