/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.impl.fabric.armor

import kr.toxicity.model.api.armor.ArmorItem
import kr.toxicity.model.api.armor.PlayerArmor
import net.minecraft.core.component.DataComponents
import net.minecraft.server.network.ServerPlayerConnection
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.item.equipment.EquipmentAssets
import net.minecraft.world.item.equipment.trim.ArmorTrim
import kotlin.jvm.optionals.getOrNull

class PlayerArmorImpl(private val connection: ServerPlayerConnection) : PlayerArmor {

    private val player get() = connection.player

    override fun helmet(): ArmorItem? = player.getItemBySlot(EquipmentSlot.HEAD).toArmorItem()

    override fun leggings(): ArmorItem? = player.getItemBySlot(EquipmentSlot.LEGS).toArmorItem()

    override fun chestplate(): ArmorItem? = player.getItemBySlot(EquipmentSlot.CHEST).toArmorItem()

    override fun boots(): ArmorItem? = player.getItemBySlot(EquipmentSlot.FEET).toArmorItem()

    private fun ItemStack.assetIdOrNull() = get(DataComponents.EQUIPPABLE)?.assetId?.getOrNull()

    private fun ItemStack.toArmorItem() = assetIdOrNull()?.let { asset ->
        val trim = get(DataComponents.TRIM)
        val tint = get(DataComponents.DYED_COLOR)?.rgb
            ?: if (asset === EquipmentAssets.LEATHER) DyedItemColor.LEATHER_COLOR else 0xFFFFFF

        ArmorItem(
            tint,
            asset.identifier().path,
            trim?.getPath(),
            trim?.getPalette()
        )
    }

    private fun ArmorTrim.getPath() = pattern.value().assetId.path

    private fun ArmorTrim.getPalette() = material.value().assets.base.suffix
}
