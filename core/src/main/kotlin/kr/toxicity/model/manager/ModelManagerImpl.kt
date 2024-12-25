package kr.toxicity.model.manager

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kr.toxicity.model.api.data.blueprint.BlueprintChildren.BlueprintGroup
import kr.toxicity.model.api.data.blueprint.BlueprintJson
import kr.toxicity.model.api.data.blueprint.ModelBlueprint
import kr.toxicity.model.api.data.renderer.BlueprintRenderer
import kr.toxicity.model.api.data.renderer.RendererGroup
import kr.toxicity.model.api.manager.ModelManager
import kr.toxicity.model.util.*
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.Collections

object ModelManagerImpl : ModelManager, GlobalManagerImpl {

    private val item = Material.LEATHER_HORSE_ARMOR
    private var index = 1

    private val renderMap = HashMap<String, BlueprintRenderer>()

    override fun reload() {
        renderMap.clear()
        index = 1
        val assets = DATA_FOLDER
            .subFolder("build")
            .clear()
            .subFolder("assets")
        val renderer = assets.subFolder("modelrenderer")
        val textures = renderer.subFolder("textures").subFolder("item")
        val model = renderer.subFolder("models").subFolder("item")
        val modernMode = renderer.subFolder("models").subFolder("modern_item")

        val itemName = item.name.lowercase()
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

        DATA_FOLDER.subFolder("models").forEach {
            if (it.extension == "bbmodel") {
                val load = it.toModel()
                load.buildImage().forEach { image ->
                    image.image.save(textures.subFile("${image.name}.png"))
                }
                val jsonList = ArrayList<BlueprintJson>()
                val modernJsonList = ArrayList<BlueprintJson>()
                renderMap[load.name] = load.toRenderer render@ { blueprintGroup ->
                    if (blueprintGroup.buildJson(-2, load, jsonList)) {
                        override.add(JsonObject().apply {
                            add("predicate", JsonObject().apply {
                                addProperty("custom_model_data", index)
                            })
                            addProperty("model", "modelrenderer:item/${blueprintGroup.jsonName(load)}")
                        })
                    } else return@render null
                    if (blueprintGroup.buildJson(0, load, modernJsonList)) {
                        modernEntries.add(JsonObject().apply {
                            addProperty("threshold", index)
                            add("model", JsonObject().apply {
                                addProperty("type", "minecraft:model")
                                addProperty("model", "modelrenderer:modern_item/${blueprintGroup.jsonName(load)}")
                                add("tints", JsonArray().apply {
                                    add(JsonObject().apply {
                                        addProperty("type", "minecraft:custom_model_data")
                                        addProperty("default", 0xFF8060)
                                    })
                                })
                            })
                        })
                    } else return@render null
                    index++
                }
                jsonList.forEach { json ->
                    json.element.save(model.subFile("${json.name}.json"))
                }
                modernJsonList.forEach { json ->
                    json.element.save(modernMode.subFile("${json.name}.json"))
                }
            }
        }
        modelJson.apply {
            add("overrides", override)
        }.save(assets.subFolder("minecraft")
            .subFolder("models")
            .subFolder("item")
            .subFile("$itemName.json"))
        JsonObject().apply {
            add("model", modernModelJson.apply {
                add("entries", modernEntries)
            })
        }.save(assets.subFolder("minecraft")
            .subFolder("items")
            .subFile("$itemName.json"))
    }

    private fun ModelBlueprint.toRenderer(consumer: (BlueprintGroup) -> Int?): BlueprintRenderer {
        fun BlueprintGroup.parse(): RendererGroup {
            return RendererGroup(
                name,
                scale.toFloat(),
                consumer(this)?.let { i ->
                    ItemStack(item).apply {
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
                hitBox()
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