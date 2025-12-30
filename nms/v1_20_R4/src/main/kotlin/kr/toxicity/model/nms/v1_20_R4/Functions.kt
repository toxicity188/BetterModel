/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.nms.v1_20_R4

import io.netty.buffer.Unpooled
import io.papermc.paper.adventure.PaperAdventure
import io.papermc.paper.configuration.GlobalConfiguration
import it.unimi.dsi.fastutil.ints.IntSet
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.tracker.EntityTrackerRegistry
import kr.toxicity.model.api.util.EventUtil
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.network.syncher.SynchedEntityData.DataItem
import net.minecraft.network.syncher.SynchedEntityData.DataValue
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.RangedAttackGoal
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal
import net.minecraft.world.entity.animal.FlyingAnimal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.craftbukkit.util.CraftChatMessage
import org.bukkit.event.Event
import org.joml.Vector3f
import java.util.*
import kotlin.math.max

internal inline fun <reified T, reified R> createAdaptedFieldGetter(noinline paperGetter: (T) -> R): (T) -> R {
    return if (BetterModel.IS_PAPER) paperGetter else createAdaptedFieldGetter()
}
internal inline fun <reified T, reified R> createAdaptedFieldGetter(): (T) -> R {
    return T::class.java.declaredFields.first {
        R::class.java.isAssignableFrom(it.type)
    }.apply {
        isAccessible = true
    }.let { getter ->
        { t ->
            getter[t] as R
        }
    }
}

internal fun <H, T> dirtyChecked(hash: () -> H, function: (H) -> T, equalityChecker: (H, H) -> Boolean = { a, b -> a == b }): () -> T {
    val lock = Any()
    var h = hash()
    var value = function(h)
    return {
        val newH = hash()
        if (equalityChecker(h, newH)) value else synchronized(lock) {
            h = newH
            value = function(h)
            value
        }
    }
}

internal val CONFIG get() = BetterModel.config()
internal val EMPTY_ITEM = VanillaItemStack.EMPTY
internal fun BukkitItemStack.asVanilla() = CraftItemStack.asNMSCopy(this)
internal fun VanillaItemStack.asBukkit() = CraftItemStack.asCraftMirror(this)

internal val ONLINE_MODE by lazy(LazyThreadSafetyMode.NONE) {
    if (BetterModel.IS_PAPER) GlobalConfiguration.get().proxies.isProxyOnlineMode else Bukkit.getOnlineMode()
}

internal fun List<Int>.toIntSet(): IntSet = IntSet.of(*toIntArray())

internal fun Entity.passengerPosition(dest: Vector3f): Vector3f {
    return attachments.get(EntityAttachment.PASSENGER, 0, yRot).let { v ->
        dest.set(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())
    }
}

internal fun Event.call(): Boolean = EventUtil.call(this)

private val DATA_ITEMS = SynchedEntityData::class.java.declaredFields.first {
    it.type.isArray
}.apply {
    isAccessible = true
}

internal fun SynchedEntityData.pack(
    clean: Boolean = false,
    itemFilter: (DataItem<*>) -> Boolean = { true },
    valueFilter: (DataValue<*>) -> Boolean = { true },
    required: (List<Pair<DataItem<*>, DataValue<*>>>) -> Boolean = { it.isNotEmpty() }
): List<DataValue<*>>? = (DATA_ITEMS[this] as Array<*>)
    .mapNotNull map@ {
        val item = (it as? DataItem<*>)?.takeIf(itemFilter) ?: return@map null
        val value = item.value().takeIf(valueFilter) ?: return@map null
        item to value
    }
    .takeIf(required)
    ?.map {
        if (clean) it.first.isDirty = false
        it.second
    }

internal fun Entity.isWalking(): Boolean {
    return controllingPassenger?.isWalking() ?: when (this) {
        is Mob -> navigation.isInProgress || goalSelector.availableGoals.any {
            it.isRunning && when (it.goal) {
                is RangedAttackGoal, is RangedCrossbowAttackGoal<*>, is RangedBowAttackGoal<*> -> true
                else -> false
            }
        }
        is ServerPlayer -> xMovement() != 0F || zMovement() != 0F
        else -> false
    }
}

internal fun ServerPlayer.xMovement(): Float {
    return xxa
}

internal fun ServerPlayer.yMovement(): Float = if (isJump()) 1F else if (isShiftKeyDown) -1F else 0F

internal fun ServerPlayer.zMovement(): Float {
    return zza
}

internal fun LivingEntity.jumpFactor(): Float {
    val f: Float = level().getBlockState(blockPosition()).block.getJumpFactor()
    val f1: Float = level().getBlockState(BlockPos.containing(x, boundingBox.minY - 0.5000001, z)).block.getJumpFactor()
    return if (f.toDouble() == 1.0) f1 else f
}

internal fun LivingEntity.jumpFromGround() {
    val jumpPower = (getAttribute(Attributes.JUMP_STRENGTH)?.value?.toFloat() ?: 0.4F) * jumpFactor() + jumpBoostPower
    if (!(jumpPower <= 1.0E-5f)) {
        val deltaMovement = deltaMovement
        setDeltaMovement(deltaMovement.x, max(jumpPower.toDouble(), deltaMovement.y), deltaMovement.z)
        if (isSprinting) {
            val f = yRot * (Math.PI / 180.0).toFloat()
            addDeltaMovement(Vec3(-Mth.sin(f) * 0.2, 0.0, Mth.cos(f) * 0.2))
        }
        hasImpulse = true
    }
}

internal fun ServerPlayer.isJump() = jumping

internal val Entity.isFlying: Boolean
    get() = when (this) {
        is FlyingAnimal -> isFlying
        is FlyingMob -> true
        is Mob -> isNoAi
        is Player -> abilities.flying
        is LivingEntity -> isFallFlying
        else -> false
    }

internal val CraftEntity.vanillaEntity: Entity
    get() = if (BetterModel.IS_PAPER) handleRaw else handle

internal inline fun <T> useByteBuf(block: (FriendlyByteBuf) -> T): T {
    val buffer = FriendlyByteBuf(Unpooled.buffer())
    return try {
        block(buffer)
    } finally {
        buffer.release()
    }
}

internal fun EntityTrackerRegistry.entityFlag(uuid: UUID, byte: Byte): Byte {
    var b = byte.toInt()
    val hideOption = hideOption(uuid)
    if (hideOption.fire()) b = b and 1.inv()
    if (hideOption.visibility()) b = b or (1 shl 5)
    if (hideOption.glowing()) b = b and (1 shl 6).inv()
    return b.toByte()
}

internal fun org.bukkit.util.Vector.toVanilla() = Vec3(x, y, z)
internal fun Vec3.toBukkit() = org.bukkit.util.Vector(x, y, z)

internal inline fun LivingEntity.toEquipmentPacket(mapper: (EquipmentSlot) -> ItemStack? = { if (hasItemInSlot(it)) getItemBySlot(it) else null }): ClientboundSetEquipmentPacket? {
    val equip = EquipmentSlot.entries.mapNotNull {
        mapper(it)?.let { item -> com.mojang.datafixers.util.Pair.of(it, item) }
    }
    return if (equip.isNotEmpty()) ClientboundSetEquipmentPacket(id, equip) else null
}
internal fun LivingEntity.toEmptyEquipmentPacket() = toEquipmentPacket { ItemStack.EMPTY }

internal val Player.hotbarSlot get() = inventory.selected + 36
internal val PLAYER_EQUIPMENT_SLOT = IntSet.of(*intArrayOf(45, 5, 6, 7, 8))
internal fun ClientboundContainerSetSlotPacket.isEquipment(player: Player) = containerId == 0 && (PLAYER_EQUIPMENT_SLOT.contains(slot) || slot == player.hotbarSlot)

internal fun Entity.toFakeAddPacket() = ClientboundAddEntityPacket(
    id,
    uuid,
    x,
    y,
    z,
    xRot,
    yRot,
    EntityType.ITEM_DISPLAY,
    0,
    deltaMovement,
    yHeadRot.toDouble()
)

internal fun Player.toCustomisation() = entityData.get(Player.DATA_PLAYER_MODE_CUSTOMISATION).toInt()

internal fun VanillaComponent.asAdventure() = if (BetterModel.IS_PAPER) {
    PaperAdventure.asAdventure(this)
} else {
    GsonComponentSerializer.gson().deserialize(CraftChatMessage.toJSON(this))
}

internal fun AdventureComponent.asVanilla() = if (BetterModel.IS_PAPER) {
    PaperAdventure.asVanilla(this)
} else {
    CraftChatMessage.fromJSON(GsonComponentSerializer.gson().serialize(this))
}
