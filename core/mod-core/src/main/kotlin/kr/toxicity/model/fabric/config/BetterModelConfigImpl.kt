/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.fabric.config

import kr.toxicity.model.api.BetterModelConfig
import kr.toxicity.model.api.config.DebugConfig
import kr.toxicity.model.api.config.IndicatorConfig
import kr.toxicity.model.api.config.ModuleConfig
import kr.toxicity.model.api.config.PackConfig
import kr.toxicity.model.api.mount.MountController
import kr.toxicity.model.api.mount.MountControllers
import kr.toxicity.model.api.platform.PlatformItemStack
import kr.toxicity.model.api.util.EntityUtil
import kr.toxicity.model.fabric.wrap
import kr.toxicity.model.util.toPackName
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.Path
import java.util.function.Supplier

fun Path.toConfig() = BetterModelConfigImpl(YamlConfigurationLoader.builder().path(this).build().load())

class BetterModelConfigImpl(yaml: ConfigurationNode) : BetterModelConfig {

    private val debug = yaml.node("debug")?.let { node ->
        DebugConfig.from { node.node(it).getBoolean(false) }
    } ?: DebugConfig.DEFAULT
    private val indicator = yaml.node("indicator")?.let { node ->
        IndicatorConfig.from { node.node(it).getBoolean(false) }
    } ?: IndicatorConfig.DEFAULT
    private val module = yaml.node("module")?.let { node ->
        ModuleConfig.from { node.node(it).getBoolean(false) }
    } ?: ModuleConfig.DEFAULT
    private val pack = yaml.node("pack")?.let { node ->
        PackConfig.from { node.node(it).getBoolean(false) }
    } ?: PackConfig.DEFAULT
    private val sightTrace = yaml.node("sight-trace").getBoolean(true)
    private val mergeWithExternalResources = yaml.node("merge-with-external-resources").getBoolean(false)
    private val itemModel = yaml.node("item").getString("leather_horse_armor")
    private val item = runCatching {
        BuiltInRegistries.ITEM.getValue(
            Identifier.withDefaultNamespace(itemModel)
        )
    }.getOrDefault(Items.LEATHER_HORSE_ARMOR).let {
        Supplier { ItemStack(it).wrap() }
    }
    private val itemNamespace = yaml.node("item-namespace").getString("bm_models").toPackName()
    private val maxSight by lazy {
        yaml.node("max-sight").getDouble(-1.0).run {
            if (this <= 0.0) EntityUtil.renderDistance() else this
        }
    }

    private val minSight = yaml.node("min-sight").getDouble(5.0)
    private val namespace = yaml.node("namespace").getString("bettermodel")
    private val packType = yaml.node("pack-type").getString("zip")?.let {
        runCatching {
            BetterModelConfig.PackType.valueOf(it.uppercase())
        }.getOrNull()
    } ?: BetterModelConfig.PackType.ZIP
    private val buildFolderLocation = (yaml.node("build-folder-location").getString("BetterModel/build")).replace('/', File.separatorChar)
    private val followMobInvisibility = yaml.node("follow-mob-invisibility").getBoolean(true)
    private val versionCheck = yaml.node("version-check").getBoolean(true)
    private val defaultMountController = when (yaml.node("default-mount-controller").getString("walk")?.lowercase()) {
        "invalid" -> MountControllers.INVALID
        "none" -> MountControllers.NONE
        "fly" -> MountControllers.FLY
        else -> MountControllers.WALK
    }
    private val lerpFrameTime = yaml.node("lerp-frame-time").getInt(5)
    private val cancelPlayerModelInventory = yaml.node("cancel-player-model-inventory").getBoolean(false)
    private val playerHideDelay = yaml.node("player-hide-delay").getLong(3L).coerceAtLeast(1L)
    private val packetBundlingSize = yaml.node("packet-bundling-size").getInt(16)
    private val enableStrictLoading = yaml.node("enable-strict-loading").getBoolean(false)

    override fun debug(): DebugConfig = debug
    override fun indicator(): IndicatorConfig = indicator
    override fun module(): ModuleConfig = module
    override fun pack(): PackConfig = pack
    override fun item(): Supplier<PlatformItemStack> = item
    override fun itemModel(): String = itemModel
    override fun itemNamespace(): String = itemNamespace
    override fun metrics(): Boolean = false
    override fun sightTrace(): Boolean = sightTrace
    override fun mergeWithExternalResources(): Boolean = mergeWithExternalResources
    override fun maxSight(): Double = maxSight
    override fun minSight(): Double = minSight
    override fun namespace(): String = namespace
    override fun packType(): BetterModelConfig.PackType = packType
    override fun buildFolderLocation(): String = buildFolderLocation
    override fun followMobInvisibility(): Boolean = followMobInvisibility
    override fun usePurpurAfk(): Boolean = false
    override fun versionCheck(): Boolean = versionCheck
    override fun defaultMountController(): MountController = defaultMountController
    override fun lerpFrameTime(): Int = lerpFrameTime
    override fun cancelPlayerModelInventory(): Boolean = cancelPlayerModelInventory
    override fun playerHideDelay(): Long = playerHideDelay
    override fun packetBundlingSize(): Int = packetBundlingSize
    override fun enableStrictLoading(): Boolean = enableStrictLoading
}

