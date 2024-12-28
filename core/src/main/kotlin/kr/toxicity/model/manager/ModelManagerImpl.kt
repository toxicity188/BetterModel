package kr.toxicity.model.manager

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kr.toxicity.model.api.data.blueprint.BlueprintChildren.BlueprintGroup
import kr.toxicity.model.api.data.blueprint.BlueprintJson
import kr.toxicity.model.api.data.blueprint.ModelBlueprint
import kr.toxicity.model.api.data.renderer.BlueprintRenderer
import kr.toxicity.model.api.data.renderer.RendererGroup
import kr.toxicity.model.api.manager.ConfigManager
import kr.toxicity.model.api.manager.ConfigManager.PackType.*
import kr.toxicity.model.api.manager.ModelManager
import kr.toxicity.model.util.*
import org.bukkit.inventory.ItemStack
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.Collections
import java.util.Comparator
import java.util.TreeMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ModelManagerImpl : ModelManager, GlobalManagerImpl {

    private var index = 1

    private val renderMap = HashMap<String, BlueprintRenderer>()

    private const val MINECRAFT_ITEM_PATH = "assets/minecraft/models/item"
    private const val MODERN_MINECRAFT_ITEM_PATH = "assets/minecraft/items"

    private class PackData(
        val path: String,
        val byteArray: () -> ByteArray
    ) : () -> ByteArray by byteArray

    private class PackDataZipper {
        private val map = hashMapOf<String, PackData>()

        fun add(parent: String, name: String, byteArray: () -> ByteArray) {
            val n = "$parent/$name"
            if (map.put(n, PackData(n, byteArray)) != null) warn(
                "Name collision found: $n"
            )
        }

        fun toFileTree(file: File) {
            val fileTree = Collections.synchronizedMap(TreeMap<String, File>(Comparator.reverseOrder()))
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
            if (ConfigManagerImpl.enablePlayerLimb()) PLUGIN.loadAssets("pack") { s, i ->
                s.toFile().outputStream().buffered().use { output ->
                    i.copyTo(output)
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
                if (ConfigManagerImpl.enablePlayerLimb()) PLUGIN.loadAssets("pack") { s, i ->
                    synchronized(zip) {
                        zip.putNextEntry(ZipEntry(s))
                        zip.write(i.readAllBytes())
                        zip.closeEntry()
                    }
                }
            }
        }
    }

    override fun reload() {
        renderMap.clear()
        index = 1
        val zipper = PackDataZipper()
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
        DATA_FOLDER.subFolder("models").forEachAllFolder {
            if (it.extension == "bbmodel") {
                val load = it.toModel()
                load.buildImage().forEach { image ->
                    zipper.add(texturesPath, "${image.name}.png") {
                        image.image.toByteArray()
                    }
                }
                val jsonList = arrayListOf<BlueprintJson>()
                val modernJsonList = arrayListOf<BlueprintJson>()
                renderMap[load.name] = load.toRenderer render@ { blueprintGroup ->
                    val blueprint = blueprintGroup.buildJson(-2, load) ?: return@render null
                    val modernBlueprint = blueprintGroup.buildJson(0, load) ?: return@render null
                    override.add(JsonObject().apply {
                        add("predicate", JsonObject().apply {
                            addProperty("custom_model_data", index)
                        })
                        addProperty("model", "${ConfigManagerImpl.namespace()}:item/${blueprintGroup.jsonName(load)}")
                    })
                    modernEntries.add(JsonObject().apply {
                        addProperty("threshold", index)
                        add("model", JsonObject().apply {
                            addProperty("type", "minecraft:model")
                            addProperty("model", "${ConfigManagerImpl.namespace()}:modern_item/${blueprintGroup.jsonName(load)}")
                            add("tints", JsonArray().apply {
                                add(JsonObject().apply {
                                    addProperty("type", "minecraft:custom_model_data")
                                    addProperty("default", 0xFF8060)
                                })
                            })
                        })
                    })
                    jsonList += blueprint
                    modernJsonList += modernBlueprint
                    index++
                }
                jsonList.forEach { json ->
                    zipper.add(modelsPath, "${json.name}.json") {
                        json.element.toByteArray()
                    }
                }
                modernJsonList.forEach { json ->
                    zipper.add(modernModelsPath, "${json.name}.json") {
                        json.element.toByteArray()
                    }
                }
            }
        }
        zipper.add(MINECRAFT_ITEM_PATH, "$itemName.json") {
            modelJson.apply {
                add("overrides", override)
            }.toByteArray()
        }
        zipper.add(MODERN_MINECRAFT_ITEM_PATH, "$itemName.json") {
            JsonObject().apply {
                add("model", modernModelJson.apply {
                    add("entries", modernEntries)
                })
            }.toByteArray()
        }
        runCatching {
            when (ConfigManagerImpl.packType()) {
                FOLDER -> zipper.toFileTree(File(DATA_FOLDER.parent, ConfigManagerImpl.buildFolderLocation()).apply {
                    mkdirs()
                })
                ZIP -> zipper.zip(File(DATA_FOLDER.parent, "${ConfigManagerImpl.buildFolderLocation()}.zip"))
            }
        }.onFailure {
            warn(
                "Unable to pack resource pack.",
                "Reason: ${it.message ?: it.javaClass.simpleName}",
                "Stack trace: ${it.stackTraceToString()}"
            )
        }
    }

    private fun ModelBlueprint.toRenderer(consumer: (BlueprintGroup) -> Int?): BlueprintRenderer {
        fun BlueprintGroup.parse(): RendererGroup {
            return RendererGroup(
                name,
                scale.toFloat(),
                consumer(this)?.let { i ->
                    ItemStack(ConfigManagerImpl.item()).apply {
                        itemMeta = itemMeta.apply {
                            setCustomModelData(i)
                        }
                    }
                },
                this,
                children.mapNotNull {
                    if (it is BlueprintGroup) {
                        it.name to it.parse()
                    } else null
                }.toMap(),
                hitBox(),
                null
            )
        }
        return BlueprintRenderer(this, group.mapNotNull {
            if (it is BlueprintGroup) {
                it.name to it.parse()
            } else null
        }.toMap(), animations)
    }

    override fun renderer(name: String): BlueprintRenderer? = renderMap[name]
    override fun renderers(): List<BlueprintRenderer> = renderMap.values.toList()
    override fun keys(): Set<String> = Collections.unmodifiableSet(renderMap.keys)
}