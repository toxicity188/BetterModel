package kr.toxicity.model.compatibility.citizens.trait

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.data.renderer.ModelRenderer
import kr.toxicity.model.api.tracker.EntityTrackerRegistry
import net.citizensnpcs.api.event.DespawnReason
import net.citizensnpcs.api.trait.Trait
import net.citizensnpcs.api.trait.TraitName
import net.citizensnpcs.api.util.DataKey
import net.citizensnpcs.api.util.RemoveReason

@TraitName("model")
class ModelTrait : Trait("model") {
    private var _renderer: ModelRenderer? = null
    var renderer
        get() = _renderer
        set(value) {
            npc.entity?.let {
                value?.create(it) ?: EntityTrackerRegistry.registry(it.uniqueId)?.close()
            }
            _renderer = value
        }

    override fun load(key: DataKey) {
        key.getString("")?.let {
            BetterModel.modelOrNull(it)?.let { model ->
                renderer = model
            }
        }
    }

    override fun save(key: DataKey) {
        key.setString("", npc.entity?.uniqueId?.let { uuid ->
            EntityTrackerRegistry.registry(uuid)?.first()?.name()
        })
    }

    override fun onSpawn() {
        npc.entity?.let {
            if (EntityTrackerRegistry.registry(it.uniqueId) == null) {
                renderer?.create(it)
            }
        }
    }

    override fun onCopy() {
        onSpawn()
    }

    override fun onDespawn() {
        npc?.entity?.uniqueId?.let {
            EntityTrackerRegistry.registry(it)?.close()
        }
    }

    override fun onDespawn(reason: DespawnReason?) {
        onDespawn()
    }

    override fun onRemove() {
        npc?.entity?.uniqueId?.let {
            EntityTrackerRegistry.registry(it)?.close()
        }
    }

    override fun onRemove(reason: RemoveReason?) {
        onRemove()
    }
}