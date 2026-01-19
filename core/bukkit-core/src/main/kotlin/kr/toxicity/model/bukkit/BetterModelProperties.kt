/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.bukkit

import com.vdurmont.semver4j.Semver
import kr.toxicity.model.BetterModelEvaluatorImpl
import kr.toxicity.model.BetterModelEventBusImpl
import kr.toxicity.model.api.BetterModelConfig
import kr.toxicity.model.api.BetterModelPlatform.ReloadResult
import kr.toxicity.model.api.bukkit.BetterModelBukkit
import kr.toxicity.model.api.event.PluginEndReloadEvent
import kr.toxicity.model.api.event.PluginStartReloadEvent
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.api.version.MinecraftVersion.*
import kr.toxicity.model.bukkit.configuration.PluginConfiguration
import kr.toxicity.model.bukkit.manager.CompatibilityManager
import kr.toxicity.model.bukkit.manager.EntityManager
import kr.toxicity.model.bukkit.manager.PlayerManagerImpl
import kr.toxicity.model.bukkit.scheduler.BukkitScheduler
import kr.toxicity.model.bukkit.scheduler.PaperScheduler
import kr.toxicity.model.manager.*
import kr.toxicity.model.util.callEvent
import kr.toxicity.model.util.handleException
import kr.toxicity.model.util.toComponent
import kr.toxicity.model.util.warn
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit

private typealias Latest = kr.toxicity.model.nms.v1_21_R7.NMSImpl

internal class BetterModelProperties(
    private val plugin: AbstractBetterModelPlugin
) {
    private lateinit var _config: BetterModelConfig
    private var _metrics: Metrics? = null

    val version = parse(Bukkit.getBukkitVersion().substringBefore('-'))
    val nms = when (version) {
        V1_21_11 -> Latest()
        V1_21_9, V1_21_10 -> kr.toxicity.model.nms.v1_21_R6.NMSImpl()
        V1_21_6, V1_21_7, V1_21_8 -> kr.toxicity.model.nms.v1_21_R5.NMSImpl()
        V1_21_5 -> kr.toxicity.model.nms.v1_21_R4.NMSImpl()
        V1_21_4 -> kr.toxicity.model.nms.v1_21_R3.NMSImpl()
        V1_21, V1_21_1 -> kr.toxicity.model.nms.v1_21_R1.NMSImpl()
        else if BetterModelBukkit.IS_PAPER -> {
            warn(
                "Note: this version is officially untested.".toComponent(),
                "So be careful to use!".toComponent()
            )
            Latest()
        }
        else -> throw RuntimeException("Unsupported version: $version")
    }
    val scheduler = if (BetterModelBukkit.IS_FOLIA) PaperScheduler() else BukkitScheduler()
    val evaluator = BetterModelEvaluatorImpl()
    val eventbus = BetterModelEventBusImpl()
    @Suppress("DEPRECATION") //To support Spigot :(
    val semver = Semver(plugin.description.version, Semver.SemverType.LOOSE)
    val snapshot = runCatching {
        plugin.attributes().getValue("Dev-Build").toInt()
    }.getOrElse {
        it.handleException("Unable to parse manifest's build data")
        -1
    }
    var config
        get() = _config
        set(value) {
            _config = value.apply {
                if (metrics()) {
                    if (_metrics == null) _metrics = Metrics(plugin, 24237)
                } else {
                    _metrics?.shutdown()
                    _metrics = null
                }
            }
        }
    val managers by lazy {
        listOf(
            CompatibilityManager,
            ArmorManager,
            ProfileManagerImpl,
            SkinManagerImpl,
            ModelManagerImpl,
            PlayerManagerImpl,
            EntityManager,
            ScriptManagerImpl
        )
    }

    var reloadStartTask: (PackZipper) -> Unit = { callEvent { PluginStartReloadEvent(it) } }
    var reloadEndTask: (ReloadResult) -> Unit = { callEvent { PluginEndReloadEvent(it) } }

    init {
        config = BetterModelConfigImpl(PluginConfiguration.CONFIG.create())
    }
}
