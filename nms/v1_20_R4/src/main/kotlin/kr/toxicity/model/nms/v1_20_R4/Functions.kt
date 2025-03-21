package kr.toxicity.model.nms.v1_20_R4

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.data.blueprint.ModelBoundingBox
import net.minecraft.core.BlockPos
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.network.syncher.SynchedEntityData.DataItem
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityAttachment
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.attributes.Attributes
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
    return attachments.get(EntityAttachment.PASSENGER, 0, yRot).let { v ->
        Vector3f((v.x * scale).toFloat(), (v.y * scale).toFloat(), (v.z * scale).toFloat())
    }
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
    if (BetterModel.IS_PAPER) return packAll()!!
    val list = arrayListOf<SynchedEntityData.DataValue<*>>()
    (DATA_ITEMS[this] as Array<DataItem<*>?>).forEach {
        list += (it ?: return@forEach).value()
    }
    return list
}

fun Float.packDegree() = floor(this * 256.0F / 360.0F).toInt().toByte()

fun Entity.isWalking(): Boolean {
    return controllingPassenger?.isWalking() ?: when (this) {
        is Mob -> navigation.isInProgress
        is ServerPlayer -> xMovement() != 0F || zMovement() != 0F
        else -> false
    }
}

fun ServerPlayer.xMovement(): Float {
    return xxa
}

fun ServerPlayer.zMovement(): Float {
    return zza
}

fun LivingEntity.jumpFactor(): Float {
    val f: Float = level().getBlockState(blockPosition()).block.getJumpFactor()
    val f1: Float = level().getBlockState(BlockPos.containing(x, boundingBox.minY - 0.5000001, z)).block.getJumpFactor()
    return if (f.toDouble() == 1.0) f1 else f
}

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