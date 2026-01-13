package kr.toxicity.model.nms.v1_21_R5

import kr.toxicity.model.api.bukkit.platform.BukkitAdapter.*
import kr.toxicity.model.api.bukkit.platform.*
import kr.toxicity.model.api.bukkit.platform.BukkitItemStack
import kr.toxicity.model.api.platform.*
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

fun Entity.wrap() = adapt(this)
fun LivingEntity.wrap() = adapt(this)
fun OfflinePlayer.wrap() = adapt(this)
fun Player.wrap() = adapt(this)
fun Location.wrap() = adapt(this)
fun World.wrap() = adapt(this)
fun ItemStack.wrap() = adapt(this)

fun PlatformEntity.unwarp(): Entity = (this as BukkitEntity).source()
fun PlatformLivingEntity.unwarp(): LivingEntity = (this as BukkitLivingEntity).source()
fun PlatformOfflinePlayer.unwarp(): OfflinePlayer = (this as BukkitOfflinePlayer).source()
fun PlatformPlayer.unwarp(): Player = (this as BukkitPlayer).source()
fun PlatformLocation.unwarp(): Location = (this as BukkitLocation).source()
fun PlatformWorld.unwarp(): World = (this as BukkitWorld).source()
fun PlatformItemStack.unwarp(): ItemStack = (this as BukkitItemStack).source()
