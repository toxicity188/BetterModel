/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.fabric.entity

import kr.toxicity.model.api.armor.PlayerArmor
import kr.toxicity.model.api.fabric.entity.BaseFabricEntity
import kr.toxicity.model.api.fabric.entity.BaseFabricPlayer
import kr.toxicity.model.api.fabric.platform.FabricPlayer
import kr.toxicity.model.api.platform.PlatformPlayer
import kr.toxicity.model.api.player.PlayerSkinParts
import kr.toxicity.model.api.profile.ModelProfile
import kr.toxicity.model.fabric.armor.PlayerArmorImpl
import net.minecraft.server.level.ServerPlayer

class BaseFabricPlayerImpl(
    private val player: ServerPlayer,
    private val profileProvider: () -> ModelProfile,
    private val skinPartsProvider: () -> PlayerSkinParts
) :
    BaseFabricPlayer,
    BaseFabricEntity by BaseFabricEntityImpl(player)
{
    private val armors: PlayerArmorImpl = PlayerArmorImpl(player)

    override fun platform(): PlatformPlayer = FabricPlayer(player)

    override fun updateInventory() = player.containerMenu.sendAllDataToRemote()

    override fun armors(): PlayerArmor = armors

    override fun profile(): ModelProfile = profileProvider.invoke()

    override fun skinParts(): PlayerSkinParts = skinPartsProvider.invoke()
}
