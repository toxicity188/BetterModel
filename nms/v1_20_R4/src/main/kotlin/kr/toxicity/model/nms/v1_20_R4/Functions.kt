package kr.toxicity.model.nms.v1_20_R4

import io.netty.buffer.Unpooled
import io.papermc.paper.util.TickThread
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.nms.PacketBundler
import kr.toxicity.model.api.tracker.Tracker
import kr.toxicity.model.api.util.EventUtil
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.network.syncher.SynchedEntityData.DataItem
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.RangedAttackGoal
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal
import net.minecraft.world.entity.animal.FlyingAnimal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.event.Event
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.max

internal inline fun <reified T, reified R> createAdaptedFieldGetter(noinline paperGetter: (T) -> R): (T) -> R {
    return if (BetterModel.IS_PAPER) paperGetter else T::class.java.declaredFields.first {
        R::class.java.isAssignableFrom(it.type)
    }.apply {
        isAccessible = true
    }.let { getter ->
        { t ->
            getter[t] as R
        }
    }
}

internal val EMPTY_QUATERNION = Quaternionf()

internal val CONFIG by lazy {
    BetterModel.plugin().configManager()
}

internal fun List<Int>.toIntSet(): IntOpenHashSet = IntOpenHashSet(this)

internal fun Entity.passengerPosition(): Vector3f {
    return attachments.get(EntityAttachment.PASSENGER, 0, yRot).let { v ->
        Vector3f(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())
    }
}

internal fun Event.call(): Boolean = EventUtil.call(this)

private val DATA_ITEMS by lazy {
    SynchedEntityData::class.java.declaredFields.first {
        it.type.isArray
    }.apply {
        isAccessible = true
    }
}

@Suppress("UNCHECKED_CAST")
internal fun SynchedEntityData.pack(): List<SynchedEntityData.DataValue<*>> {
    if (BetterModel.IS_PAPER) return packAll()!!
    val list = arrayListOf<SynchedEntityData.DataValue<*>>()
    (DATA_ITEMS[this] as Array<DataItem<*>?>).forEach {
        list += (it ?: return@forEach).value()
    }
    return list
}

internal fun Entity.isWalking(): Boolean {
    return controllingPassenger?.isWalking() ?: when (this) {
        is Mob -> (navigation.isInProgress || goalSelector.availableGoals.any {
            it.isRunning && when (it.goal) {
                is RangedAttackGoal, is RangedCrossbowAttackGoal<*>, is RangedBowAttackGoal<*> -> true
                else -> false
            }
        }) && deltaMovement.horizontalDistance() > 0.002
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

internal val isTickThread
    get() = if (BetterModel.IS_PAPER) TickThread.isTickThread() else Thread.currentThread() === MinecraftServer.getServer().serverThread

internal fun <T> useByteBuf(block: (FriendlyByteBuf) -> T): T {
    val buffer = FriendlyByteBuf(Unpooled.buffer())
    return try {
        block(buffer)
    } finally {
        buffer.release()
    }
}

internal fun PacketBundler.unwrap(): PacketBundlerImpl = this as PacketBundlerImpl

internal fun Tracker.entityFlag(byte: Byte): Byte {
    var b = byte.toInt()
    val hideOption = modifier().hideOption()
    if (hideOption.fire()) b = b and 1.inv()
    if (hideOption.visibility()) b = b or (1 shl 5)
    if (hideOption.glowing()) b = b and (1 shl 6).inv()
    return b.toByte()
}

internal fun org.bukkit.util.Vector.toVanilla() = Vec3(x, y, z)
internal fun Vec3.toBukkit() = org.bukkit.util.Vector(x, y, z)