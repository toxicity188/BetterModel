package kr.toxicity.model.manager

import kr.toxicity.model.api.manager.ConfigManager
import kr.toxicity.model.configuration.PluginConfiguration
import kr.toxicity.model.util.PLUGIN
import kr.toxicity.model.util.ifNull
import org.bstats.bukkit.Metrics
import org.bukkit.Material

object ConfigManagerImpl : ConfigManager, GlobalManagerImpl {

    private var metrics: Metrics? = null
    private var sightTrace = true
    private var item = Material.LEATHER_HORSE_ARMOR
    private var maxSight = 45.0
    private var minSight = 5.0
    private var lockOnPlayAnimation = true
    private var keyframeThreshold = 2L
    private var enablePlayerLimb = true

    override fun item(): Material = item
    override fun metrics(): Boolean = metrics != null
    override fun sightTrace(): Boolean = sightTrace
    override fun maxSight(): Double = maxSight
    override fun minSight(): Double = minSight
    override fun lockOnPlayAnimation(): Boolean = lockOnPlayAnimation
    override fun keyframeThreshold(): Long = keyframeThreshold
    override fun enablePlayerLimb(): Boolean = enablePlayerLimb

    override fun reload() {
        val yaml = PluginConfiguration.CONFIG.create()
        if (yaml.getBoolean("metrics", true)) {
            if (metrics == null) metrics = Metrics(PLUGIN, 24237)
        } else {
            metrics?.shutdown()
            metrics = null
        }
        sightTrace = yaml.getBoolean("sight-trace", true)
        item = yaml.getString("item")?.let {
            runCatching {
                Material.getMaterial(it).ifNull("This item doesn't exist: $it")
            }.getOrDefault(Material.LEATHER_HORSE_ARMOR)
        } ?: Material.LEATHER_HORSE_ARMOR
        maxSight = yaml.getDouble("max-sight", 45.0)
        minSight = yaml.getDouble("min-sight", 5.0)
        lockOnPlayAnimation = yaml.getBoolean("lock-on-play-animation", true)
        keyframeThreshold = yaml.getLong("keyframe-threshold", 2).coerceAtLeast(1)
        enablePlayerLimb = yaml.getBoolean("enable-player-limb", true)
    }
}