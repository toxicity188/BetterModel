package kr.toxicity.model.player

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.PropertyMap
import com.mojang.util.UUIDTypeAdapter
import kr.toxicity.model.api.player.PlayerSkinProvider
import kr.toxicity.model.util.handleException
import kr.toxicity.model.util.httpClient
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

    override fun provide(profile: GameProfile): CompletableFuture<GameProfile> {
        return httpClient {
            sendAsync(HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/lookup/name/${profile.name}"))
                .build(), HttpResponse.BodyHandlers.ofInputStream()
            ).thenCompose {
                val uuid = it.body()
                    .use {
                        JsonParser.parseReader(it.bufferedReader())
                    }
                    .asJsonObject
                    .getAsJsonPrimitive("id")
                    .asString
                sendAsync(HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/$uuid"))
                    .build(), HttpResponse.BodyHandlers.ofInputStream())
            }.thenApply {
                it.body().use { stream ->
                    serializer.fromJson(stream.bufferedReader(), Profile::class.java).let { p ->
                        GameProfile(
                            p.id,
                            p.name
                        ).apply {
                            properties.putAll(p.properties)
                        }
                    }
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