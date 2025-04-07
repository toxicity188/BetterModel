package kr.toxicity.model.nms.v1_21_R2

import ca.spottedleaf.moonrise.common.util.TickThread
import kr.toxicity.model.api.BetterModel
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.network.syncher.SynchedEntityData.DataItem
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.*
import net.minecraft.world.entity.animal.FlyingAnimal
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.joml.Vector3f
import kotlin.math.floor

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
    val leftMovement: Boolean = lastClientInput.left()
    val rightMovement: Boolean = lastClientInput.right()
    return if (leftMovement == rightMovement) 0F else if (leftMovement) 1F else -1F
}

fun ServerPlayer.yMovement(): Float = if (isJump()) 1F else if (lastClientInput.shift) -1F else 0F

fun ServerPlayer.zMovement(): Float {
    val forwardMovement: Boolean = lastClientInput.forward()
    val backwardMovement: Boolean = lastClientInput.backward()
    return if (forwardMovement == backwardMovement) 0F else if (forwardMovement) 1F else -1F
}

fun ServerPlayer.isJump() = lastClientInput.jump()

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