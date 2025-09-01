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
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.extension
import kotlin.io.path.fileSize

object ModelManagerImpl : ModelManager, GlobalManager {

    private const val MODERN_MODEL_ITEM_NAME = "bm_models"

    private lateinit var itemModelNamespace: NamespacedKey
    private val generalModelMap = hashMapOf<String, ModelRenderer>()
    private val generalModelView = generalModelMap.toImmutableView()
    private val playerModelMap = hashMapOf<String, ModelRenderer>()
    private val playerModelView = playerModelMap.toImmutableView()

    private fun importModels(
        type: ModelRenderer.Type,
        pipeline: ReloadPipeline,
        zipper: PackZipper,
        dir: File
    ): List<ImportedModel> {
        val modelFileMap = ConcurrentHashMap<String, Pair<Path, ModelBlueprint>>()
        val textures = zipper.assets().bettermodel().textures()
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
            val load = it.toFile().toTexturedModel() ?: return@forEachParallel warn("This model file has unsupported element type (e.g., mesh): $it")
            modelFileMap.compute(load.name) compute@ { _, v ->
                pipeline.progress()
                if (v != null) {
                    // A model with the same name already exists from a different file
                    warn(
                        "Duplicate $typeName model name '${load.name}'.",
                        "Duplicated file: $it",
                        "And: ${v.first}"
                    )
                    return@compute v
                }
                debugPack {
                    "$typeName model file successfully loaded: $it"
                }
                it to load.apply {
                    buildImage().forEach { image ->
                        textures.add("${image.name}.png", image.estimatedSize()) {
                            image.image.toByteArray()
                        }
                        image.mcmeta()?.let { meta ->
                            textures.add("${image.name}.png.mcmeta", -1) {
                                meta.toByteArray()
                            }
                        }
                    }
                }
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
                importModels(ModelRenderer.Type.GENERAL, pipeline, zipper, DATA_FOLDER.getOrCreateDirectory("models") { folder ->
                    if (PLUGIN.version().useModernResource()) folder.addResource("demon_knight.bbmodel")
                })
            )
            if (CONFIG.module().playerAnimation) it.addModelTo(
                playerModelMap,
                importModels(ModelRenderer.Type.PLAYER, pipeline, zipper, DATA_FOLDER.getOrCreateDirectory("players") { folder ->
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
        private val legacyModel = ModelBuilder(zipper.legacy().bettermodel().models().resolve("item"))
        private val modernModel = ModelBuilder(zipper.modern().bettermodel().models().resolve("modern_item"))
        private var estimatedSize = 0L

        override fun close() {
            val itemName = CONFIG.item().name.lowercase()
            modernModel.ifNotEmpty {
                jsonObjectOf("model" to jsonObjectOf(
                    "type" to "range_dispatch",
                    "property" to "custom_model_data",
                    "fallback" to jsonObjectOf(
                        "type" to "minecraft:model",
                        "model" to "minecraft:item/$itemName"
                    ),
                    "entries" to it
                ))
            }?.run {
                zipper.modern().bettermodel().items().add("$MODERN_MODEL_ITEM_NAME.json", estimatedSize) {
                    toByteArray()
                }
            }
            legacyModel.ifNotEmpty {
                jsonObjectOf(
                    "parent" to "minecraft:item/generated",
                    "textures" to jsonObjectOf("layer0" to "minecraft:item/$itemName"),
                    "overrides" to it
                )
            }?.run {
                val byteArray = toByteArray()
                val estimatedSize = byteArray.size.toLong()
                legacyModel.pack.add("$MODERN_MODEL_ITEM_NAME.json", estimatedSize) { byteArray }
                zipper.legacy().minecraft().models().resolve("item").add("$itemName.json", estimatedSize) { byteArray }
            }
        }

        fun addModelTo(
            targetMap: MutableMap<String, ModelRenderer>,
            model: List<ImportedModel>
        ) {
            if (model.isEmpty()) return
            val maxScale = model.maxOf {
                it.blueprint.scale()
            }
            model.forEach { importedModel ->
                val size = importedModel.jsonSize
                val load = importedModel.blueprint
                targetMap[load.name] = load.toRenderer(importedModel.type, maxScale) render@ { group ->
                    if (!load.hasTexture()) return@render null
                    var success = false
                    //Modern
                    group.buildModernJson(maxScale, load)?.let { modernBlueprint ->
                        modernModel.entries += jsonObjectOf(
                            "threshold" to indexer,
                            "model" to modernBlueprint.toModernJson()
                        )
                        modernBlueprint.forEach { json ->
                            modernModel.pack.add("${json.name}.json", size / modernBlueprint.size) {
                                json.element.toByteArray()
                            }
                        }
                        success = true
                    }
                    //Legacy
                    group.buildLegacyJson(PLUGIN.version().useModernResource(), maxScale, load)?.let { blueprint ->
                        legacyModel.entries += jsonObjectOf(
                            "predicate" to jsonObjectOf("custom_model_data" to indexer),
                            "model" to "${CONFIG.namespace()}:item/${blueprint.name}"
                        )
                        legacyModel.pack.add("${blueprint.name}.json", size) {
                            blueprint.element.toByteArray()
                        }
                        success = true
                    }
                    if (success) indexer++ else null
                }.apply {
                    debugPack {
                        "This model was successfully imported: ${load.name}"
                    }
                    ModelImportedEvent(this).call()
                }
                estimatedSize += size
            }
        }

        data class ModelBuilder(
            val pack: PackBuilder
        ) {
            val entries = jsonArrayOf()

            inline fun <T> ifNotEmpty(block: (JsonArray) -> T): T? {
                return if (!entries.isEmpty) block(entries) else null
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

    private fun ModelBlueprint.toRenderer(type: ModelRenderer.Type, scale: Float, consumer: (BlueprintGroup) -> Int?): ModelRenderer {
        fun BlueprintGroup.parse(): RendererGroup {
            return RendererGroup(
                name,
                scale,
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

    override fun start() {
        BoneTags.entries.forEach {
            BoneTagRegistry.addTag(it)
        }
    }

    override fun reload(pipeline: ReloadPipeline, zipper: PackZipper) {
        itemModelNamespace = NamespacedKey(CONFIG.namespace(), MODERN_MODEL_ITEM_NAME)
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