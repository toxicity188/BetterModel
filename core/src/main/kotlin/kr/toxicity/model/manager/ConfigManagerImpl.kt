package kr.toxicity.model.manager

import kr.toxicity.model.api.config.DebugConfig
import kr.toxicity.model.api.config.ModuleConfig
import kr.toxicity.model.api.manager.ConfigManager
import kr.toxicity.model.api.manager.ConfigManager.PackType
import kr.toxicity.model.api.manager.ReloadInfo
import kr.toxicity.model.api.mount.MountController
import kr.toxicity.model.api.mount.MountControllers
import kr.toxicity.model.api.pack.PackConfig
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.api.util.EntityUtil
import kr.toxicity.model.configuration.PluginConfiguration
import kr.toxicity.model.util.PLUGIN
import kr.toxicity.model.util.ifNull
import org.bstats.bukkit.Metrics
import org.bukkit.Material
import java.io.File

object ConfigManagerImpl : ConfigManager, GlobalManagerImpl {

    private var debug = DebugConfig.DEFAULT
    private var module = ModuleConfig.DEFAULT
    private var pack = PackConfig.DEFAULT
    private var metrics: Metrics? = null
    private var sightTrace = true
    private var item = Material.LEATHER_HORSE_ARMOR
    private var maxSight = -1.0
    private var minSight = 5.0
    private var lockOnPlayAnimation = false
    private var namespace = "bettermodel"
    private var packType = PackType.FOLDER
    private var buildFolderLocation = "BetterModel/build".replace('/', File.separatorChar)
    private var followMobInvisibility = true
    private var animatedTextureFrameTime = 10
    private var usePurpurAfk = true
    private var versionCheck = true
    private var defaultMountController = MountControllers.WALK
    private var lerpFrameTime = 5
    private var cancelPlayerModelInventory = false
    private var playerHideDelay = 3L

    override fun debug(): DebugConfig = debug
    override fun module(): ModuleConfig = module
    override fun pack(): PackConfig = pack
    override fun item(): Material = item
    override fun metrics(): Boolean = metrics != null
    override fun sightTrace(): Boolean = sightTrace
    override fun maxSight(): Double = maxSight
    override fun minSight(): Double = minSight
    override fun lockOnPlayAnimation(): Boolean = lockOnPlayAnimation
    override fun namespace(): String = namespace
    override fun packType(): PackType = packType
    override fun buildFolderLocation(): String = buildFolderLocation
    override fun followMobInvisibility(): Boolean = followMobInvisibility
    override fun animatedTextureFrameTime(): Int = animatedTextureFrameTime
    override fun usePurpurAfk(): Boolean = usePurpurAfk
    override fun versionCheck(): Boolean = versionCheck
    override fun defaultMountController(): MountController = defaultMountController
    override fun lerpFrameTime(): Int = lerpFrameTime
    override fun cancelPlayerModelInventory(): Boolean = cancelPlayerModelInventory
    override fun playerHideDelay(): Long = playerHideDelay

    override fun reload(info: ReloadInfo, zipper: PackZipper) {
        val yaml = PluginConfiguration.CONFIG.create()
        if (yaml.getBoolean("metrics", true)) {
            if (metrics == null) metrics = Metrics(PLUGIN, 24237)
        } else {
            metrics?.shutdown()
            metrics = null
        }
        debug = yaml.getConfigurationSection("debug")?.let {
            DebugConfig.from(it)
        } ?: DebugConfig.DEFAULT
        module = yaml.getConfigurationSection("module")?.let {
            ModuleConfig.from(it)
        } ?: ModuleConfig.DEFAULT
        pack = yaml.getConfigurationSection("pack")?.let {
            PackConfig.from(it)
        } ?: PackConfig.DEFAULT
        sightTrace = yaml.getBoolean("sight-trace", true)
        item = yaml.getString("item")?.let {
            runCatching {
                Material.getMaterial(it.uppercase()).ifNull { "This item doesn't exist: $it" }
            }.getOrDefault(Material.LEATHER_HORSE_ARMOR)
        } ?: Material.LEATHER_HORSE_ARMOR
        maxSight = yaml.getDouble("max-sight", -1.0)
        if (maxSight <= 0.0) maxSight = EntityUtil.RENDER_DISTANCE
        minSight = yaml.getDouble("min-sight", 5.0)
        lockOnPlayAnimation = yaml.getBoolean("lock-on-play-animation", false)
        namespace = yaml.getString("namespace") ?: "bettermodel"
        animatedTextureFrameTime = yaml.getInt("animated-texture-frame-time", 10)
        packType = yaml.getString("pack-type")?.let {
            runCatching {
                PackType.valueOf(it.uppercase())
            }.getOrNull()
        } ?: PackType.FOLDER
        buildFolderLocation = (yaml.getString("build-folder-location") ?: "BetterModel/build").replace('/', File.separatorChar)
        followMobInvisibility = yaml.getBoolean("follow-mob-invisibility", true)
        usePurpurAfk = yaml.getBoolean("use-purpur-afk", true)
        versionCheck = yaml.getBoolean("version-check", true)
        defaultMountController = when (yaml.getString("default-mount-controller")?.lowercase()) {
            "invalid" -> MountControllers.INVALID
            "none" -> MountControllers.NONE
            "fly" -> MountControllers.FLY
            else -> MountControllers.WALK
        }
        lerpFrameTime = yaml.getInt("lerp-frame-time", 5)
        cancelPlayerModelInventory = yaml.getBoolean("cancel-player-model-inventory")
        playerHideDelay = yaml.getLong("player-hide-delay", 3L).coerceAtLeast(1L)
    }
}