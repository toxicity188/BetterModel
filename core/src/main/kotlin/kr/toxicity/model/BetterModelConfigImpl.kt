package kr.toxicity.model

import kr.toxicity.model.api.BetterModelConfig
import kr.toxicity.model.api.config.DebugConfig
import kr.toxicity.model.api.config.ModuleConfig
import kr.toxicity.model.api.mount.MountController
import kr.toxicity.model.api.mount.MountControllers
import kr.toxicity.model.api.pack.PackConfig
import kr.toxicity.model.api.util.EntityUtil
import kr.toxicity.model.util.ifNull
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import java.io.File

class BetterModelConfigImpl(yaml: ConfigurationSection) : BetterModelConfig {

    private var debug = yaml.getConfigurationSection("debug")?.let {
        DebugConfig.from(it)
    } ?: DebugConfig.DEFAULT
    private var module = yaml.getConfigurationSection("module")?.let {
        ModuleConfig.from(it)
    } ?: ModuleConfig.DEFAULT
    private var pack = yaml.getConfigurationSection("pack")?.let {
        PackConfig.from(it)
    } ?: PackConfig.DEFAULT
    private var metrics = yaml.getBoolean("metrics", true)
    private var sightTrace = yaml.getBoolean("sight-trace", true)
    private var item = yaml.getString("item")?.let {
        runCatching {
            Material.getMaterial(it.uppercase()).ifNull { "This item doesn't exist: $it" }
        }.getOrDefault(Material.LEATHER_HORSE_ARMOR)
    } ?: Material.LEATHER_HORSE_ARMOR
    private var maxSight = yaml.getDouble("max-sight", -1.0).run {
        if (this <= 0.0) EntityUtil.RENDER_DISTANCE else this
    }
    private var minSight = yaml.getDouble("min-sight", 5.0)
    private var lockOnPlayAnimation = yaml.getBoolean("lock-on-play-animation", false)
    private var namespace = yaml.getString("namespace") ?: "bettermodel"
    private var packType = yaml.getString("pack-type")?.let {
        runCatching {
            BetterModelConfig.PackType.valueOf(it.uppercase())
        }.getOrNull()
    } ?: BetterModelConfig.PackType.ZIP
    private var buildFolderLocation = (yaml.getString("build-folder-location") ?: "BetterModel/build").replace('/', File.separatorChar)
    private var followMobInvisibility = yaml.getBoolean("follow-mob-invisibility", true)
    private var animatedTextureFrameTime = yaml.getInt("animated-texture-frame-time", 10)
    private var usePurpurAfk = yaml.getBoolean("use-purpur-afk", true)
    private var versionCheck = yaml.getBoolean("version-check", true)
    private var defaultMountController = when (yaml.getString("default-mount-controller")?.lowercase()) {
        "invalid" -> MountControllers.INVALID
        "none" -> MountControllers.NONE
        "fly" -> MountControllers.FLY
        else -> MountControllers.WALK
    }
    private var lerpFrameTime = yaml.getInt("lerp-frame-time", 5)
    private var cancelPlayerModelInventory = yaml.getBoolean("cancel-player-model-inventory")
    private var playerHideDelay = yaml.getLong("player-hide-delay", 3L).coerceAtLeast(1L)

    override fun debug(): DebugConfig = debug
    override fun module(): ModuleConfig = module
    override fun pack(): PackConfig = pack
    override fun item(): Material = item
    override fun metrics(): Boolean = metrics
    override fun sightTrace(): Boolean = sightTrace
    override fun maxSight(): Double = maxSight
    override fun minSight(): Double = minSight
    override fun lockOnPlayAnimation(): Boolean = lockOnPlayAnimation
    override fun namespace(): String = namespace
    override fun packType(): BetterModelConfig.PackType = packType
    override fun buildFolderLocation(): String = buildFolderLocation
    override fun followMobInvisibility(): Boolean = followMobInvisibility
    override fun animatedTextureFrameTime(): Int = animatedTextureFrameTime
    override fun usePurpurAfk(): Boolean = usePurpurAfk
    override fun versionCheck(): Boolean = versionCheck
    override fun defaultMountController(): MountController = defaultMountController
    override fun lerpFrameTime(): Int = lerpFrameTime
    override fun cancelPlayerModelInventory(): Boolean = cancelPlayerModelInventory
    override fun playerHideDelay(): Long = playerHideDelay
}