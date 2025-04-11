package kr.toxicity.model.nms.v1_21_R4

import kr.toxicity.model.api.event.ModelDamageSource
import net.minecraft.world.damagesource.DamageSource
import org.bukkit.Location
import org.bukkit.craftbukkit.util.CraftLocation
import org.bukkit.entity.Entity

class ModelDamageSourceImpl(
    private val source: DamageSource
) : ModelDamageSource {
    override fun getCausingEntity(): Entity? = source.entity?.bukkitEntity
    override fun getDirectEntity(): Entity? = source.directEntity?.bukkitEntity
    override fun getDamageLocation(): Location? = source.sourcePositionRaw()?.let {
        CraftLocation.toBukkit(it, causingEntity?.world)
    }
    override fun getSourceLocation(): Location? = source.sourcePosition?.let {
        CraftLocation.toBukkit(it, causingEntity?.world)
    }
    override fun isIndirect(): Boolean = !source.isDirect
    override fun getFoodExhaustion(): Float = source.foodExhaustion
    override fun scalesWithDifficulty(): Boolean = source.scalesWithDifficulty()
}