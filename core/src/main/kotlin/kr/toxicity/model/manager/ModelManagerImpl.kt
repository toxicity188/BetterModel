/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.manager

import com.google.gson.JsonArray
import kr.toxicity.model.api.bone.BoneItemMapper
import kr.toxicity.model.api.bone.BoneTagRegistry
import kr.toxicity.model.api.bone.BoneTags
import kr.toxicity.model.api.data.blueprint.BlueprintChildren.BlueprintGroup
import kr.toxicity.model.api.data.blueprint.BlueprintJson
import kr.toxicity.model.api.data.blueprint.ModelBlueprint
import kr.toxicity.model.api.data.renderer.ModelRenderer
import kr.toxicity.model.api.data.renderer.RendererGroup
import kr.toxicity.model.api.event.ModelImportedEvent
import kr.toxicity.model.api.manager.ModelManager
import kr.toxicity.model.api.pack.PackBuilder
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.util.*
import net.kyori.adventure.text.format.NamedTextColor.*
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.extension
import kotlin.io.path.fileSize

object ModelManagerImpl : ModelManager, GlobalManager {

    private lateinit var itemModelNamespace: NamespacedKey
    private val generalModelMap = hashMapOf<String, ModelRenderer>()
    private val generalModelView = generalModelMap.toImmutableView()
    private val playerModelMap = hashMapOf<String, ModelRenderer>()
    private val playerModelView = playerModelMap.toImmutableView()

    private fun importModels(
        type: ModelRenderer.Type,
        pipeline: ReloadPipeline,
        dir: File
    ): List<ImportedModel> {
        val modelFileMap = ConcurrentHashMap<String, Pair<Path, ModelBlueprint>>()
        val targetFolder = dir.fileTreeList().use { stream ->
            stream.filter { it.extension == "bbmodel" }.toList()
        }.ifEmpty {
            return emptyList()
        }
        val typeName = type.name.lowercase()
        pipeline.apply {
            status = "Importing $typeName models..."
            goal = targetFolder.size
        }.forEachParallel(targetFolder, Path::fileSize) {
            val load = it.toFile().toTexturedModel() ?: return@forEachParallel warn(componentOf(
                "This model file has unsupported element type (e.g., mesh): ".toComponent(),
                it.toString().toComponent(RED)
            ))
            modelFileMap.compute(load.name) compute@ { _, v ->
                val index = pipeline.progress()
                if (v != null) {
                    // A model with the same name already exists from a different file
                    warn(
                        "Duplicate $typeName model name '${load.name}'.".toComponent(),
                        "Duplicated file: $it".toComponent(RED),
                        "And: ${v.first}".toComponent(RED)
                    )
                    return@compute v
                }
                debugPack {
                    componentOf(
                        "$typeName model file successfully loaded: ".toComponent(),
                        it.toString().toComponent(GREEN),
                        " ($index/${pipeline.goal})".toComponent(DARK_GRAY)
                    )
                }
                it to load
            }
        }
        return modelFileMap.values
            .asSequence()
            .sortedBy { it.first }
            .map { ImportedModel(it.first.fileSize(), type,it.second) }
            .toList()
    }

    private fun loadModels(pipeline: ReloadPipeline, zipper: PackZipper) {
        ModelPipeline(zipper).use {
            if (CONFIG.module().model) it.addModelTo(
                generalModelMap,
                importModels(ModelRenderer.Type.GENERAL, pipeline, DATA_FOLDER.getOrCreateDirectory("models") { folder ->
                    File(DATA_FOLDER.parent, "ModelEngine/blueprints")
                        .takeIf(File::isDirectory)
                        ?.run {
                            copyRecursively(folder, overwrite = true)
                            info("ModelEngine's models are successfully migrated.".toComponent(GREEN))
                        } ?: run {
                        if (PLUGIN.version().useModernResource()) folder.addResource("demon_knight.bbmodel")
                    }
                })
            )
            if (CONFIG.module().playerAnimation) it.addModelTo(
                playerModelMap,
                importModels(ModelRenderer.Type.PLAYER, pipeline, DATA_FOLDER.getOrCreateDirectory("players") { folder ->
                    folder.addResource("steve.bbmodel")
                })
            )
        }
    }

    private data class ImportedModel(
        val size: Long,
        val type: ModelRenderer.Type,
        val blueprint: ModelBlueprint
    ) {
        val jsonSize = size - blueprint.textures.sumOf {
            it.image.height * it.image.width * 4
        }
    }

    private class ModelPipeline(
        private val zipper: PackZipper
    ) : AutoCloseable {

        private var indexer = 1
        private var estimatedSize = 0L

        private val textures = zipper.assets().bettermodel().textures()
        private val legacyModel = ModelBuilder(
            pack = zipper.legacy().bettermodel().models().resolve("item"),
            available = CONFIG.pack().generateLegacyModel,
            onBuild = { blueprints, size ->
                val blueprint = blueprints.first()
                entries += jsonObjectOf(
                    "predicate" to jsonObjectOf("custom_model_data" to indexer),
                    "model" to "${CONFIG.namespace()}:item/${blueprint.name}"
                )
                pack.add("${blueprint.name}.json", size) {
                    blueprint.element.toByteArray()
                }
            },
            onClose = {
                val itemName = CONFIG.item().name.lowercase()
                jsonObjectOf(
                    "parent" to "minecraft:item/generated",
                    "textures" to jsonObjectOf("layer0" to "minecraft:item/$itemName"),
                    "overrides" to entries
                ).run {
                    val byteArray = toByteArray()
                    val estimatedSize = byteArray.size.toLong()
                    pack.add("${CONFIG.itemNamespace()}.json", estimatedSize) { byteArray }
                    zipper.legacy().minecraft().models().resolve("item").add("$itemName.json", estimatedSize) { byteArray }
                }
            }
        )

        private val modernModel = ModelBuilder(
            pack = zipper.modern().bettermodel().models().resolve("modern_item"),
            available = CONFIG.pack().generateModernModel,
            onBuild = { blueprints, size ->
                entries += jsonObjectOf(
                    "threshold" to indexer,
                    "model" to blueprints.toModernJson()
                )
                blueprints.forEach { json ->
                    pack.add("${json.name}.json", size / blueprints.size) {
                        json.element.toByteArray()
                    }
                }
            },
            onClose = {
                jsonObjectOf("model" to jsonObjectOf(
                    "type" to "range_dispatch",
                    "property" to "custom_model_data",
                    "fallback" to jsonObjectOf(
                        "type" to "minecraft:model",
                        "model" to "minecraft:item/${CONFIG.item().name.lowercase()}"
                    ),
                    "entries" to entries
                )).run {
                    zipper.modern().bettermodel().items().add("${CONFIG.itemNamespace()}.json", estimatedSize) {
                        toByteArray()
                    }
                }
            }
        )

        override fun close() {
            modernModel.close()
            legacyModel.close()
        }

        fun addModelTo(
            targetMap: MutableMap<String, ModelRenderer>,
            model: List<ImportedModel>
        ) {
            if (model.isEmpty()) return
            model.forEach { importedModel ->
                val size = importedModel.jsonSize
                val load = importedModel.blueprint
                val hasTexture = load.hasTexture()
                targetMap[load.name] = load.toRenderer(importedModel.type) render@ { group ->
                    if (!hasTexture) return@render null
                    var success = false
                    //Modern
                    modernModel.ifAvailable {
                        group.buildModernJson(textures.obfuscator().withModels(pack.obfuscator()), load)
                    }?.let {
                        modernModel.build(it, size)
                        success = true
                    }
                    //Legacy
                    legacyModel.ifAvailable {
                        group.buildLegacyJson(
                            PLUGIN.version().useModernResource(),
                            textures.obfuscator().withModels(pack.obfuscator()),
                            load
                        )
                    }?.let {
                        legacyModel.build(listOf(it), size)
                        success = true
                    }
                    if (success) indexer++ else null
                }.apply {
                    debugPack {
                        componentOf(
                            "This model was successfully imported: ".toComponent(),
                            load.name.toComponent(GREEN)
                        )
                    }
                    ModelImportedEvent(this).call()
                }
                if (hasTexture) load.buildImage(textures.obfuscator()).forEach { image ->
                    textures.add("${image.name}.png", image.estimatedSize()) {
                        image.image.toByteArray()
                    }
                    image.mcmeta()?.let { meta ->
                        textures.add("${image.name}.png.mcmeta", -1) {
                            meta.toByteArray()
                        }
                    }
                }
                estimatedSize += size
            }
        }

        data class ModelBuilder(
            val pack: PackBuilder,
            private val available: Boolean,
            private val onBuild: ModelBuilder.(List<BlueprintJson>, Long) -> Unit,
            private val onClose: ModelBuilder.() -> Unit
        ) : AutoCloseable {
            val entries = jsonArrayOf()

            inline fun <T> ifAvailable(block: ModelBuilder.() -> T): T? {
                return if (available) block() else null
            }

            fun build(list: List<BlueprintJson>, size: Long) {
                onBuild(list, size)
            }

            override fun close() {
                ifAvailable {
                    if (!entries.isEmpty) onClose()
                }
            }
        }

        private fun List<BlueprintJson>.toModernJson() = if (size == 1) get(0).toModernJson() else jsonObjectOf(
            "type" to "minecraft:composite",
            "models" to fold(JsonArray(size)) { array, element -> array.apply { add(element.toModernJson()) } }
        )

        private fun BlueprintJson.toModernJson() = jsonObjectOf(
            "type" to "minecraft:model",
            "model" to "${CONFIG.namespace()}:modern_item/${name}",
            "tints" to jsonArrayOf(
                jsonObjectOf(
                    "type" to "minecraft:custom_model_data",
                    "default" to 0xFFFFFF
                )
            )
        )

        private fun ModelBlueprint.toRenderer(type: ModelRenderer.Type, consumer: (BlueprintGroup) -> Int?): ModelRenderer {
            fun BlueprintGroup.parse(): RendererGroup {
                return RendererGroup(
                    name,
                    scale(),
                    if (name.toItemMapper() !== BoneItemMapper.EMPTY) null else consumer(this)?.let { i ->
                        ItemStack(CONFIG.item()).apply {
                            itemMeta = itemMeta.apply {
                                @Suppress("DEPRECATION") //To support legacy server :(
                                setCustomModelData(i)
                                if (PLUGIN.version().useItemModelName()) itemModel = itemModelNamespace
                            }
                        }
                    },
                    this,
                    children.filterIsInstance<BlueprintGroup>()
                        .associate { it.name to it.parse() },
                    hitBox(),
                )
            }
            return ModelRenderer(
                this,
                type,
                group.filterIsInstance<BlueprintGroup>()
                    .associate { it.name to it.parse() },
                animations
            )
        }
    }

    override fun start() {
        BoneTags.entries.forEach {
            BoneTagRegistry.addTag(it)
        }
    }

    override fun reload(pipeline: ReloadPipeline, zipper: PackZipper) {
        itemModelNamespace = NamespacedKey(CONFIG.namespace(), CONFIG.itemNamespace())
        generalModelMap.clear()
        playerModelMap.clear()
        loadModels(pipeline, zipper)
    }

    override fun model(name: String): ModelRenderer? = generalModelView[name]
    override fun models(): Collection<ModelRenderer> = generalModelView.values
    override fun modelKeys(): Set<String> = generalModelView.keys
    override fun limb(name: String): ModelRenderer? = playerModelView[name]
    override fun limbs(): Collection<ModelRenderer> = playerModelView.values
    override fun limbKeys(): Set<String> = playerModelView.keys
}