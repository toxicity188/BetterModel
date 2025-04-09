package kr.toxicity.model.manager

import kr.toxicity.model.api.config.DebugConfig
import kr.toxicity.model.api.config.ModuleConfig
import kr.toxicity.model.api.manager.ConfigManager
import kr.toxicity.model.api.manager.ConfigManager.PackType
import kr.toxicity.model.api.manager.ReloadInfo
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
    private var metrics: Metrics? = null
    private var sightTrace = true
    private var item = Material.LEATHER_HORSE_ARMOR
    private var maxSight = 45.0
    private var minSight = 5.0
    private var lockOnPlayAnimation = true
    private var namespace = "bettermodel"
    private var packType = PackType.FOLDER
    private var buildFolderLocation = "BetterModel/build".replace('/', File.separatorChar)
    private var disableGeneratingLegacyModels = false
    private var followMobInvisibility = true
    private var animatedTextureFrameTime = 10
    private var createPackMcmeta = true
    private var usePurpurAfk = true

    override fun debug(): DebugConfig = debug
    override fun module(): ModuleConfig = module
    override fun item(): Material = item
    override fun metrics(): Boolean = metrics != null
    override fun sightTrace(): Boolean = sightTrace
    override fun maxSight(): Double = maxSight
    override fun minSight(): Double = minSight
    override fun lockOnPlayAnimation(): Boolean = lockOnPlayAnimation
    override fun namespace(): String = namespace
    override fun packType(): PackType = packType
    override fun buildFolderLocation(): String = buildFolderLocation
    override fun disableGeneratingLegacyModels(): Boolean = disableGeneratingLegacyModels
    override fun followMobInvisibility(): Boolean = followMobInvisibility
    override fun animatedTextureFrameTime(): Int = animatedTextureFrameTime
    override fun createPackMcmeta(): Boolean = createPackMcmeta
    override fun usePurpurAfk(): Boolean = usePurpurAfk

    override fun reload(info: ReloadInfo) {
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
        sightTrace = yaml.getBoolean("sight-trace", true)
        item = yaml.getString("item")?.let {
            runCatching {
                Material.getMaterial(it).ifNull { "This item doesn't exist: $it" }
            }.getOrDefault(Material.LEATHER_HORSE_ARMOR)
        } ?: Material.LEATHER_HORSE_ARMOR
        maxSight = yaml.getDouble("max-sight", -1.0)
        if (maxSight <= 0.0) maxSight = EntityUtil.RENDER_DISTANCE
        minSight = yaml.getDouble("min-sight", 5.0)
        lockOnPlayAnimation = yaml.getBoolean("lock-on-play-animation", true)
        namespace = yaml.getString("namespace") ?: "bettermodel"
        animatedTextureFrameTime = yaml.getInt("animated-texture-frame-time", 10)
        createPackMcmeta = yaml.getBoolean("create-pack-mcmeta", true)
        packType = yaml.getString("pack-type")?.let {
            runCatching {
                PackType.valueOf(it.uppercase())
            }.getOrNull()
        } ?: PackType.FOLDER
        buildFolderLocation = (yaml.getString("build-folder-location") ?: "BetterModel/build").replace('/', File.separatorChar)
        disableGeneratingLegacyModels = yaml.getBoolean("disable-generating-legacy-models")
        followMobInvisibility = yaml.getBoolean("follow-mob-invisibility", true)
        usePurpurAfk = yaml.getBoolean("use-purpur-afk", true)
    }
}