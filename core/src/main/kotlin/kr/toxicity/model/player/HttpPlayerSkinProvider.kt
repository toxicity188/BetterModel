/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.player

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.mojang.authlib.properties.PropertyMap
import com.mojang.util.UUIDTypeAdapter
import kr.toxicity.model.api.player.PlayerSkinProvider
import kr.toxicity.model.api.skin.SkinProfile
import kr.toxicity.model.util.handleException
import kr.toxicity.model.util.httpClient
import java.io.Reader
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import java.util.concurrent.CompletableFuture

class HttpPlayerSkinProvider : PlayerSkinProvider {

    private data class Profile(
        val id: UUID,
        val name: String,
        val properties: PropertyMap
    )

    private val serializer = GsonBuilder()
        .registerTypeAdapter(UUID::class.java, UUIDTypeAdapter())
        .registerTypeAdapter(PropertyMap::class.java, PropertyMap.Serializer())
        .create()

    private fun read(reader: Reader) = serializer.fromJson(reader, Profile::class.java)

    override fun provide(profile: SkinProfile): CompletableFuture<SkinProfile> {
        return httpClient {
            sendAsync(HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/lookup/name/${profile.name}"))
                .build(), HttpResponse.BodyHandlers.ofInputStream()
            ).thenCompose {
                val uuid = it.body().use { body ->
                    body.bufferedReader().use(JsonParser::parseReader)
                }.asJsonObject
                    .getAsJsonPrimitive("id")
                    .asString
                sendAsync(HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/$uuid"))
                    .build(), HttpResponse.BodyHandlers.ofInputStream())
            }.thenApply {
                it.body().use { body ->
                    body.bufferedReader().use(::read)
                }.let { p ->
                    SkinProfile(p.id, p.name, p.properties["textures"])
                }
            }.exceptionally {
                it.handleException("Unable to get ${profile.name}'s skin data.")
                null
            }
        }.orElse {
            it.handleException("Unable to get ${profile.name}'s user data.")
            CompletableFuture.completedFuture(null)
        }
    }
}