/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.profile

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.mojang.authlib.properties.PropertyMap
import com.mojang.util.UUIDTypeAdapter
import kr.toxicity.model.api.profile.ModelProfile
import kr.toxicity.model.api.profile.ModelProfileInfo
import kr.toxicity.model.api.profile.ModelProfileSkin
import kr.toxicity.model.api.profile.ModelProfileSupplier
import kr.toxicity.model.manager.ProfileManagerImpl
import kr.toxicity.model.util.buildHttpRequest
import kr.toxicity.model.util.handleException
import kr.toxicity.model.util.httpClient
import java.io.Reader
import java.net.URI
import java.net.http.HttpResponse
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
class HttpModelProfileSupplier : ModelProfileSupplier {

    private val profileCache = Caffeine.newBuilder()
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .build<ModelProfileInfo, ModelProfile>()

    private val serializer = GsonBuilder()
        .registerTypeAdapter(UUID::class.java, UUIDTypeAdapter())
        .registerTypeAdapter(PropertyMap::class.java, PropertyMap.Serializer())
        .create()

    private data class Profile(
        val id: UUID,
        val name: String,
        val properties: PropertyMap
    )

    private fun read(reader: Reader) = serializer.fromJson(reader, Profile::class.java)

    override fun supply(info: ModelProfileInfo): ModelProfile.Uncompleted {
        return object : ModelProfile.Uncompleted {
            override fun info(): ModelProfileInfo = info

            override fun complete(): CompletableFuture<ModelProfile> {
                return profileCache.getIfPresent(info)?.let { CompletableFuture.completedFuture(it) } ?: httpClient {
                    (info.name?.let {
                        sendAsync(
                            buildHttpRequest {
                                GET()
                                uri(URI.create("https://api.minecraftservices.com/minecraft/profile/lookup/name/${it}"))
                            },
                            HttpResponse.BodyHandlers.ofInputStream()
                        ).thenApply { body ->
                            body.body().use { body ->
                                body.reader().use(JsonParser::parseReader)
                            }.asJsonObject
                                .getAsJsonPrimitive("id")
                                .asString
                        }
                    } ?: CompletableFuture.completedFuture(info.id.toString().replace("-", ""))).thenComposeAsync {
                        sendAsync(
                            buildHttpRequest {
                                GET()
                                uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/$it"))
                            },
                            HttpResponse.BodyHandlers.ofInputStream()
                        )
                    }.thenApplyAsync {
                        it.body().use { body ->
                            body.reader().use(::read)
                        }.let { profile ->
                            ModelProfile.of(
                                ModelProfileInfo(profile.id, profile.name),
                                profile.properties["textures"].firstOrNull()?.let { property ->
                                    ProfileManagerImpl.skin(property.value)
                                } ?: ModelProfileSkin.EMPTY
                            ).apply {
                                profileCache.put(info, this)
                            }
                        }
                    }.exceptionally {
                        it.handleException("Unable to get ${info.name}'s skin data.")
                        fallback()
                    }
                }.orElse {
                    it.handleException("Unable to get ${info.name}'s user data.")
                    CompletableFuture.completedFuture(fallback())
                }
            }
        }
    }
}
