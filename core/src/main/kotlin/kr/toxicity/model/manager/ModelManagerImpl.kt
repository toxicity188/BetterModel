package kr.toxicity.model.manager

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kr.toxicity.model.api.bone.BoneItemMapper
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
import kr.toxicity.model.api.util.EventUtil
import kr.toxicity.model.api.version.MinecraftVersion
import kr.toxicity.model.util.*
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import java.io.File
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ModelManagerImpl : ModelManager, GlobalManagerImpl {

    private const val MINECRAFT_ITEM_PATH = "assets/minecraft/models/item"
    private const val MODERN_MODEL_ITEM_NAME = "bm_models"

    private var index = 1
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
            val n = if (parent.isEmpty()) name else "$parent/$name"
            if (map.put(n, PackData(n, byteArray)) != null) warn(
                "Name collision found: $n"
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
            }
            fileTree.values.forEach(File::delete)
            map.clear()
        }

        fun zip(file: File) {
            ZipOutputStream(runCatching {
                MessageDigest.getInstance("SHA-1")
            }.getOrNull()?.let {
                DigestOutputStream(file.outputStream().buffered(), it)
            } ?: file.outputStream().buffered()).use { zip ->
                map.values.toList().forEachAsync {
                    val result = it.byteArray()
                    synchronized(zip) {
                        zip.putNextEntry(ZipEntry(it.path))
                        zip.write(result)
                        zip.closeEntry()
                    }
                }
            }
        }
    }

    override fun reload(info: ReloadInfo) {
        renderMap.clear()
        index = 1

        val zipper = PackDataZipper()
        val modernModel = PackDataZipper()
        val legacyModel = PackDataZipper()

        val itemName = ConfigManagerImpl.item().name.lowercase()
        val modelJson = JsonObject().apply {
            addProperty("parent", "minecraft:item/generated")
            add("textures", JsonObject().apply {
                addProperty("layer0", "minecraft:item/$itemName")
            })
        }
        val modernModelJson = JsonObject().apply {
            addProperty("type", "range_dispatch")
            addProperty("property", "custom_model_data")
            add("fallback", JsonObject().apply {
                addProperty("type", "minecraft:model")
                addProperty("model", "minecraft:item/$itemName")
            })
        }
        val override = JsonArray()
        val modernEntries = JsonArray()

        val namespace = ConfigManagerImpl.namespace()
        itemModelNamespace = NamespacedKey(namespace, MODERN_MODEL_ITEM_NAME)
        val texturesPath = "assets/$namespace/textures/item"
        val modelsPath = "assets/$namespace/models/item"
        val modernModelsPath = "assets/$namespace/models/modern_item"

        renderMap.clear()
        val model = arrayListOf<ModelBlueprint>()
        val modelNames = hashMapOf<String, String>() // Map of model name -> source file name

        if (ConfigManagerImpl.module().model()) {
            DATA_FOLDER.subFolder("models").forEachAllFolder {
                if (it.extension == "bbmodel") {
                    val load = it.toModel()
                    val existingFile = modelNames[load.name]
                    if (existingFile != null) {
                        // A model with the same name already exists from a different file
                        warn("Duplicate model name '${load.name}'. Files '${existingFile}' and '${it.name}' result in the same name. '${it.name}' will not be loaded.")
                        return@forEachAllFolder
                    }
                    modelNames[load.name] = it.name
                    load.buildImage().forEach { image ->
                        zipper.add(texturesPath, "${image.name}.png") {
                            image.image.toByteArray()
                        }
                        image.mcmeta()?.let { meta ->
                            zipper.add(texturesPath, "${image.name}.png.mcmeta") {
                                meta.toByteArray()
                            }
                        }
                    }
                    model += load
                }
            }
            val maxScale = model.maxOfOrNull {
                it.scale()
            } ?: 1F
            model.forEach { load ->
                val jsonList = arrayListOf<BlueprintJson>()
                val modernJsonList = arrayListOf<BlueprintJson>()
                renderMap[load.name] = load.toRenderer(maxScale) render@ { blueprintGroup ->
                    //Modern
                    val modernBlueprint = blueprintGroup.buildModernJson(maxScale, load) ?: return@render null
                    modernEntries.add(JsonObject().apply {
                        addProperty("threshold", index)
                        add("model", modernBlueprint.toModernJson())
                    })
                    modernJsonList += modernBlueprint
                    //Legacy
                    blueprintGroup.buildJson(maxScale, load)?.let { blueprint ->
                        override.add(JsonObject().apply {
                            add("predicate", JsonObject().apply {
                                addProperty("custom_model_data", index)
                            })
                            addProperty("model", "${ConfigManagerImpl.namespace()}:item/${blueprint.name}")
                        })
                        jsonList += blueprint
                    }
                    index++
                }.apply {
                    EventUtil.call(ModelImportedEvent(this))
                }
                jsonList.forEach { json ->
                    legacyModel.add(modelsPath, "${json.name}.json") {
                        json.element.toByteArray()
                    }
                }
                modernJsonList.forEach { json ->
                    modernModel.add(modernModelsPath, "${json.name}.json") {
                        json.element.toByteArray()
                    }
                }
            }
            legacyModel.add(MINECRAFT_ITEM_PATH, "$itemName.json") {
                modelJson.apply {
                    add("overrides", override)
                }.toByteArray()
            }
            modernModel.add("assets/$namespace/items", "$MODERN_MODEL_ITEM_NAME.json") {
                JsonObject().apply {
                    add("model", modernModelJson.apply {
                        add("entries", modernEntries)
                    })
                }.toByteArray()
            }
        }

        if (!info.firstReload) runCatching {
            if (ConfigManagerImpl.module().playerAnimation()) {
                PLUGIN.loadAssets("pack") { s, i ->
                    val read = i.readAllBytes()
                    legacyModel.add("", s) {
                        read
                    }
                }
                PLUGIN.loadAssets("modern_pack") { s, i ->
                    val read = i.readAllBytes()
                    modernModel.add("", s) {
                        read
                    }
                }
            }
            zipper.add("${ConfigManagerImpl.namespace()}_modern", modernModel)
            if (!ConfigManagerImpl.disableGeneratingLegacyModels()) zipper.add("${ConfigManagerImpl.namespace()}_legacy", legacyModel)
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
                ZIP -> zipper.zip(File(DATA_FOLDER.parent, "${ConfigManagerImpl.buildFolderLocation()}.zip"))
            }
        }.handleFailure {
            "Unable to pack resource pack."
        }
    }

    private fun List<BlueprintJson>.toModernJson() = if (size == 1) get(0).toModernJson() else JsonObject().apply {
        addProperty("type", "minecraft:composite")
        add("models", map { it.toModernJson() }.fold(JsonArray()) { array, element -> array.apply { add(element) } })
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
                            if (PLUGIN.version() >= MinecraftVersion.V1_21_4) itemModel = itemModelNamespace
                        }
                    }
                },
                this,
                children.mapNotNull {
                    if (it is BlueprintGroup) {
                        it.boneName() to it.parse()
                    } else null
                }.toMap(),
                hitBox(),
            )
        }
        return ModelRenderer(this, group.mapNotNull {
            if (it is BlueprintGroup) {
                it.boneName() to it.parse()
            } else null
        }.toMap(), animations)
    }

    override fun renderer(name: String): ModelRenderer? = renderMap[name]
    override fun renderers(): List<ModelRenderer> = renderMap.values.toList()
    override fun keys(): Set<String> = Collections.unmodifiableSet(renderMap.keys)
}