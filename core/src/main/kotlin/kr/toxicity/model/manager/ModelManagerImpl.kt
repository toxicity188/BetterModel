package kr.toxicity.model.manager

import com.google.gson.JsonArray
import kr.toxicity.model.api.animation.AnimationModifier
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
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.extension
import kotlin.io.path.fileSize

object ModelManagerImpl : ModelManager, GlobalManager {

    private const val MODERN_MODEL_ITEM_NAME = "bm_models"

    private lateinit var itemModelNamespace: NamespacedKey
    private val generalModelMap = hashMapOf<String, ModelRenderer>()
    private val generalModelView = generalModelMap.toImmutableView()
    private val playerModelMap = hashMapOf<String, ModelRenderer>()
    private val playerModelView = playerModelMap.toImmutableView()


    override fun start() {
        BoneTags.entries.forEach {
            BoneTagRegistry.addTag(it)
        }
    }

    private fun importModels(
        pipeline: ReloadPipeline,
        zipper: PackZipper,
        dir: File
    ): List<ImportedModel> {
        val modelFileMap = ConcurrentHashMap<String, Pair<Path, ModelBlueprint>>()
        val textures = zipper.assets().bettermodel().textures()
        val targetFolder = dir.fileTreeList().use { stream ->
            stream.filter { it.extension == "bbmodel" }.toList()
        }
        pipeline.status = "Importing models..."
        pipeline goal targetFolder.size
        pipeline.forEachParallel(targetFolder, Path::fileSize) {
            val load = it.toFile().toTexturedModel() ?: return@forEachParallel warn("This model file has unsupported element type (e.g., mesh): $it")
            modelFileMap.compute(load.name) compute@ { _, v ->
                pipeline.progress()
                if (v != null) {
                    // A model with the same name already exists from a different file
                    warn(
                        "Duplicate model name '${load.name}'.",
                        "Duplicated file: $it",
                        "And: ${v.first}"
                    )
                    return@compute v
                }
                debugPack {
                    "Model file successfully loaded: $it"
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
            .map { ImportedModel(it.first.fileSize(), it.second) }
            .toList()
    }

    private fun addModelTo(
        targetMap: MutableMap<String, ModelRenderer>,
        pipeline: ModelPipeline,
        type: ModelRenderer.Type,
        model: List<ImportedModel>
    ) {
        val maxScale = model.maxOfOrNull {
            it.blueprint.scale()
        } ?: 1F
        model.forEach { importedModel ->
            val size = importedModel.jsonSize
            val load = importedModel.blueprint
            targetMap[load.name] = load.toRenderer(type, maxScale) render@ { blueprintGroup ->
                if (!load.hasTexture()) return@render null
                var success = false
                val index = pipeline.indexer.get()
                //Modern
                blueprintGroup.buildModernJson(maxScale, load)?.let { modernBlueprint ->
                    pipeline.modernModel.entries.add(jsonObjectOf(
                        "threshold" to index,
                        "model" to modernBlueprint.toModernJson()
                    ))
                    modernBlueprint.forEach { json ->
                        pipeline.modernModel.pack.add("${json.name}.json", size / modernBlueprint.size) {
                            json.element.toByteArray()
                        }
                    }
                    success = true
                }
                //Legacy
                blueprintGroup.buildLegacyJson(PLUGIN.version().useModernResource(), maxScale, load)?.let { blueprint ->
                    pipeline.legacyModel.entries.add(jsonObjectOf(
                        "predicate" to jsonObjectOf("custom_model_data" to index),
                        "model" to "${CONFIG.namespace()}:item/${blueprint.name}"
                    ))
                    pipeline.legacyModel.pack.add("${blueprint.name}.json", size) {
                        blueprint.element.toByteArray()
                    }
                    success = true
                }
                if (success) pipeline.indexer.andIncrement else null
            }.apply {
                debugPack {
                    "This model was successfully imported: ${load.name}"
                }
                ModelImportedEvent(this).call()
            }
        }
        pipeline.estimatedSize += model.sumOf { it.jsonSize }
    }

    private data class ImportedModel(
        val size: Long,
        val blueprint: ModelBlueprint
    ) {
        val jsonSize = size - blueprint.textures.sumOf {
            it.image.height * it.image.width * 4
        }
    }

    private class ModelPipeline(
        private val zipper: PackZipper
    ) : AutoCloseable {
        val indexer = AtomicInteger(1)
        val legacyModel = ModelBuilder(zipper.legacy().bettermodel().models().resolve("item"))
        val modernModel = ModelBuilder(zipper.modern().bettermodel().models().resolve("modern_item"))
        var estimatedSize = 0L

        override fun close() {
            val itemName = CONFIG.item().name.lowercase()
            jsonObjectOf("model" to jsonObjectOf(
                "type" to "range_dispatch",
                "property" to "custom_model_data",
                "fallback" to jsonObjectOf(
                    "type" to "minecraft:model",
                    "model" to "minecraft:item/$itemName"
                ),
                "entries" to modernModel.entries
            )).run {
                zipper.modern().bettermodel().items().add("$MODERN_MODEL_ITEM_NAME.json", estimatedSize) {
                    toByteArray()
                }
            }
            jsonObjectOf(
                "parent" to "minecraft:item/generated",
                "textures" to jsonObjectOf("layer0" to "minecraft:item/$itemName"),
                "overrides" to legacyModel.entries
            ).run {
                val byteArray = toByteArray()
                val estimatedSize = byteArray.size.toLong()
                legacyModel.pack.add("$MODERN_MODEL_ITEM_NAME.json", estimatedSize) { byteArray }
                zipper.legacy().minecraft().models().resolve("item").add("$itemName.json", estimatedSize) { byteArray }
            }
        }

        data class ModelBuilder(
            val pack: PackBuilder
        ) {
            val entries = jsonArrayOf()
        }
    }

    private fun loadModels(pipeline: ReloadPipeline, zipper: PackZipper) {
        ModelPipeline(zipper).use {
            if (CONFIG.module().model) addModelTo(
                generalModelMap,
                it,
                ModelRenderer.Type.GENERAL,
                importModels(pipeline, zipper, DATA_FOLDER.getOrCreateDirectory("models") { folder ->
                    if (PLUGIN.version().useModernResource()) folder.addResource("demon_knight.bbmodel")
                })
            )
            if (CONFIG.module().playerAnimation) addModelTo(
                playerModelMap,
                it,
                ModelRenderer.Type.PLAYER,
                importModels(pipeline, zipper, DATA_FOLDER.getOrCreateDirectory("players") { folder ->
                    folder.addResource("steve.bbmodel")
                })
            )
        }
    }

    override fun reload(pipeline: ReloadPipeline, zipper: PackZipper) {
        itemModelNamespace = NamespacedKey(CONFIG.namespace(), MODERN_MODEL_ITEM_NAME)
        generalModelMap.clear()
        playerModelMap.clear()
        loadModels(pipeline, zipper)
    }

    private fun List<BlueprintJson>.toModernJson() = if (size == 1) get(0).toModernJson() else jsonObjectOf(
        "type" to "minecraft:composite",
        "models" to map { it.toModernJson() }.fold(JsonArray(size)) { array, element -> array.apply { add(element) } }
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
            type,
            this,
            group.filterIsInstance<BlueprintGroup>()
                .associate { it.name to it.parse() },
            animations
        )
    }

    override fun model(name: String): ModelRenderer? = generalModelView[name]
    override fun models(): Collection<ModelRenderer> = generalModelView.values
    override fun modelKeys(): Set<String> = generalModelView.keys
    override fun limb(name: String): ModelRenderer? = playerModelView[name]
    override fun limbs(): Collection<ModelRenderer> = playerModelView.values
    override fun limbKeys(): Set<String> = playerModelView.keys

    override fun animate(player: Player, model: String, animation: String, modifier: AnimationModifier): Boolean {
        return playerModelView[model]?.let {
            val create = it.getOrCreate(player)
            val success = create.animate(animation, modifier) {
                create.close()
            }
            if (!success) create.close()
            success
        } == true
    }
}