/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.command

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.data.renderer.ModelRenderer
import org.incendo.cloud.context.CommandContext

fun command(
    name: String,
    description: String,
    vararg aliases: String,
    block: CommandBuilder.() -> Unit
) = CommandBuildContext(name, description, *aliases).run {
    root.block()
    build()
}

inline fun CommandContext<*>.limb(key: String, notFound: (String) -> ModelRenderer) = optional<String>(key).flatMap {
    BetterModel.limb(it)
}.orElse(null) ?: notFound(key)

inline fun CommandContext<*>.model(key: String, notFound: (String) -> ModelRenderer) = optional<String>(key).flatMap {
    BetterModel.model(it)
}.orElse(null) ?: notFound(key)

inline fun <T> CommandContext<*>.string(key: String, mapper: (String) -> T) = mapper(get(key))

fun <T> CommandContext<*>.nullableString(key: String, mapper: (String) -> T): T? = optional<String>(key).map { mapper(it) }.orElse(null)

inline fun <reified T : Any> CommandContext<*>.nullable(key: String): T? = optional<T>(key).orElse(null)
inline fun <reified T : Any> CommandContext<*>.nullable(key: String, ifNotFound: T): T = optional<T>(key).orElse(null) ?: ifNotFound
inline fun <reified T : Any> CommandContext<*>.nullable(key: String, ifNotFound: () -> T): T = optional<T>(key).orElse(null) ?: ifNotFound()