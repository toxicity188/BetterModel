package kr.toxicity.model.nms.v1_21_R1

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.data.blueprint.ModelBoundingBox
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.network.syncher.SynchedEntityData.DataItem
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.*
import net.minecraft.world.entity.animal.FlyingAnimal
import org.bukkit.Bukkit
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.joml.Vector3f
import kotlin.math.floor

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

fun ServerPlayer.isJump() = jumping

val Entity.isFlying: Boolean
    get() = when (this) {
        is FlyingAnimal -> isFlying
        is FlyingMob -> true
        is Mob -> isNoAi
        is LivingEntity -> isFallFlying
        else -> false
    }