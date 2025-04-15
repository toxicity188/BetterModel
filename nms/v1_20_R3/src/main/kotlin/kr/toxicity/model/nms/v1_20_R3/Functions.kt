package kr.toxicity.model.nms.v1_20_R3

import io.netty.buffer.Unpooled
import io.papermc.paper.util.TickThread
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.util.EventUtil
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.network.syncher.SynchedEntityData.DataItem
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.FlyingMob
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.FlyingAnimal
import net.minecraft.world.phys.Vec3
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity
import org.bukkit.event.Event
import org.joml.Vector3f
import kotlin.math.max

inline fun <reified T, reified R> createAdaptedFieldGetter(noinline paperGetter: (T) -> R): (T) -> R {
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

fun Entity.passengerPosition(scale: Double): Vector3f {
    return Vector3f(0F, getDimensions(pose).height * scale.toFloat(), 0F)
}

fun Event.call(): Boolean = EventUtil.call(this)

private val DATA_ITEMS by lazy {
    SynchedEntityData::class.java.declaredFields.first {
        it.type.isArray
    }.apply {
        isAccessible = true
    }
}

@Suppress("UNCHECKED_CAST")
fun SynchedEntityData.pack(): List<SynchedEntityData.DataValue<*>> {
    val list = arrayListOf<SynchedEntityData.DataValue<*>>()
    (DATA_ITEMS[this] as Array<DataItem<*>?>).forEach {
        list += (it ?: return@forEach).value()
    }
    return list
}

fun Entity.isWalking(): Boolean {
    return controllingPassenger?.isWalking() ?: when (this) {
        is Mob -> navigation.isInProgress && deltaMovement.horizontalDistance() > 0.002
        is ServerPlayer -> xMovement() != 0F || zMovement() != 0F
        else -> false
    }
}

fun ServerPlayer.xMovement(): Float {
    return xxa
}

fun ServerPlayer.yMovement(): Float = if (isJump()) 1F else if (isShiftKeyDown) -1F else 0F

fun ServerPlayer.zMovement(): Float {
    return zza
}

fun LivingEntity.jumpFactor(): Float {
    val f: Float = level().getBlockState(blockPosition()).block.getJumpFactor()
    val f1: Float = level().getBlockState(BlockPos.containing(x, boundingBox.minY - 0.5000001, z)).block.getJumpFactor()
    return if (f.toDouble() == 1.0) f1 else f
}

val LivingEntity.gravity: Double
    get() = if (deltaMovement.y <= 0.0 && this.hasEffect(MobEffects.SLOW_FALLING)) 0.01 else 0.08

fun LivingEntity.jumpFromGround() {
    val jumpPower = getAttributeValue(Attributes.JUMP_STRENGTH).toFloat() * jumpFactor() + jumpBoostPower
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

fun ServerPlayer.isJump() = jumping

val Entity.isFlying: Boolean
    get() = when (this) {
        is FlyingAnimal -> isFlying
        is FlyingMob -> true
        is Mob -> isNoAi
        is LivingEntity -> isFallFlying
        else -> false
    }

val CraftEntity.vanillaEntity: Entity
    get() = if (BetterModel.IS_PAPER) handleRaw else handle

val isTickThread
    get() = if (BetterModel.IS_PAPER) TickThread.isTickThread() else Thread.currentThread() === MinecraftServer.getServer().serverThread

fun <T> useByteBuf(block: (FriendlyByteBuf) -> T): T {
    val buffer = FriendlyByteBuf(Unpooled.buffer())
    return try {
        block(buffer)
    } finally {
        buffer.release()
    }
}