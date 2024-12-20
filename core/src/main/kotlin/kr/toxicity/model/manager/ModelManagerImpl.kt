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

object ModelManagerImpl : ModelManager {

    private val item = Material.LEATHER_BOOTS
    private var index = 0

    private val renderMap = HashMap<String, BlueprintRenderer>()

    override fun reload() {
        renderMap.clear()
        index = 0
        val assets = DATA_FOLDER
            .subFolder("build")
            .clear()
            .subFolder("assets")
        val renderer = assets.subFolder("modelrenderer")
        val textures = renderer.subFolder("textures").subFolder("item")
        val model = renderer.subFolder("models").subFolder("item")

        val itemName = item.name.lowercase()
        val modelJson = JsonObject().apply {
            addProperty("parent", "minecraft:item/generated")
            add("textures", JsonObject().apply {
                addProperty("layer0", "minecraft:item/$itemName")
            })
        }
        val override = JsonArray()

        DATA_FOLDER.subFolder("models").forEach {
            if (it.extension == "bbmodel") {
                val load = it.toModel()
                load.buildImage().forEach { image ->
                    image.image.save(textures.subFile("${image.name}.png"))
                }
                val jsonList = ArrayList<BlueprintJson>()
                renderMap[load.name] = load.toRenderer { blueprintGroup ->
                    if (blueprintGroup.buildJson(load, jsonList)) {
                        val i = ++index
                        override.add(JsonObject().apply {
                            add("predicate", JsonObject().apply {
                                addProperty("custom_model_data", i)
                            })
                            addProperty("model", "modelrenderer:item/${blueprintGroup.jsonName(load)}")
                        })
                        i
                    } else null
                }
                jsonList.forEach { json ->
                    json.element.save(model.subFile("${json.name}.json"))
                }
            }
        }
        modelJson.add("overrides", override)
        modelJson.save(assets.subFolder("minecraft")
            .subFolder("models")
            .subFolder("item")
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
                }.toMap()
            )
        }
        return BlueprintRenderer(group.mapNotNull {
            if (it is BlueprintGroup) {
                it.name to it.parse()
            } else null
        }.toMap(), animations)
    }

    override fun renderer(name: String): BlueprintRenderer? = renderMap[name]
}