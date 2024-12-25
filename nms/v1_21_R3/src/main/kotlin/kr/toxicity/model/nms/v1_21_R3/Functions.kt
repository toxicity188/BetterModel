package kr.toxicity.model.nms.v1_21_R3

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityAttachment
import net.minecraft.world.phys.AABB
import org.joml.Vector3f

operator fun AABB.times(scale: Double) = AABB(
    minX * scale,
    minY * scale,
    minZ * scale,
    maxX * scale,
    maxY * scale,
    maxZ * scale
)

operator fun AABB.div(scale: Double) = AABB(
    minX / scale,
    minY / scale,
    minZ / scale,
    maxX / scale,
    maxY / scale,
    maxZ / scale
)

operator fun AABB.plus(other: AABB): AABB = AABB(
    minX + other.minX,
    minX + other.minY,
    minZ + other.minZ,
    maxX + other.maxX,
    maxY + other.maxY,
    maxZ + other.maxZ
)

fun Entity.passengerPosition(): Vector3f {
    return attachments.get(EntityAttachment.PASSENGER, 0, yRot).let { v ->
        Vector3f(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())
    }
}