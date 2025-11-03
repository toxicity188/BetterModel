/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.nms.v1_21_R6

import kr.toxicity.model.api.armor.ArmorItem
import kr.toxicity.model.api.armor.PlayerArmor
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.item.equipment.EquipmentAssets
import org.bukkit.craftbukkit.entity.CraftPlayer

class PlayerArmorImpl(
    private val player: CraftPlayer
) : PlayerArmor {

    override fun helmet(): ArmorItem? {
        return player.handle.getItemBySlot(EquipmentSlot.HEAD).toArmorItem()
    }

    override fun leggings(): ArmorItem? {
        return player.handle.getItemBySlot(EquipmentSlot.LEGS).toArmorItem()
    }

    override fun chestplate(): ArmorItem? {
        return player.handle.getItemBySlot(EquipmentSlot.CHEST).toArmorItem()
    }

    override fun boots(): ArmorItem? {
        return player.handle.getItemBySlot(EquipmentSlot.FEET).toArmorItem()
    }

    private fun VanillaItemStack.toArmorItem(): ArmorItem? = get(DataComponents.EQUIPPABLE)?.assetId?.map {
        val trim = get(DataComponents.TRIM)
        ArmorItem(
            get(DataComponents.DYED_COLOR)?.rgb ?: if (it === EquipmentAssets.LEATHER) DyedItemColor.LEATHER_COLOR else 0xFFFFFF,
            it.location().path,
            trim?.pattern?.value()?.assetId?.path,
            trim?.material?.value()?.assets?.base?.suffix
        )
    }?.orElse(null)
}