/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.manager

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kr.toxicity.model.api.bone.BoneItemMapper
import kr.toxicity.model.api.bone.BoneName
import kr.toxicity.model.api.bone.BoneRenderContext
import kr.toxicity.model.api.bone.BoneTag
import kr.toxicity.model.api.bone.BoneTags
import kr.toxicity.model.api.data.Float3
import kr.toxicity.model.api.data.ModelAsset
import kr.toxicity.model.api.data.blueprint.BlueprintElement
import kr.toxicity.model.api.data.blueprint.BlueprintJson
import kr.toxicity.model.api.data.blueprint.ModelBlueprint
import kr.toxicity.model.api.data.renderer.ModelRenderer
import kr.toxicity.model.api.data.renderer.RendererGroup
import kr.toxicity.model.api.event.ModelAssetsEvent
import kr.toxicity.model.api.event.ModelImportedEvent
import kr.toxicity.model.api.manager.ModelManager
import kr.toxicity.model.api.nms.Profiled
import kr.toxicity.model.api.pack.PackBuilder
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.api.platform.PlatformItemTransform
import kr.toxicity.model.api.platform.PlatformNamespace
import kr.toxicity.model.api.util.TransformedItemStack
import kr.toxicity.model.util.*
import net.kyori.adventure.text.format.NamedTextColor.*
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.extension

object ModelManagerImpl : ModelManager, GlobalManager {

    private val generalModelMap = addressingMapOf<String, ModelRenderer>()
    private val generalModelView = generalModelMap.toImmutableView()
    private val playerModelMap = addressingMapOf<String, ModelRenderer>()
    private val playerModelView = playerModelMap.toImmutableView()
    private val modelExtensions = setOf("bbmodel", "ajmodel")

    private val customHeadItemMapper = object : BoneItemMapper {
        override fun apply(
            context: BoneRenderContext,
            transformedItemStack: TransformedItemStack
        ): TransformedItemStack = (context.source() as? Profiled)
            ?.armors()
            ?.helmetItem()
            ?: TransformedItemStack.empty()

        override fun transform(): PlatformItemTransform = PlatformItemTransform.HEAD
    }

    private val customHeadItemTag = object : BoneTag {
        override fun name(): String = INTERNAL_HEAD_ITEM_NAME
        override fun itemMapper(): BoneItemMapper = customHeadItemMapper
        override fun tags(): List<String> = emptyList()
    }

    private fun importModels(
        type: ModelRenderer.Type,
        pipeline: ReloadPipeline,
        dir: File
    ): Sequence<ImportedModel> {
        val targetAssets = ModelAssetsEvent(type, dir.fileTrees().use { stream ->
            stream.filter { it.extension.lowercase() in modelExtensions }
                .map(ModelAsset::of)
                .toMutableSet()
        }).apply { call() }
            .assets
            .ifEmpty { return emptySequence() }
            .toList()
        val modelFileMap = ConcurrentHashMap<String, Pair<ModelAsset, ModelBlueprint>>(targetAssets.size)
        val typeName = type.name.lowercase()
        pipeline.apply {
            status = "Importing $typeName models..."
            goal = targetAssets.size
        }.forEachParallel(targetAssets, ModelAsset::sizeAssume) {
            val index = pipeline.progress(it.name)
            val load = it.toTexturedModel() ?: return@forEachParallel
            modelFileMap.compute(load.name) { _, v ->
                if (v != null) {
                    // A model with the same name already exists from a different file
                    warn(
                        "Duplicate $typeName model name '${load.name}'.".toComponent(),
                        "Duplicated file: $it".toComponent(RED),
                        "And: ${v.first}".toComponent(RED)
                    )
                    if (v.first < it) return@compute v
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
            .map {
                ImportedModel(
                    it.first.sizeAssume - it.second.textures.sumOf { tex -> tex.image.size },
                    type,
                    it.second
                )
            }
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
                        folder.addResource("demon_knight.bbmodel")
                        folder.addResource("blue_wizard.bbmodel")
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
        val jsonSize: Long,
        val type: ModelRenderer.Type,
        val blueprint: ModelBlueprint
    )

    private class ModelPipeline(
        private val zipper: PackZipper
    ) : AutoCloseable {

        private val textures = zipper.assets().bettermodel().textures()

        private val modernModel = ModelBuilder(
            namespace = zipper.assets().obfuscate("model"),
            builder = { zipper.assets().bettermodel().models().resolve(namespace) },
            available = true,
            onBuild = { name, blueprints, json, size ->
                items.add(name, size) {
                    jsonObjectOf("model" to blueprints.toModernJson(namespace, json)).toByteArray()
                }
                blueprints.forEach { json ->
                    models.add(json.jsonName(), size / blueprints.size) {
                        json.buildJson().toByteArray()
                    }
                }
            }
        )

        override fun close() {
        }

        fun addModelTo(
            targetMap: MutableMap<String, ModelRenderer>,
            model: Sequence<ImportedModel>
        ) {
            model.forEach { addModelTo(targetMap, it) }
        }

        private fun addModelTo(
            targetMap: MutableMap<String, ModelRenderer>,
            importedModel: ImportedModel
        ) {
            val (size, type, blueprint) = importedModel
            val context = blueprint.context()
            targetMap[blueprint.name] = blueprint.toRenderer(type) render@ { group ->
                if (!context.canBeRendered()) return@render null
                modernModel.ifAvailable {
                    val json = group.buildModernJson(obfuscator, context)
                    val itemModel = group.buildMeshItemModel(context)
                    if (json != null || itemModel != null) {
                        group.jsonName(context)
                            .also { name -> build("$name.json", json ?: emptyList(), itemModel, if (json != null) size / json.size else 0) }
                            .let { "$namespace/$it" }
                    } else null
                }
            }.apply {
                debugPack {
                    componentOf(
                        "This model was successfully imported: ".toComponent(),
                        blueprint.name.toComponent(GREEN)
                    )
                }
                callEvent { ModelImportedEvent(blueprint, this) }
            }
            context.buildImage(textures.obfuscator()).forEach { image ->
                textures.add(image.pngName(), image.estimatedSize()) {
                    image.toByteArray()
                }
                image.mcmeta()?.let { meta ->
                    textures.add(image.mcmetaName(), -1) {
                        meta.toByteArray()
                    }
                }
            }
        }

        inner class ModelBuilder(
            val namespace: String,
            val builder: ModelBuilder.() -> PackBuilder,
            private val available: Boolean,
            private val onBuild: ModelBuilder.(String, List<BlueprintJson>, JsonObject?, Long) -> Unit,
        ) {
            val items = zipper.assets().bettermodel().items().resolve(namespace)
            val models = builder()
            val obfuscator = textures.obfuscator().withModels(models.obfuscator())

            inline fun <T> ifAvailable(block: ModelBuilder.() -> T): T? {
                return if (available) block() else null
            }

            fun build(name: String, list: List<BlueprintJson>, json: JsonObject?, size: Long) {
                onBuild(name, list, json, size)
            }
        }

        private fun List<BlueprintJson>.toModernJson(namespace: String, plus: JsonObject?) = if (size == 1) first().toModernJson(namespace) else jsonObjectOf(
            "type" to "composite",
            "models" to fold(JsonArray(size + if (plus != null) 1 else 0).apply {
                plus?.run(::add)
            }) { array, element -> array.apply { add(element.toModernJson(namespace)) } }
        )

        private fun BlueprintJson.toModernJson(namespace: String) = jsonObjectOf(
            "type" to "model",
            "model" to "${CONFIG.namespace()}:$namespace/$name",
            "tints" to jsonArrayOf(
                jsonObjectOf(
                    "type" to "custom_model_data",
                    "default" to 0xFFFFFF
                )
            )
        )

        private fun ModelBlueprint.toRenderer(type: ModelRenderer.Type, builder: (BlueprintElement.Group) -> String?): ModelRenderer {
            fun <T> Collection<BlueprintElement>.toBoneMap(mapper: (BlueprintElement.Bone) -> T) = filterIsInstance<BlueprintElement.Bone>().let { bone ->
                bone.associateTo(sequencedAddressingMapOf(bone.size)) { it.name() to mapper(it) }
            }.toImmutableView()

            fun BlueprintElement.Bone.customHeadItemRenderer(
                existingChildren: Map<BoneName, RendererGroup>
            ): RendererGroup {
                val baseName = "$INTERNAL_HEAD_ITEM_NAME:${uuid()}"
                var rawName = baseName
                var collision = 0
                while (existingChildren.keys.any { it.rawName() == rawName }) {
                    rawName = "$baseName:${++collision}"
                }
                val internalBone = BlueprintElement.Group(
                    UUID.nameUUIDFromBytes(rawName.toByteArray(StandardCharsets.UTF_8)),
                    BoneName(setOf(customHeadItemTag), rawName, rawName),
                    origin().invertXZ(),
                    Float3.ZERO,
                    emptyList(),
                    true
                )
                return RendererGroup(1.0F, null, internalBone, emptySequencedMap(), null)
            }

            fun BlueprintElement.Bone.parse(): RendererGroup {
                val childRenderers = if (this is BlueprintElement.Group) {
                    children.toBoneMap { it.parse() }
                } else emptySequencedMap()
                val renderedChildren = if (type == ModelRenderer.Type.PLAYER && name().tagged(BoneTags.PLAYER_HEAD)) {
                    sequencedAddressingMapOf<BoneName, RendererGroup>(childRenderers.size + 1).apply {
                        putAll(childRenderers)
                        customHeadItemRenderer(childRenderers).let { put(it.name(), it) }
                    }.toImmutableView()
                } else childRenderers
                if (this !is BlueprintElement.Group) return RendererGroup(1.0F, null, this, renderedChildren, null)
                return RendererGroup(
                    scale(),
                    if (name.toItemMapper() !== BoneItemMapper.EMPTY) null else builder(this)?.let { itemNamespace ->
                        CONFIG.item().get().itemModel(PlatformNamespace(CONFIG.namespace(), itemNamespace))
                    },
                    this,
                    renderedChildren,
                    hitBox(),
                )
            }
            return ModelRenderer(
                name,
                type,
                elements.toBoneMap { it.parse() },
                animations
            )
        }
    }

    override fun start() {
    }

    override fun reload(pipeline: ReloadPipeline, zipper: PackZipper) {
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

    private const val INTERNAL_HEAD_ITEM_NAME = "bettermodel:internal_player_head_item"
}
