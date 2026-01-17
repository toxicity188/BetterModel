/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.fabric.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import kr.toxicity.model.api.BetterModelConfig
import kr.toxicity.model.api.config.DebugConfig
import kr.toxicity.model.api.config.IndicatorConfig
import kr.toxicity.model.api.config.ModuleConfig
import kr.toxicity.model.api.config.PackConfig
import kr.toxicity.model.api.fabric.platform.FabricItemStack
import kr.toxicity.model.api.mount.MountControllers
import kr.toxicity.model.api.platform.PlatformItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import java.util.function.Supplier

@JsonIgnoreProperties(ignoreUnknown = true)
data class BetterModelConfigImpl(
    @param:JsonProperty("debug")
    val debug: DebugConfig = DebugConfig.DEFAULT,

    @param:JsonProperty("indicator")
    val indicator: IndicatorConfig = IndicatorConfig.DEFAULT,

    @param:JsonProperty("module")
    val module: ModuleConfig = ModuleConfig.DEFAULT,

    @param:JsonProperty("pack")
    val pack: PackConfig = PackConfig.DEFAULT,

    @param:JsonProperty("metrics")
    val metrics: Boolean = true,

    @param:JsonProperty("sight-trace")
    val sightTrace: Boolean = true,

    @param:JsonProperty("merge-with-external-resources")
    val mergeWithExternalResources: Boolean = true,

    @param:JsonProperty("item")
    val itemModel: String = "LEATHER_HORSE_ARMOR",

    @param:JsonProperty("item-namespace")
    val itemNamespace: String = "bm_models",

    @param:JsonProperty("max-sight")
    val maxSight: Double = -1.0,

    @param:JsonProperty("min-sight")
    val minSight: Double = 5.0,

    @param:JsonProperty("namespace")
    val namespace: String = "bettermodel",

    @param:JsonProperty("pack-type")
    val packType: BetterModelConfig.PackType = BetterModelConfig.PackType.ZIP,

    @param:JsonProperty("build-folder-location")
    val buildFolderLocation: String = "BetterModel/build",

    @param:JsonProperty("follow-mob-invisibility")
    val followMobInvisibility: Boolean = true,

    @param:JsonProperty("use-purpur-afk")
    val usePurpurAfk: Boolean = true,

    @param:JsonProperty("version-check")
    val versionCheck: Boolean = true,

    @param:JsonProperty("default-mount-controller")
    val defaultMountController: MountControllers = MountControllers.WALK,

    @param:JsonProperty("lerp-frame-time")
    val lerpFrameTime: Int = 5,

    @param:JsonProperty("cancel-player-model-inventory")
    val cancelPlayerModelInventory: Boolean = false,

    @param:JsonProperty("player-hide-delay")
    val playerHideDelay: Long = 3L,

    @param:JsonProperty("packet-bundling-size")
    val packetBundlingSize: Int = 16,

    @param:JsonProperty("enable-strict-loading")
    val enableStrictLoading: Boolean = false
) :
    BetterModelConfig
{
    private val itemLike: Item by lazy {
        BuiltInRegistries.ITEM.getValue(
            Identifier.withDefaultNamespace(itemModel)
        )
    }

    private val itemSupplier: Supplier<PlatformItemStack> = {
        FabricItemStack(
            ItemStack(itemLike)
        )
    }

    override fun debug() = debug

    override fun indicator() = indicator

    override fun module() = module

    override fun pack() = pack

    override fun metrics() = metrics

    override fun sightTrace() = sightTrace

    override fun mergeWithExternalResources() = mergeWithExternalResources

    override fun item() = itemSupplier

    override fun itemModel() = itemModel

    override fun itemNamespace() = itemNamespace

    override fun maxSight() = maxSight

    override fun minSight() = minSight

    override fun namespace() = namespace

    override fun packType() = packType

    override fun buildFolderLocation() = buildFolderLocation

    override fun followMobInvisibility() = followMobInvisibility

    override fun usePurpurAfk() = usePurpurAfk

    override fun versionCheck() = versionCheck

    override fun defaultMountController() = defaultMountController

    override fun lerpFrameTime() = lerpFrameTime

    override fun cancelPlayerModelInventory() = cancelPlayerModelInventory

    override fun playerHideDelay() = playerHideDelay

    override fun packetBundlingSize() = packetBundlingSize

    override fun enableStrictLoading() = enableStrictLoading
}
