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
import kr.toxicity.model.util.getOrCreateDirectory
import kr.toxicity.model.util.handleFailure
import kr.toxicity.model.util.subFiles
import kr.toxicity.model.util.toImage
import java.io.File

object ArmorManager : GlobalManager {

    var armor: ArmorModel = ArmorModel.EMPTY
        private set

    override fun reload(
        pipeline: ReloadPipeline,
        zipper: PackZipper
    ) {
        if (!PLUGIN.version().useModernResource() || !CONFIG.module().playerAnimation) {
            armor = ArmorModel.EMPTY
            return
        }
        val folder = DATA_FOLDER.getOrCreateDirectory("armor_assets") {
            PLUGIN.loadAssets(
                pipeline,
                "armor_assets"
            ) { path, stream ->
                File(it, path).apply {
                    parentFile.mkdirs()
                }.outputStream().buffered().use { buffer ->
                    stream.copyTo(buffer)
                }
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