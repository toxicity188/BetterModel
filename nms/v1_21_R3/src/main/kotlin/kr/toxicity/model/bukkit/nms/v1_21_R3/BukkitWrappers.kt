/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.nms.v1_21_R3

import kr.toxicity.model.api.bukkit.platform.*
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter.adapt
import kr.toxicity.model.api.bukkit.platform.BukkitItemStack
import kr.toxicity.model.api.platform.*
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

internal fun Entity.wrap() = adapt(this)
internal fun LivingEntity.wrap() = adapt(this)
internal fun OfflinePlayer.wrap() = adapt(this)
internal fun Player.wrap() = adapt(this)
internal fun Location.wrap() = adapt(this)
internal fun World.wrap() = adapt(this)
internal fun ItemStack.wrap() = adapt(this)

internal fun PlatformEntity.unwarp(): Entity = (this as BukkitEntity).source()
internal fun PlatformLivingEntity.unwarp(): LivingEntity = (this as BukkitLivingEntity).source()
internal fun PlatformOfflinePlayer.unwarp(): OfflinePlayer = (this as BukkitOfflinePlayer).source()
internal fun PlatformPlayer.unwarp(): Player = (this as BukkitPlayer).source()
internal fun PlatformLocation.unwarp(): Location = (this as BukkitLocation).source()
internal fun PlatformWorld.unwarp(): World = (this as BukkitWorld).source()
internal fun PlatformItemStack.unwarp(): ItemStack = (this as BukkitItemStack).source()
