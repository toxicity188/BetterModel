package kr.toxicity.model.manager

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kr.toxicity.model.api.bone.BoneItemMapper
import kr.toxicity.model.api.bone.BoneTagRegistry
import kr.toxicity.model.api.bone.BoneTags
import kr.toxicity.model.api.data.blueprint.BlueprintChildren.BlueprintGroup
import kr.toxicity.model.api.data.blueprint.BlueprintJson
import kr.toxicity.model.api.data.blueprint.ModelBlueprint
import kr.toxicity.model.api.data.renderer.ModelRenderer
import kr.toxicity.model.api.data.renderer.RendererGroup
import kr.toxicity.model.api.event.ModelImportedEvent
import kr.toxicity.model.api.manager.ConfigManager.PackType.FOLDER
import kr.toxicity.model.api.manager.ConfigManager.PackType.ZIP
import kr.toxicity.model.api.manager.ModelManager
import kr.toxicity.model.api.manager.ReloadInfo
import kr.toxicity.model.util.*
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import java.io.File
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ModelManagerImpl : ModelManager, GlobalManagerImpl {

    private const val MINECRAFT_ITEM_PATH = "assets/minecraft/models/item"
    private const val MODERN_MODEL_ITEM_NAME = "bm_models"

    private lateinit var itemModelNamespace: NamespacedKey
    private val renderMap = hashMapOf<String, ModelRenderer>()

    private class PackData(
        val path: String,
        val byteArray: () -> ByteArray
    ) : () -> ByteArray by byteArray {
        fun prefixed(prefix: String) = PackData("$prefix/$path", byteArray)
    }

    private class PackDataZipper {
        private val map = ConcurrentHashMap<String, PackData>()

        fun add(parent: String, name: String, byteArray: () -> ByteArray) {
            add(if (parent.isEmpty()) name else "$parent/$name", byteArray)
        }
        fun add(path: String, byteArray: () -> ByteArray) {
            if (map.put(path, PackData(path, byteArray)) != null) warn(
                "Name collision found: $path"
            )
        }

        fun add(prefix: String, other: PackDataZipper) {
            map += other.map.entries.associate {
                "$prefix/${it.key}" to it.value.prefixed(prefix)
            }
        }

        fun toFileTree(file: File) {
            val fileTree = Collections.synchronizedMap(sortedMapOf<String, File>(Comparator.reverseOrder()))
            val l = file.path.length + 1
            file.forEach { sub ->
                sub.forEachAll {
                    fileTree[it.path.substring(l)] = it
                }
            }
            fun String.toFile(): File {
                val replaced = replace('/', File.separatorChar)
                return fileTree.remove(replaced) ?: File(file, replaced).apply {
                    parentFile.mkdirs()
                }
            }
            map.values.toList().forEachAsync {
                it.path.toFile().outputStream().buffered().use { output ->
                    output.write(it.byteArray())
                }
                debugPack {
                    "This file was successfully zipped: ${it.path}"
                }
            }
            fileTree.values.forEach(File::delete)
            map.clear()
        }

        fun toZip(file: File) {
            ZipOutputStream(runCatching {
                MessageDigest.getInstance("SHA-1")
            }.map {
                DigestOutputStream(file.outputStream().buffered(), it)
            }.getOrElse {
                file.outputStream().buffered()
            }).use { zip ->
                zip.setLevel(Deflater.BEST_COMPRESSION)
                zip.setComment("BetterModel's generated resource pack.")
                map.values.toList().forEachAsync {
                    val result = it.byteArray()
                    synchronized(zip) {
                        zip.putNextEntry(ZipEntry(it.path))
                        zip.write(result)
                        zip.closeEntry()
                    }
                    debugPack {
                        "This file was successfully zipped: ${it.path}"
                    }
                }
            }
        }
    }

    override fun start() {
        BoneTags.entries.forEach {
            BoneTagRegistry.addTag(it)
        }
    }

    override fun reload(info: ReloadInfo) {
        val itemName = ConfigManagerImpl.item().name.lowercase()
        val namespace = ConfigManagerImpl.namespace()
        itemModelNamespace = NamespacedKey(namespace, MODERN_MODEL_ITEM_NAME)
        val texturesPath = "assets/$namespace/textures/item"
        val modelsPath = "assets/$namespace/models/item"
        val modernModelsPath = "assets/$namespace/models/modern_item"
        val zipper = PackDataZipper()

        renderMap.clear()
        if (ConfigManagerImpl.module().model()) {
            val modelFileMap = ConcurrentHashMap<String, Pair<File, ModelBlueprint>>()
            DATA_FOLDER.getOrCreateDirectory("models") { folder ->
                folder.addResourceAs("demon_knight.bbmodel")
            }.fileTreeList()
                .filter { it.extension == "bbmodel" }
                .forEachAsync {
                    val load = it.toModel()
                    modelFileMap.compute(load.name) compute@ { _, v ->
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
                                zipper.add(texturesPath, "${image.name}.png") {
                                    image.image.toByteArray()
                                }
                                image.mcmeta()?.let { meta ->
                                    zipper.add(texturesPath, "${image.name}.png.mcmeta") {
                                        meta.toByteArray()
                                    }
                                }
                            }
                        }
                    }
                }
            val model = modelFileMap.values
                .asSequence()
                .sortedBy { it.first }
                .map { it.second }
                .toList()
            val entries = JsonArray()
            val maxScale = model.maxOfOrNull {
                it.scale()
            } ?: 1F
            var index = 1
            model.forEach { load ->
                renderMap[load.name] = load.toRenderer(maxScale) render@ { blueprintGroup ->
                    if (PLUGIN.version().useModernResource()) {
                        //Modern
                        val modernBlueprint = blueprintGroup.buildModernJson(maxScale, load) ?: return@render null
                        entries.add(JsonObject().apply {
                            addProperty("threshold", index)
                            add("model", modernBlueprint.toModernJson())
                        })
                        modernBlueprint.forEach { json ->
                            zipper.add(modernModelsPath, "${json.name}.json") {
                                json.element.get().toByteArray()
                            }
                        }
                    } else {
                        //Legacy
                        val blueprint = blueprintGroup.buildJson(maxScale, load) ?: return@render null
                        entries.add(JsonObject().apply {
                            add("predicate", JsonObject().apply {
                                addProperty("custom_model_data", index)
                            })
                            addProperty("model", "$namespace:item/${blueprint.name}")
                        })
                        zipper.add(modelsPath, "${blueprint.name}.json") {
                            blueprint.element.get().toByteArray()
                        }
                    }
                    index++
                }.apply {
                    debugPack {
                        "This model was successfully imported: ${load.name}"
                    }
                    ModelImportedEvent(this).call()
                }
            }
            if (PLUGIN.version().useModernResource()) {
                zipper.add("assets/$namespace/items", "$MODERN_MODEL_ITEM_NAME.json") {
                    JsonObject().apply {
                        add("model", JsonObject().apply {
                            addProperty("type", "range_dispatch")
                            addProperty("property", "custom_model_data")
                            add("fallback", JsonObject().apply {
                                addProperty("type", "minecraft:model")
                                addProperty("model", "minecraft:item/$itemName")
                            })
                            add("entries", entries)
                        })
                    }.toByteArray()
                }
            } else {
                zipper.add(MINECRAFT_ITEM_PATH, "$itemName.json") {
                    JsonObject().apply {
                        addProperty("parent", "minecraft:item/generated")
                        add("textures", JsonObject().apply {
                            addProperty("layer0", "minecraft:item/$itemName")
                        })
                        add("overrides", entries)
                    }.toByteArray()
                }
            }
        }

        if (!info.firstReload) runCatching {
            if (ConfigManagerImpl.module().playerAnimation()) {
                if (SkinManagerImpl.supported()) SkinManagerImpl.write { path, supplier ->
                    zipper.add(path, supplier)
                } else PLUGIN.loadAssets("pack") { s, i ->
                    val read = i.readAllBytes()
                    zipper.add("", s) {
                        read
                    }
                }
            }
            if (ConfigManagerImpl.createPackMcmeta()) {
                PLUGIN.getResource("icon.png")?.buffered()?.let {
                    val read = it.readAllBytes()
                    zipper.add("", "pack.png") {
                        read
                    }
                }
                zipper.add("", "pack.mcmeta") {
                    PACK_MCMETA.toByteArray()
                }
            }
            when (ConfigManagerImpl.packType()) {
                FOLDER -> zipper.toFileTree(File(DATA_FOLDER.parent, ConfigManagerImpl.buildFolderLocation()).apply {
                    mkdirs()
                })
                ZIP -> zipper.toZip(File(DATA_FOLDER.parent, "${ConfigManagerImpl.buildFolderLocation()}.zip"))
            }
        }.handleFailure {
            "Unable to pack resource pack."
        }
    }

    private fun List<BlueprintJson>.toModernJson() = if (size == 1) get(0).toModernJson() else JsonObject().apply {
        addProperty("type", "minecraft:composite")
        add("models", map { it.toModernJson() }.fold(JsonArray(size)) { array, element -> array.apply { add(element) } })
    }

    private fun BlueprintJson.toModernJson() = JsonObject().apply {
        addProperty("type", "minecraft:model")
        addProperty("model", "${ConfigManagerImpl.namespace()}:modern_item/${name}")
        add("tints", JsonArray().apply {
            add(JsonObject().apply {
                addProperty("type", "minecraft:custom_model_data")
                addProperty("default", 0xFFFFFF)
            })
        })
    }

    private fun ModelBlueprint.toRenderer(scale: Float, consumer: (BlueprintGroup) -> Int?): ModelRenderer {
        fun BlueprintGroup.parse(): RendererGroup {
            return RendererGroup(
                boneName(),
                scale,
                if (boneName().toItemMapper() !== BoneItemMapper.EMPTY) null else consumer(this)?.let { i ->
                    ItemStack(ConfigManagerImpl.item()).apply {
                        itemMeta = itemMeta.apply {
                            @Suppress("DEPRECATION") //To support legacy server :(
                            setCustomModelData(i)
                            if (PLUGIN.version().useModernResource()) itemModel = itemModelNamespace
                        }
                    }
                },
                this,
                children.filterIsInstance<BlueprintGroup>()
                    .associate { it.boneName() to it.parse() },
                hitBox(),
            )
        }
        return ModelRenderer(
            this,
            group.filterIsInstance<BlueprintGroup>()
                .associate { it.boneName() to it.parse() },
            animations
        )
    }

    override fun renderer(name: String): ModelRenderer? = renderMap[name]
    override fun renderers(): List<ModelRenderer> = renderMap.values.toList()
    override fun keys(): Set<String> = Collections.unmodifiableSet(renderMap.keys)
}