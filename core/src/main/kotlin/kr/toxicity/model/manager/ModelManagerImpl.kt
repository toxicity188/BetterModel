package kr.toxicity.model.manager

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kr.toxicity.model.api.data.blueprint.BlueprintChildren.BlueprintGroup
import kr.toxicity.model.api.data.blueprint.BlueprintJson
import kr.toxicity.model.api.data.blueprint.ModelBlueprint
import kr.toxicity.model.api.data.renderer.BlueprintRenderer
import kr.toxicity.model.api.data.renderer.RendererGroup
import kr.toxicity.model.api.manager.ConfigManager.PackType.FOLDER
import kr.toxicity.model.api.manager.ConfigManager.PackType.ZIP
import kr.toxicity.model.api.manager.ModelManager
import kr.toxicity.model.api.manager.ReloadInfo
import kr.toxicity.model.util.*
import net.kyori.adventure.key.Key
import org.bukkit.inventory.ItemStack
import java.io.File
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ModelManagerImpl : ModelManager, GlobalManagerImpl {

    private var index = 1

    private val renderMap = hashMapOf<String, BlueprintRenderer>()

    private const val MINECRAFT_ITEM_PATH = "assets/minecraft/models/item"
    private const val MODERN_MINECRAFT_ITEM_PATH = "assets/minecraft/items"

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

        val texturesPath = "assets/${ConfigManagerImpl.namespace()}/textures/item"
        val modelsPath = "assets/${ConfigManagerImpl.namespace()}/models/item"
        val modernModelsPath = "assets/${ConfigManagerImpl.namespace()}/models/modern_item"

        renderMap.clear()
        val model = arrayListOf<ModelBlueprint>()

        if (ConfigManagerImpl.module().model()) {
            DATA_FOLDER.subFolder("models").forEachAllFolder {
                if (it.extension == "bbmodel") {
                    if (!Key.parseableValue(it.nameWithoutExtension)) {
                        warn("Skipping model with invalid name: '${it.name}', it will not be loaded. Only a-z, 0-9, '-', '_' and '.' characters allowed.")
                        return@forEachAllFolder
                    }
                    val load = it.toModel()
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
                        add("model", JsonObject().apply {
                            addProperty("type", "minecraft:composite")
                            add("models", JsonArray().apply {
                                modernBlueprint.forEach { mb ->
                                    add(JsonObject().apply {
                                        addProperty("type", "minecraft:model")
                                        addProperty("model", "${ConfigManagerImpl.namespace()}:modern_item/${mb.name}")
                                        add("tints", JsonArray().apply {
                                            add(JsonObject().apply {
                                                addProperty("type", "minecraft:custom_model_data")
                                                addProperty("default", 0xFFFFFF)
                                            })
                                        })
                                    })
                                }
                            })
                        })
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
            modernModel.add(MODERN_MINECRAFT_ITEM_PATH, "$itemName.json") {
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
        }.onFailure {
            it.handleException("Unable to pack resource pack.")
        }
    }

    private fun ModelBlueprint.toRenderer(scale: Float, consumer: (BlueprintGroup) -> Int?): BlueprintRenderer {
        fun BlueprintGroup.parse(): RendererGroup {
            return RendererGroup(
                boneName(),
                scale,
                consumer(this)?.let { i ->
                    ItemStack(ConfigManagerImpl.item()).apply {
                        itemMeta = itemMeta.apply {
                            @Suppress("DEPRECATION") //To support legacy server :(
                            setCustomModelData(i)
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
                null
            )
        }
        return BlueprintRenderer(this, group.mapNotNull {
            if (it is BlueprintGroup) {
                it.boneName() to it.parse()
            } else null
        }.toMap(), animations)
    }

    override fun renderer(name: String): BlueprintRenderer? = renderMap[name]
    override fun renderers(): List<BlueprintRenderer> = renderMap.values.toList()
    override fun keys(): Set<String> = Collections.unmodifiableSet(renderMap.keys)
}