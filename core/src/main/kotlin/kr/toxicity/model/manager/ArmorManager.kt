/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.manager

import kr.toxicity.library.armormodel.ArmorImage
import kr.toxicity.library.armormodel.ArmorModel
import kr.toxicity.library.armormodel.ArmorNameMapper
import kr.toxicity.library.armormodel.ArmorPaletteImage
import kr.toxicity.model.api.pack.PackObfuscator
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.util.CONFIG
import kr.toxicity.model.util.DATA_FOLDER
import kr.toxicity.model.util.PLUGIN
import kr.toxicity.model.util.buildHttpRequest
import kr.toxicity.model.util.getOrCreateDirectory
import kr.toxicity.model.util.handleFailure
import kr.toxicity.model.util.httpClient
import kr.toxicity.model.util.info
import kr.toxicity.model.util.subFiles
import kr.toxicity.model.util.toComponent
import kr.toxicity.model.util.toImage
import kr.toxicity.model.util.toJson
import net.kyori.adventure.text.format.NamedTextColor
import java.io.File
import java.net.URI
import java.net.http.HttpResponse
import java.util.concurrent.CompletableFuture
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import kotlin.io.path.createTempFile

object ArmorManager : GlobalManager {

    private val ARMOR_PATH = "assets/minecraft/textures/entity/equipment/humanoid" to "assets/minecraft/textures/entity/equipment/humanoid_leggings"
    private val ARMOR_TRIM_PATH = "assets/minecraft/textures/trims/entity/humanoid" to "assets/minecraft/textures/trims/entity/humanoid_leggings"
    private const val ARMOR_PALETTE_PATH = "assets/minecraft/textures/trims/color_palettes"

    private val armors = setOf(
        "chainmail",
        "copper",
        "diamond",
        "gold",
        "iron",
        "leather",
        "netherite"
    )

    private val palettes = setOf(
        "amethyst",
        "copper",
        "copper_darker",
        "diamond",
        "diamond_darker",
        "emerald",
        "gold",
        "gold_darker",
        "iron",
        "iron_darker",
        "lapis",
        "netherite",
        "netherite_darker",
        "quartz",
        "redstone",
        "resin"
    )

    private val trims = setOf(
        "bolt",
        "coast",
        "dune",
        "eye",
        "flow",
        "host",
        "raiser",
        "rib",
        "sentry",
        "shaper",
        "silence",
        "snout",
        "spire",
        "tide",
        "vex",
        "ward",
        "wayfinder",
        "wild"
    )

    var armor: ArmorModel = ArmorModel.EMPTY
        private set

    private data class VersionManifest(
        val latest: ManifestLatest,
        val versions: List<ManifestVersion>
    )

    private data class ManifestLatest(
        val release: String
    )

    private data class ManifestVersion(
        val id: String,
        val url: String
    )

    private data class VersionHash(
        val downloads: Map<String, HashDownload>
    )

    private data class HashDownload(
        val url: String
    )

    private data class MinecraftClient(
        val version: String,
        val file: File
    )

    private fun downloadMinecraftClient(): CompletableFuture<MinecraftClient?> = httpClient {
        val cacheFolder = DATA_FOLDER.getOrCreateDirectory(".cache")
        sendAsync(
            buildHttpRequest {
                GET()
                uri(URI.create("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
            },
            HttpResponse.BodyHandlers.ofInputStream()
        ).thenComposeAsync { response1 ->
            val manifest = response1.toJson(VersionManifest::class.java).run {
                versions.associateBy { it.id }[latest.release]!!
            }
            val cache = File(cacheFolder, "${manifest.id}.jar")
            if (cache.exists() && cache.length() > 0) CompletableFuture.supplyAsync { MinecraftClient(manifest.id, cache) }
            else sendAsync(
                buildHttpRequest {
                    GET()
                    uri(URI.create(manifest.url))
                },
                HttpResponse.BodyHandlers.ofInputStream()
            ).thenComposeAsync { response2 ->
                val hash = response2.toJson(VersionHash::class.java)
                sendAsync(
                    buildHttpRequest {
                        GET()
                        uri(URI.create(hash.downloads["client"]!!.url))
                    },
                    HttpResponse.BodyHandlers.ofInputStream()
                ).thenComposeAsync { response3 ->
                    val temp = createTempFile(cache.parentFile.toPath(), manifest.id, ".tmp").toFile()
                    response3.body().use { input ->
                        temp.outputStream().buffered().use(input::copyTo)
                    }
                    temp.renameTo(cache)
                    CompletableFuture.supplyAsync { MinecraftClient(manifest.id, cache) }
                }
            }
        }
    }.orElse {
        CompletableFuture.completedFuture(null)
    }

    private class ArmorImageCache(
        val name: String,
        val armor: ByteArray,
        val leggings: ByteArray
    ) {
        val size: Long get() = armor.size.toLong() + leggings.size.toLong()

        fun write(target: File) {
            val file = File(target, name).apply { mkdirs() }
            File(file, "armor.png").outputStream().buffered().use { it.write(armor) }
            File(file, "leggings.png").outputStream().buffered().use { it.write(leggings) }
        }
    }

    private fun JarFile.loadArmorImage(name: String, pathPair: Pair<String, String>) = ArmorImageCache(
        name,
        loadImage(pathPair.first, name),
        loadImage(pathPair.second, name)
    )

    private fun JarFile.loadImage(path: String, name: String) = getInputStream(ZipEntry("$path/$name.png")).use { it.readAllBytes() }

    override fun reload(
        pipeline: ReloadPipeline,
        zipper: PackZipper
    ) {
        if (!PLUGIN.version().useModernResource() || !CONFIG.module().playerAnimation) {
            armor = ArmorModel.EMPTY
            return
        }
        val folder = DATA_FOLDER.getOrCreateDirectory("armors") {
            info("Downloading client jar...".toComponent())
            val armorsFile = File(it, "armors")
            val trimsFile = File(it, "armor_trims")
            val palettesFile = File(it, "palettes").apply { mkdirs() }
            runCatching {
                downloadMinecraftClient().join()?.let { client ->
                    JarFile(client.file).use { jar ->
                        pipeline.forEachParallel(
                            armors.map { name -> jar.loadArmorImage(name, ARMOR_PATH) },
                                ArmorImageCache::size
                        ) { image -> image.write(armorsFile) }
                        pipeline.forEachParallel(
                            trims.map { name -> jar.loadArmorImage(name, ARMOR_TRIM_PATH) },
                            ArmorImageCache::size
                        ) { image -> image.write(trimsFile) }
                        pipeline.forEachParallel(
                            palettes.map { name -> name to jar.loadImage(ARMOR_PALETTE_PATH, name) },
                            { image -> image.second.size.toLong() },
                        ) { image -> File(palettesFile, "${image.first}.png").writeBytes(image.second)}
                    }
                }
                info("Download success!".toComponent(NamedTextColor.LIGHT_PURPLE))
            }.handleFailure {
                "Unable to download default armor assets."
            }
        }
        val textures = PackObfuscator.order()
        val models = PackObfuscator.order()
        armor = ArmorModel.builder()
            .namespace(CONFIG.namespace())
            .streamLoader { path -> PLUGIN.getResource(path)!! }
            .armors(pipeline
                .mapParallel(File(folder, "armors").subFiles(), File::length) { it.toArmorImage() }
                .sortedBy { it.name }
            )
            .armorTrims(pipeline
                .mapParallel(File(folder, "armor_trims").subFiles(), File::length) { it.toArmorImage() }
                .sortedBy { it.name }
            )
            .palettes(pipeline
                .mapParallel(File(folder, "palettes").subFiles(), File::length) { it.toPaletteImage() }
                .sortedBy { it.name }
            )
            .nameMapper(ArmorNameMapper(
                { textures.obfuscate(it) },
                { models.obfuscate(it) }
            ))
            .flush(false)
            .build()
        armor.builders().forEach {
            zipper.modern().add(
                it.path(),
                256
            ) {
                it.get()
            }
        }
    }

    private fun File.toArmorImage() = runCatching {
        ArmorImage(
            nameWithoutExtension,
            File(this, "armor.png").toImage(),
            File(this, "leggings.png").toImage()
        )
    }.handleFailure {
        "Unable to load this armor image: $path"
    }.getOrNull()

    private fun File.toPaletteImage() = runCatching {
        ArmorPaletteImage(nameWithoutExtension, toImage())
    }.handleFailure {
        "Unable to load this palette image: $path"
    }.getOrNull()
}