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
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.util.*
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.extension

object ModelManagerImpl : ModelManager, GlobalManager {

    private const val MODERN_MODEL_ITEM_NAME = "bm_models"

    private lateinit var itemModelNamespace: NamespacedKey
    private val renderMap = hashMapOf<String, ModelRenderer>()
    private val rendererView = renderMap.toImmutableView()


    override fun start() {
        BoneTags.entries.forEach {
            BoneTagRegistry.addTag(it)
        }
    }

    private fun importModels(pipeline: ReloadPipeline, zipper: PackZipper): List<ModelBlueprint> {
        val modelFileMap = ConcurrentHashMap<String, Pair<File, ModelBlueprint>>()
        val textures = zipper.assets().bettermodel().textures()
        val targetFolder = DATA_FOLDER.getOrCreateDirectory("models") { folder ->
            if (PLUGIN.version().useModernResource()) folder.addResource("demon_knight.bbmodel")
        }.fileTreeList()
            .filter { it.extension == "bbmodel" }
            .map { it.toFile() }
            .toList()
        pipeline.status = "Importing models..."
        pipeline goal targetFolder.size
        pipeline.forEachParallel(targetFolder,File::length) {
            val load = it.toTexturedModel() ?: return@forEachParallel warn("This model file has unsupported element type (e.g., mesh): ${it.path}")
            modelFileMap.compute(load.name) compute@ { _, v ->
                pipeline.progress()
                if (v != null) {
                    // A model with the same name already exists from a different file
                    warn(
                        "Duplicate model name '${load.name}'.",
                        "Duplicated file: ${it.path}",
                        "And: ${v.first.path}"
                    )
                    return@compute v
                }
                debugPack {
                    "Model file successfully loaded: ${it.path}"
                }
                it to load.apply {
                    buildImage().forEach { image ->
                        textures.add("${image.name}.png", image.estimatedSize()) {
                            image.image.toByteArray()
                        }
                        image.mcmeta()?.let { meta ->
                            textures.add("${image.name}.png.mcmeta", meta.estimatedSize()) {
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
            .map { it.second }
            .toList()
    }

    private fun loadModels(pipeline: ReloadPipeline, zipper: PackZipper) {
        val legacyModel = zipper.legacy().bettermodel().models().resolve("item")
        val modernModel = zipper.modern().bettermodel().models().resolve("modern_item")
        val itemName = CONFIG.item().name.lowercase()
        val model = importModels(pipeline, zipper)

        val maxScale = model.maxOfOrNull {
            it.scale()
        } ?: 1F
        var index = 1

        val legacyEntries = jsonArrayOf()
        val modernEntries = jsonArrayOf()

        model.forEach { load ->
            renderMap[load.name] = load.toRenderer(maxScale) render@ { blueprintGroup ->
                var success = false
                //Modern
                blueprintGroup.buildModernJson(maxScale, load)?.let { modernBlueprint ->
                    modernEntries.add(jsonObjectOf(
                        "threshold" to index,
                        "model" to modernBlueprint.toModernJson()
                    ))
                    modernBlueprint.forEach { json ->
                        modernModel.add("${json.name}.json", json.element.estimatedSize()) {
                            json.element.toByteArray()
                        }
                    }
                    success = true
                }
                //Legacy
                blueprintGroup.buildLegacyJson(PLUGIN.version().useModernResource(), maxScale, load)?.let { blueprint ->
                    legacyEntries.add(jsonObjectOf(
                        "predicate" to jsonObjectOf("custom_model_data" to index),
                        "model" to "${CONFIG.namespace()}:item/${blueprint.name}"
                    ))
                    legacyModel.add("${blueprint.name}.json", blueprint.element.estimatedSize()) {
                        blueprint.element.toByteArray()
                    }
                    success = true
                }
                if (success) index++ else null
            }.apply {
                debugPack {
                    "This model was successfully imported: ${load.name}"
                }
                ModelImportedEvent(this).call()
            }
        }
        jsonObjectOf("model" to jsonObjectOf(
            "type" to "range_dispatch",
            "property" to "custom_model_data",
            "fallback" to jsonObjectOf(
                "type" to "minecraft:model",
                "model" to "minecraft:item/$itemName"
            ),
            "entries" to modernEntries
        )).run {
            zipper.modern().bettermodel().items().add("$MODERN_MODEL_ITEM_NAME.json", estimatedSize()) {
                toByteArray()
            }
        }
        jsonObjectOf(
            "parent" to "minecraft:item/generated",
            "textures" to jsonObjectOf("layer0" to "minecraft:item/$itemName"),
            "overrides" to legacyEntries
        ).run {
            val byteArray = toByteArray()
            legacyModel.add("$MODERN_MODEL_ITEM_NAME.json", byteArray.size.toLong()) { byteArray }
            zipper.legacy().minecraft().models().resolve("item").add("$itemName.json", byteArray.size.toLong()) { byteArray }
        }
    }

    override fun reload(pipeline: ReloadPipeline, zipper: PackZipper) {
        itemModelNamespace = NamespacedKey(CONFIG.namespace(), MODERN_MODEL_ITEM_NAME)
        renderMap.clear()
        if (CONFIG.module().model()) loadModels(pipeline, zipper)
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

    private fun ModelBlueprint.toRenderer(scale: Float, consumer: (BlueprintGroup) -> Int?): ModelRenderer {
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
            group.filterIsInstance<BlueprintGroup>()
                .associate { it.name to it.parse() },
            animations
        )
    }

    override fun renderer(name: String): ModelRenderer? = rendererView[name]
    override fun renderers(): Collection<ModelRenderer> = rendererView.values
    override fun keys(): Set<String> = rendererView.keys
}