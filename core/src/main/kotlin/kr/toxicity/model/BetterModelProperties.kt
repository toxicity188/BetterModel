/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model

import com.vdurmont.semver4j.Semver
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.BetterModelConfig
import kr.toxicity.model.api.BetterModelPlugin.ReloadResult
import kr.toxicity.model.api.event.PluginEndReloadEvent
import kr.toxicity.model.api.event.PluginStartReloadEvent
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.api.version.MinecraftVersion.*
import kr.toxicity.model.configuration.PluginConfiguration
import kr.toxicity.model.manager.*
import kr.toxicity.model.scheduler.BukkitScheduler
import kr.toxicity.model.scheduler.PaperScheduler
import kr.toxicity.model.util.call
import kr.toxicity.model.util.handleException
import kr.toxicity.model.util.toComponent
import kr.toxicity.model.util.warn
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit

private typealias Latest = kr.toxicity.model.nms.v1_21_R6.NMSImpl

internal class BetterModelProperties(
    private val plugin: AbstractBetterModelPlugin
) {
    private lateinit var _config: BetterModelConfig
    private var _metrics: Metrics? = null

    val version = parse(Bukkit.getBukkitVersion().substringBefore('-'))
    val nms = when (version) {
        V1_21_9, V1_21_10 -> Latest()
        V1_21_6, V1_21_7, V1_21_8 -> kr.toxicity.model.nms.v1_21_R5.NMSImpl()
        V1_21_5 -> kr.toxicity.model.nms.v1_21_R4.NMSImpl()
        V1_21_4 -> kr.toxicity.model.nms.v1_21_R3.NMSImpl()
        V1_21_2, V1_21_3 -> kr.toxicity.model.nms.v1_21_R2.NMSImpl()
        V1_21, V1_21_1 -> kr.toxicity.model.nms.v1_21_R1.NMSImpl()
        V1_20_5, V1_20_6 -> kr.toxicity.model.nms.v1_20_R4.NMSImpl()
        else if BetterModel.IS_PAPER -> {
            warn(
                "Note: this version is officially untested.".toComponent(),
                "So be careful to use!".toComponent()
            )
            Latest()
        }
        else -> throw RuntimeException("Unsupported version: $version")
    }
    val scheduler = if (BetterModel.IS_FOLIA) PaperScheduler() else BukkitScheduler()
    val evaluator = BetterModelEvaluatorImpl()
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
            SkinManagerImpl,
            ModelManagerImpl,
            PlayerManagerImpl,
            EntityManager,
            ScriptManagerImpl,
            CommandManager
        )
    }

    var reloadStartTask: (PackZipper) -> Unit = { PluginStartReloadEvent(it).call() }
    var reloadEndTask: (ReloadResult) -> Unit = { PluginEndReloadEvent(it).call() }

    init {
        config = BetterModelConfigImpl(PluginConfiguration.CONFIG.create())
    }
}