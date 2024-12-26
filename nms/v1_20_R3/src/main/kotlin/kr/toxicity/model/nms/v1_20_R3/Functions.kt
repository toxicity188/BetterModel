package kr.toxicity.model.nms.v1_20_R3

import kr.toxicity.model.api.data.blueprint.ModelBoundingBox
import net.minecraft.world.entity.Entity
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
    return Vector3f(0F, getDimensions(pose).height, 0F)
}