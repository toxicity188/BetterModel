package kr.toxicity.model.nms.v1_19_R3

import kr.toxicity.model.api.data.blueprint.ModelBoundingBox
import net.minecraft.core.BlockPos
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.network.syncher.SynchedEntityData.DataItem
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
import org.bukkit.Bukkit
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.joml.Vector3f
import kotlin.math.floor
import kotlin.math.max

operator fun ModelBoundingBox.times(scale: Double) = ModelBoundingBox(
    minX * scale,
    minY * scale,
    minZ * scale,
    maxX * scale,
    maxY * scale,
    maxZ * scale
)

fun Entity.passengerPosition(scale: Double): Vector3f {
    return Vector3f(0F, getDimensions(pose).height * scale.toFloat(), 0F)
}

fun Event.call(): Boolean {
    Bukkit.getPluginManager().callEvent(this)
    return if (this is Cancellable) !isCancelled else true
}

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

fun Float.packDegree() = floor(this * 256.0F / 360.0F).toInt().toByte()

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
    val f: Float = level.getBlockState(blockPosition()).block.getJumpFactor()
    val f1: Float = level.getBlockState(BlockPos.containing(x, boundingBox.minY - 0.5000001, z)).block.getJumpFactor()
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