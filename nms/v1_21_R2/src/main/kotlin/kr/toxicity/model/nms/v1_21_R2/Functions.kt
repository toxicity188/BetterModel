package kr.toxicity.model.nms.v1_21_R2

import kr.toxicity.model.api.data.blueprint.ModelBoundingBox
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityAttachment
import org.joml.Vector3f

operator fun ModelBoundingBox.times(scale: Double) = ModelBoundingBox(
    minX * scale,
    minY * scale,
    minZ * scale,
    maxX * scale,
    maxY * scale,
    maxZ * scale
)

fun Entity.passengerPosition(): Vector3f {
    return attachments.get(EntityAttachment.PASSENGER, 0, yRot).let { v ->
        Vector3f(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())
    }
}