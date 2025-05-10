package kr.toxicity.model.compatibility.hmccosmetics

import com.hibiscusmc.hmccosmetics.api.HMCCosmeticsAPI
import com.hibiscusmc.hmccosmetics.cosmetic.CosmeticSlot
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.bone.BoneTagRegistry
import kr.toxicity.model.api.bone.CustomBoneTag
import kr.toxicity.model.api.event.CloseTrackerEvent
import kr.toxicity.model.api.event.CreateEntityTrackerEvent
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.api.util.TransformedItemStack
import kr.toxicity.model.compatibility.Compatibility
import kr.toxicity.model.util.registerListener
import org.bukkit.entity.ItemDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class HMCCosmeticsCompatibility : Compatibility {
    override fun start() {
        registerListener(object : Listener {
            @EventHandler
            fun CreateEntityTrackerEvent.create() {
                HMCCosmeticsAPI.getUser(sourceEntity().uniqueId)?.let { user ->
                    if (user.isBackpackSpawned) user.userBackpackManager.clearItems()
                }
            }
            @EventHandler
            fun CloseTrackerEvent.remove() {
                (tracker as? EntityTracker)?.sourceEntity()?.let {
                    val user = HMCCosmeticsAPI.getUser(it.uniqueId) ?: return
                    BetterModel.inst().scheduler().task(it) {
                        if (user.isBackpackSpawned) user.userBackpackManager.setItem(user.getUserCosmeticItem(CosmeticSlot.BACKPACK))
                    }
                }
            }
        })
        BoneTagRegistry.addTag(CustomBoneTag(
            "hmccosmetics_backpack",
            ItemDisplay.ItemDisplayTransform.FIXED,
            arrayOf("hmcbp")
        ) {
            HMCCosmeticsAPI.getUser(it.uniqueId)
                ?.getCosmetic(CosmeticSlot.BACKPACK)
                ?.item
                ?.let(TransformedItemStack::of)
        })
    }
}