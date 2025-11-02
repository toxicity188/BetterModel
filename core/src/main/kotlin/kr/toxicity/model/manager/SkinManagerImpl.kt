/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.manager

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import it.unimi.dsi.fastutil.ints.IntList
import kr.toxicity.library.armormodel.ArmorResource
import kr.toxicity.library.dynamicuv.*
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.armor.ArmorItem
import kr.toxicity.model.api.event.CreatePlayerSkinEvent
import kr.toxicity.model.api.event.RemovePlayerSkinEvent
import kr.toxicity.model.api.manager.SkinManager
import kr.toxicity.model.api.nms.Profiled
import kr.toxicity.model.api.pack.PackObfuscator
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.api.player.PlayerLimb
import kr.toxicity.model.api.player.PlayerSkinProvider
import kr.toxicity.model.api.skin.AuthLibAdapter
import kr.toxicity.model.api.skin.SkinData
import kr.toxicity.model.api.skin.SkinProfile
import kr.toxicity.model.api.tracker.TrackerUpdateAction
import kr.toxicity.model.api.util.TransformedItemStack
import kr.toxicity.model.api.version.MinecraftVersion
import kr.toxicity.model.authlib.V6AuthLibAdapter
import kr.toxicity.model.authlib.V7AuthLibAdapter
import kr.toxicity.model.player.HttpPlayerSkinProvider
import kr.toxicity.model.util.*
import org.bukkit.Bukkit
import org.joml.Vector3f
import java.awt.image.BufferedImage
import java.net.URI
import java.net.http.HttpResponse
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

object SkinManagerImpl : SkinManager, GlobalManager {

    private const val DIV_FACTOR = 16F / 0.9375F

    private var uvNamespace = UVNamespace(
        CONFIG.namespace(),
        "player_limb"
    )

    private val HEAD = UVModel(
        { uvNamespace },
        "head"
    ).addElement(
        UVElement(
            ElementVector(8F, 8F, 8F).div(DIV_FACTOR),
            ElementVector(0f, 4F, 0f).div(DIV_FACTOR),
            UVSpace(8, 8, 8),
            UVElement.ColorType.RGB,
            mapOf(
                UVFace.NORTH to UVPos(8, 8),
                UVFace.SOUTH to UVPos(24, 8),
                UVFace.EAST to UVPos(0, 8),
                UVFace.WEST to UVPos(16, 8),
                UVFace.UP to UVPos(8, 0),
                UVFace.DOWN to UVPos(16, 0)
            )
        )
    ).addElement(
        UVElement(
            ElementVector(8F, 8F, 8F).div(DIV_FACTOR).inflate(0.5f),
            ElementVector(0f, 4F, 0f).div(DIV_FACTOR),
            UVSpace(8, 8, 8),
            UVElement.ColorType.ARGB,
            mapOf(
                UVFace.NORTH to UVPos(8 + 32, 8),
                UVFace.SOUTH to UVPos(24 + 32, 8),
                UVFace.EAST to UVPos(32, 8),
                UVFace.WEST to UVPos(16 + 32, 8),
                UVFace.UP to UVPos(8 + 32, 0),
                UVFace.DOWN to UVPos(16 + 32, 0)
            )
        )
    )
    private val CHEST = UVModel(
        { uvNamespace },
        "chest"
    ).addElement(
        UVElement(
            ElementVector(8f, 4f, 4f).div(DIV_FACTOR),
            ElementVector(0f, 2f, 0f).div(DIV_FACTOR),
            UVSpace(8, 4, 4),
            UVElement.ColorType.RGB,
            mapOf(
                UVFace.NORTH to UVPos(20, 20),
                UVFace.SOUTH to UVPos(32, 20),
                UVFace.EAST to UVPos(16, 20),
                UVFace.WEST to UVPos(28, 20),
                UVFace.UP to UVPos(20, 16)
            )
        )
    ).addElement(
        UVElement(
            ElementVector(8f, 4f, 4f).div(DIV_FACTOR).inflate(0.25f),
            ElementVector(0f, 2f, 0f).div(DIV_FACTOR),
            UVSpace(8, 4, 4),
            UVElement.ColorType.ARGB,
            mapOf(
                UVFace.NORTH to UVPos(20, 20 + 16),
                UVFace.SOUTH to UVPos(32, 20 + 16),
                UVFace.EAST to UVPos(16, 20 + 16),
                UVFace.WEST to UVPos(28, 20 + 16),
                UVFace.UP to UVPos(20, 16 + 16)
            )
        )
    )
    private val WAIST = UVModel(
        { uvNamespace },
        "waist"
    ).addElement(
        UVElement(
            ElementVector(8f, 4f, 4f).div(DIV_FACTOR),
            ElementVector(0f, 2f, 0f).div(DIV_FACTOR),
            UVSpace(8, 4, 4),
            UVElement.ColorType.RGB,
            mapOf(
                UVFace.NORTH to UVPos(20, 24),
                UVFace.SOUTH to UVPos(32, 24),
                UVFace.EAST to UVPos(16, 24),
                UVFace.WEST to UVPos(28, 24)
            )
        )
    ).addElement(
        UVElement(
            ElementVector(8f, 4f, 4f).div(DIV_FACTOR).inflate(0.25f),
            ElementVector(0f, 2f, 0f).div(DIV_FACTOR),
            UVSpace(8, 4, 4),
            UVElement.ColorType.ARGB,
            mapOf(
                UVFace.NORTH to UVPos(20, 24 + 16),
                UVFace.SOUTH to UVPos(32, 24 + 16),
                UVFace.EAST to UVPos(16, 24 + 16),
                UVFace.WEST to UVPos(28, 24 + 16)
            )
        )
    )
    private val HIP = UVModel(
        { uvNamespace },
        "hip"
    ).addElement(
        UVElement(
            ElementVector(8f, 4f, 4f).div(DIV_FACTOR),
            ElementVector(0f, 2f, 0f).div(DIV_FACTOR),
            UVSpace(8, 4, 4),
            UVElement.ColorType.RGB,
            mapOf(
                UVFace.NORTH to UVPos(20, 28),
                UVFace.SOUTH to UVPos(32, 28),
                UVFace.EAST to UVPos(16, 28),
                UVFace.WEST to UVPos(28, 28),
                UVFace.DOWN to UVPos(28, 16)
            )
        )
    ).addElement(
        UVElement(
            ElementVector(8f, 4f, 4f).div(DIV_FACTOR).inflate(0.25f),
            ElementVector(0f, 2f, 0f).div(DIV_FACTOR),
            UVSpace(8, 4, 4),
            UVElement.ColorType.ARGB,
            mapOf(
                UVFace.NORTH to UVPos(20, 28 + 16),
                UVFace.SOUTH to UVPos(32, 28 + 16),
                UVFace.EAST to UVPos(16, 28 + 16),
                UVFace.WEST to UVPos(28, 28 + 16),
                UVFace.DOWN to UVPos(28, 16 + 16)
            )
        )
    )
    private val LEFT_LEG = UVModel(
        { uvNamespace },
        "left_leg"
    ).addElement(
        UVElement(
            ElementVector(4f, 6f, 4f).div(DIV_FACTOR),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(4, 6, 4),
            UVElement.ColorType.RGB,
            mapOf(
                UVFace.NORTH to UVPos(20, 52),
                UVFace.SOUTH to UVPos(28, 52),
                UVFace.EAST to UVPos(16, 52),
                UVFace.WEST to UVPos(24, 52),
                UVFace.UP to UVPos(20, 48)
            )
        )
    ).addElement(
        UVElement(
            ElementVector(4f, 6f, 4f).div(DIV_FACTOR).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(4, 6, 4),
            UVElement.ColorType.ARGB,
            mapOf(
                UVFace.NORTH to UVPos(20 - 16, 52),
                UVFace.SOUTH to UVPos(28 - 16, 52),
                UVFace.EAST to UVPos(0, 52),
                UVFace.WEST to UVPos(24 - 16, 52),
                UVFace.UP to UVPos(20 - 16, 48)
            )
        )
    )
    private val LEFT_FORELEG = UVModel(
        { uvNamespace },
        "left_foreleg"
    ).addElement(
        UVElement(
            ElementVector(4f, 6f, 4f).div(DIV_FACTOR),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(4, 6, 4),
            UVElement.ColorType.RGB,
            mapOf(
                UVFace.NORTH to UVPos(20, 58),
                UVFace.SOUTH to UVPos(28, 58),
                UVFace.EAST to UVPos(16, 58),
                UVFace.WEST to UVPos(24, 58),
                UVFace.DOWN to UVPos(24, 48)
            )
        )
    ).addElement(
        UVElement(
            ElementVector(4f, 6f, 4f).div(DIV_FACTOR).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(4, 6, 4),
            UVElement.ColorType.ARGB,
            mapOf(
                UVFace.NORTH to UVPos(20 - 16, 58),
                UVFace.SOUTH to UVPos(28 - 16, 58),
                UVFace.EAST to UVPos(0, 58),
                UVFace.WEST to UVPos(24 - 16, 58),
                UVFace.DOWN to UVPos(24 - 16, 48)
            )
        )
    )
    private val RIGHT_LEG = UVModel(
        { uvNamespace },
        "right_leg"
    ).addElement(
        UVElement(
            ElementVector(4f, 6f, 4f).div(DIV_FACTOR),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(4, 6, 4),
            UVElement.ColorType.RGB,
            mapOf(
                UVFace.NORTH to UVPos(4, 20),
                UVFace.SOUTH to UVPos(12, 20),
                UVFace.EAST to UVPos(0, 20),
                UVFace.WEST to UVPos(8, 20),
                UVFace.UP to UVPos(4, 16)
            )
        )
    ).addElement(
        UVElement(
            ElementVector(4f, 6f, 4f).div(DIV_FACTOR).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(4, 6, 4),
            UVElement.ColorType.ARGB,
            mapOf(
                UVFace.NORTH to UVPos(4, 20 + 16),
                UVFace.SOUTH to UVPos(12, 20 + 16),
                UVFace.EAST to UVPos(0, 20 + 16),
                UVFace.WEST to UVPos(8, 20 + 16),
                UVFace.UP to UVPos(4, 16 + 16)
            )
        )
    )
    private val RIGHT_FORELEG = UVModel(
        { uvNamespace },
        "right_foreleg"
    ).addElement(
        UVElement(
            ElementVector(4f, 6f, 4f).div(DIV_FACTOR),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(4, 6, 4),
            UVElement.ColorType.RGB,
            mapOf(
                UVFace.NORTH to UVPos(4, 26),
                UVFace.SOUTH to UVPos(12, 26),
                UVFace.EAST to UVPos(0, 26),
                UVFace.WEST to UVPos(8, 26),
                UVFace.DOWN to UVPos(8, 16)
            )
        )
    ).addElement(
        UVElement(
            ElementVector(4f, 6f, 4f).div(DIV_FACTOR).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(4, 6, 4),
            UVElement.ColorType.ARGB,
            mapOf(
                UVFace.NORTH to UVPos(4, 26 + 16),
                UVFace.SOUTH to UVPos(12, 26 + 16),
                UVFace.EAST to UVPos(0, 26 + 16),
                UVFace.WEST to UVPos(8, 26 + 16),
                UVFace.DOWN to UVPos(8, 16 + 16)
            )
        )
    )
    private val LEFT_ARM = UVModel(
        { uvNamespace },
        "left_arm"
    ).addElement(
        UVElement(
            ElementVector(4f, 6f, 4f).div(DIV_FACTOR),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(4, 6, 4),
            UVElement.ColorType.RGB,
            mapOf(
                UVFace.NORTH to UVPos(36, 52),
                UVFace.SOUTH to UVPos(44, 52),
                UVFace.EAST to UVPos(32, 52),
                UVFace.WEST to UVPos(40, 52),
                UVFace.UP to UVPos(36, 48)
            )
        )
    ).addElement(
        UVElement(
            ElementVector(4f, 6f, 4f).div(DIV_FACTOR).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(4, 6, 4),
            UVElement.ColorType.ARGB,
            mapOf(
                UVFace.NORTH to UVPos(36 + 16, 52),
                UVFace.SOUTH to UVPos(44 + 16, 52),
                UVFace.EAST to UVPos(32 + 16, 52),
                UVFace.WEST to UVPos(40 + 16, 52),
                UVFace.UP to UVPos(36 + 16, 48)
            )
        )
    )
    private val LEFT_FOREARM = UVModel(
        { uvNamespace },
        "left_forearm"
    ).addElement(
        UVElement(
            ElementVector(4f, 6f, 4f).div(DIV_FACTOR),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(4, 6, 4),
            UVElement.ColorType.RGB,
            mapOf(
                UVFace.NORTH to UVPos(36, 58),
                UVFace.SOUTH to UVPos(44, 58),
                UVFace.EAST to UVPos(32, 58),
                UVFace.WEST to UVPos(40, 58),
                UVFace.DOWN to UVPos(40, 48)
            )
        )
    ).addElement(
        UVElement(
            ElementVector(4f, 6f, 4f).div(DIV_FACTOR).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(4, 6, 4),
            UVElement.ColorType.ARGB,
            mapOf(
                UVFace.NORTH to UVPos(36 + 16, 58),
                UVFace.SOUTH to UVPos(44 + 16, 58),
                UVFace.EAST to UVPos(32 + 16, 58),
                UVFace.WEST to UVPos(40 + 16, 58),
                UVFace.DOWN to UVPos(40 + 16, 48)
            )
        )
    )
    private val RIGHT_ARM = UVModel(
        { uvNamespace },
        "right_arm"
    ).addElement(
        UVElement(
            ElementVector(4f, 6f, 4f).div(DIV_FACTOR),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(4, 6, 4),
            UVElement.ColorType.RGB,
            mapOf(
                UVFace.NORTH to UVPos(44, 20),
                UVFace.SOUTH to UVPos(52, 20),
                UVFace.EAST to UVPos(40, 20),
                UVFace.WEST to UVPos(48, 20),
                UVFace.UP to UVPos(44, 16)
            )
        )
    ).addElement(
        UVElement(
            ElementVector(4f, 6f, 4f).div(DIV_FACTOR).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(4, 6, 4),
            UVElement.ColorType.ARGB,
            mapOf(
                UVFace.NORTH to UVPos(44, 20 + 16),
                UVFace.SOUTH to UVPos(52, 20 + 16),
                UVFace.EAST to UVPos(40, 20 + 16),
                UVFace.WEST to UVPos(48, 20 + 16),
                UVFace.UP to UVPos(44, 16 + 16)
            )
        )
    )
    private val RIGHT_FOREARM = UVModel(
        { uvNamespace },
        "right_forearm"
    ).addElement(
        UVElement(
            ElementVector(4f, 6f, 4f).div(DIV_FACTOR),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(4, 6, 4),
            UVElement.ColorType.RGB,
            mapOf(
                UVFace.NORTH to UVPos(44, 24),
                UVFace.SOUTH to UVPos(52, 24),
                UVFace.EAST to UVPos(40, 24),
                UVFace.WEST to UVPos(48, 24),
                UVFace.DOWN to UVPos(48, 16)
            )
        )
    ).addElement(
        UVElement(
            ElementVector(4f, 6f, 4f).div(DIV_FACTOR).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(4, 6, 4),
            UVElement.ColorType.ARGB,
            mapOf(
                UVFace.NORTH to UVPos(44, 24 + 16),
                UVFace.SOUTH to UVPos(52, 24 + 16),
                UVFace.EAST to UVPos(40, 24 + 16),
                UVFace.WEST to UVPos(48, 24 + 16),
                UVFace.DOWN to UVPos(48, 16 + 16)
            )
        )
    )
    private val SLIM_LEFT_ARM = UVModel(
        { uvNamespace },
        "left_slim_arm"
    ).addElement(
        UVElement(
            ElementVector(3f, 6f, 4f).div(DIV_FACTOR),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(3, 6, 4),
            UVElement.ColorType.RGB,
            mapOf(
                UVFace.NORTH to UVPos(36, 52),
                UVFace.SOUTH to UVPos(43, 52),
                UVFace.EAST to UVPos(32, 52),
                UVFace.WEST to UVPos(39, 52),
                UVFace.UP to UVPos(36, 48)
            )
        )
    ).addElement(
        UVElement(
            ElementVector(3f, 6f, 4f).div(DIV_FACTOR).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(3, 6, 4),
            UVElement.ColorType.ARGB,
            mapOf(
                UVFace.NORTH to UVPos(36 + 16, 52),
                UVFace.SOUTH to UVPos(43 + 16, 52),
                UVFace.EAST to UVPos(32 + 16, 52),
                UVFace.WEST to UVPos(39 + 16, 52),
                UVFace.UP to UVPos(36 + 16, 48)
            )
        )
    )
    private val SLIM_LEFT_FOREARM = UVModel(
        { uvNamespace },
        "left_slim_forearm"
    ).addElement(
        UVElement(
            ElementVector(3f, 6f, 4f).div(DIV_FACTOR),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(3, 6, 4),
            UVElement.ColorType.RGB,
            mapOf(
                UVFace.NORTH to UVPos(36, 58),
                UVFace.SOUTH to UVPos(43, 58),
                UVFace.EAST to UVPos(32, 58),
                UVFace.WEST to UVPos(39, 58),
                UVFace.DOWN to UVPos(39, 48)
            )
        )
    ).addElement(
        UVElement(
            ElementVector(3f, 6f, 4f).div(DIV_FACTOR).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(3, 6, 4),
            UVElement.ColorType.ARGB,
            mapOf(
                UVFace.NORTH to UVPos(36 + 16, 58),
                UVFace.SOUTH to UVPos(43 + 16, 58),
                UVFace.EAST to UVPos(32 + 16, 58),
                UVFace.WEST to UVPos(39 + 16, 58),
                UVFace.DOWN to UVPos(39 + 16, 48)
            )
        )
    )
    private val SLIM_RIGHT_ARM = UVModel(
        { uvNamespace },
        "right_slim_arm"
    ).addElement(
        UVElement(
            ElementVector(3f, 6f, 4f).div(DIV_FACTOR),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(3, 6, 4),
            UVElement.ColorType.RGB,
            mapOf(
                UVFace.NORTH to UVPos(44, 20),
                UVFace.SOUTH to UVPos(51, 20),
                UVFace.EAST to UVPos(40, 20),
                UVFace.WEST to UVPos(47, 20),
                UVFace.UP to UVPos(44, 16)
            )
        )
    ).addElement(
        UVElement(
            ElementVector(3f, 6f, 4f).div(DIV_FACTOR).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(3, 6, 4),
            UVElement.ColorType.ARGB,
            mapOf(
                UVFace.NORTH to UVPos(44, 20 + 16),
                UVFace.SOUTH to UVPos(51, 20 + 16),
                UVFace.EAST to UVPos(40, 20 + 16),
                UVFace.WEST to UVPos(47, 20 + 16),
                UVFace.UP to UVPos(44, 16 + 16)
            )
        )
    )
    private val SLIM_RIGHT_FOREARM = UVModel(
        { uvNamespace },
        "right_slim_forearm"
    ).addElement(
        UVElement(
            ElementVector(3f, 6f, 4f).div(DIV_FACTOR),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(3, 6, 4),
            UVElement.ColorType.RGB,
            mapOf(
                UVFace.NORTH to UVPos(44, 24),
                UVFace.SOUTH to UVPos(51, 24),
                UVFace.EAST to UVPos(40, 24),
                UVFace.WEST to UVPos(47, 24),
                UVFace.DOWN to UVPos(47, 16)
            )
        )
    ).addElement(
        UVElement(
            ElementVector(3f, 6f, 4f).div(DIV_FACTOR).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(DIV_FACTOR),
            UVSpace(3, 6, 4),
            UVElement.ColorType.ARGB,
            mapOf(
                UVFace.NORTH to UVPos(44, 24 + 16),
                UVFace.SOUTH to UVPos(51, 24 + 16),
                UVFace.EAST to UVPos(40, 24 + 16),
                UVFace.WEST to UVPos(47, 24 + 16),
                UVFace.DOWN to UVPos(47, 16 + 16)
            )
        )
    )
    private val CAPE = UVModel(
        { uvNamespace },
        "cape"
    ).addElement(
        UVElement(
            ElementVector(10f, 16f, 1f).div(DIV_FACTOR),
            ElementVector(0f, -8f, 0.5f).div(DIV_FACTOR),
            UVSpace(10, 16, 1),
            UVElement.ColorType.RGB,
            mapOf(
                UVFace.NORTH to UVPos(12, 1),
                UVFace.SOUTH to UVPos(1, 1),
                UVFace.EAST to UVPos(11, 1),
                UVFace.WEST to UVPos(0, 1),
                UVFace.UP to UVPos(1, 0),
                UVFace.DOWN to UVPos(11, 0)
            )
        )
    )

    private data class SkinModelData(
        val namespace: String,
        val data: UVModelData
    )

    private fun UVModel.asModelData(image: BufferedImage): SkinModelData {
        val data = write(image)
        return SkinModelData(
            itemModelNamespace(),
            data
        )
    }

    private val whiteList = IntList.of(*(0..8).map { 0xFFFFFF }.toIntArray())

    private fun SkinModelData.asItem(colors: IntList = IntList.of()): TransformedItemStack = PLUGIN.nms().createSkinItem(
        namespace,
        data.flags,
        emptyList(),
        colors + data.colors
    )

    private fun SkinModelData.asItem(resource: ArmorResource, item: ArmorItem? = null): TransformedItemStack {
        if (item == null) return asItem(whiteList)
        val armorData = ArmorManager.armor.resource(resource).run {
            item.trim()?.let {
                customModelData(item.type, item.tint, it, item.palette?.let { p -> ArmorManager.armor.colors()[p] })
            } ?: customModelData(item.type, item.tint)
        }
        return PLUGIN.nms().createSkinItem(
            namespace,
            data.flags,
            armorData.strings,
            armorData.colors + data.colors
        )
    }

    fun write(block: (UVByteBuilder) -> Unit) {
        val itemObf = PackObfuscator.order()
        val modelObf = PackObfuscator.order()
        fun UVModel.write(armorResource: ArmorResource? = null) {
            val model = modelName()
            packName(itemObf.obfuscate(model))
            asJson("one_pixel", UVLoadContext(
                { modelObf.obfuscate("${model}_$it") },
                { indexer, _, array ->
                    armorResource?.let { ArmorManager.armor.resource(it) }?.let {
                        array += it.toJson()
                        indexer.shiftColor(9)
                    }
                }
            )).forEach {
                block(it)
            }
        }
        HEAD.write(ArmorResource.HELMET)
        CHEST.write(ArmorResource.CHEST)
        WAIST.write(ArmorResource.WAIST)
        HIP.write(ArmorResource.HIP)
        LEFT_LEG.write(ArmorResource.LEFT_LEG)
        LEFT_FORELEG.write(ArmorResource.LEFT_FORELEG)
        RIGHT_LEG.write(ArmorResource.RIGHT_LEG)
        RIGHT_FORELEG.write(ArmorResource.RIGHT_FORELEG)
        LEFT_ARM.write(ArmorResource.LEFT_ARM)
        LEFT_FOREARM.write()
        RIGHT_ARM.write(ArmorResource.RIGHT_ARM)
        RIGHT_FOREARM.write()
        SLIM_LEFT_ARM.write(ArmorResource.LEFT_ARM)
        SLIM_LEFT_FOREARM.write()
        SLIM_RIGHT_ARM.write(ArmorResource.RIGHT_ARM)
        SLIM_RIGHT_FOREARM.write()
        CAPE.write()

        block(UVByteBuilder.emptyImage(uvNamespace, "one_pixel"))
    }

    private val gson = Gson()

    private val profileCache = Caffeine.newBuilder()
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .removalListener<UUID, SkinDataImpl> { key, value, cause ->
            if (cause == RemovalCause.EXPIRED && key != null && value != null) {
                handleExpiration(key, value)
            }
        }
        .build<UUID, SkinDataImpl>()

    private val fallback by lazy {
        PLUGIN.getResource("fallback_skin.png")!!.use {
            SkinDataImpl(false, ImageIO.read(it), null)
        }
    }

    private val authlib = if (PLUGIN.version().useV7AuthLib()) V7AuthLibAdapter() else V6AuthLibAdapter()

    private var skinProvider = if (PLUGIN.nms().isProxyOnlineMode) PlayerSkinProvider.DEFAULT else HttpPlayerSkinProvider()

    override fun supported(): Boolean = PLUGIN.version() >= MinecraftVersion.V1_21_4

    private fun handleExpiration(key: UUID, skin: SkinDataImpl) {
        skin.original?.let {
            if (!RemovePlayerSkinEvent(it).call() || it.playerEquals()) profileCache.put(key, skin)
        }
    }

    private fun SkinProfile.playerEquals() = Bukkit.getPlayer(id)?.let { player ->
        authlib.adapt(PLUGIN.nms().profile(player))
    } == this

    override fun authlib(): AuthLibAdapter = authlib

    override fun isSlim(profile: SkinProfile): Boolean {
        val encodedValue = profile.textures
        return runCatching {
            encodedValue.isNotEmpty() && JsonParser.parseString(String(Base64.getDecoder().decode(encodedValue.first().value)))
                .asJsonObject
                .getAsJsonObject("textures")
                .getAsJsonObject("SKIN")
                .get("metadata")?.asJsonObject
                ?.get("model")?.asString == "slim"
        }.getOrDefault(false)
    }

    private data class Skin(
        val textures: SkinTextures
    )

    private data class SkinTextures(
        @SerializedName("SKIN") val skin: SkinUrl,
        @SerializedName("CAPE") val cape: SkinUrl?
    )

    private data class SkinUrl(
        val url: String
    ) {
        fun toURI(): URI = URI.create(url)
    }

    override fun getOrRequest(profile: SkinProfile): SkinData {
        return profileCache.get(profile.id) { id ->
            skinProvider.provide(profile).thenApply { provided ->
                CreatePlayerSkinEvent(provided ?: profile).run {
                    call()
                    skinProfile
                }
            }.thenComposeAsync { selected ->
                httpClient {
                    gson.fromJson(
                        String(Base64.getDecoder().decode(selected.textures.first().value)),
                        Skin::class.java
                    ).textures.run {
                        fun SkinUrl.toFuture() = sendAsync(
                            buildHttpRequest {
                                uri(toURI())
                                GET()
                            },
                            HttpResponse.BodyHandlers.ofInputStream()
                        ).thenComposeAsync { request ->
                            CompletableFuture.supplyAsync { request.body().use { ImageIO.read(it) } }
                        }
                        skin.toFuture().thenCombine(cape?.toFuture() ?: CompletableFuture.completedFuture(null)) { skin, cape ->
                            profileCache.put(id, SkinDataImpl(
                                isSlim(selected),
                                skin.convertLegacy(),
                                cape,
                                selected
                            ))
                            BetterModel.registryOrNull(id)?.trackers()?.forEach { tracker ->
                                tracker.update(TrackerUpdateAction.itemMapping()) { bone ->
                                    bone.itemMapper is PlayerLimb.LimbItemMapper
                                }
                            }
                        }
                    }
                }.orElse {
                    it.handleException("Unable to read this profile: ${selected.name}")
                    CompletableFuture.completedFuture(null)
                }
            }.exceptionally {
                it.handleException("unable to read this skin: ${profile.name}")
                profileCache.invalidate(id)
                null
            }
            fallback
        }
    }

    override fun removeCache(profile: SkinProfile) = profileCache.invalidate(profile.id)

    override fun setSkinProvider(provider: PlayerSkinProvider) {
        skinProvider = provider
    }

    private fun BufferedImage.convertLegacy() = if (height == 64) this else BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB).also { newImage ->
        fun drawTo(from: UVPos, to: UVPos, xr: IntRange, zr: IntRange) {
            val maxX = xr.last + xr.first
            for (x in xr) {
                for (z in zr) {
                    newImage.setRGB(
                        to.x + maxX - x,
                        to.z + z,
                        getRGB(from.x + x, from.z + z)
                    )
                }
            }
        }
        fun drawTo(from: UVPos, to: UVPos) {
            drawTo(from, to, 0..<4, 4..<16)
            drawTo(from, to, 4..<8, 0..<16)
            drawTo(from, to, 8..<12, 0..<16)
            drawTo(from, to, 12..<16, 4..<16)
        }
        newImage.createGraphics().let {
            it.drawImage(this, 0, 0, null)
            it.dispose()
        }
        drawTo(UVPos(0, 16), UVPos(16, 48))
        drawTo(UVPos(40, 16), UVPos(32, 48))
    }

    private class SkinDataImpl(
        private val isSlim: Boolean,
        private val skinImage: BufferedImage,
        private val capeImage: BufferedImage?,
        val original: SkinProfile? = null
    ) : SkinData {

        private val head = HEAD.asModelData(skinImage)
        private val hip = HIP.asModelData(skinImage)
        private val waist = WAIST.asModelData(skinImage)
        private val chest = CHEST.asModelData(skinImage)
        private val leftArm = (if (isSlim) SLIM_LEFT_ARM else LEFT_ARM).asModelData(skinImage)
        private val rightArm = (if (isSlim) SLIM_RIGHT_ARM else RIGHT_ARM).asModelData(skinImage)
        private val leftLeg = LEFT_LEG.asModelData(skinImage)
        private val leftForeLeg = LEFT_FORELEG.asModelData(skinImage)
        private val rightLeg = RIGHT_LEG.asModelData(skinImage)
        private val rightForeLeg = RIGHT_FORELEG.asModelData(skinImage)
        private val leftForeArm = (if (isSlim) SLIM_LEFT_FOREARM else LEFT_FOREARM).asModelData(skinImage).asItem()
        private val rightForeArm = (if (isSlim) SLIM_RIGHT_FOREARM else RIGHT_FOREARM).asModelData(skinImage).asItem()
        private val cape = capeImage?.let { CAPE.asModelData(it).asItem() }

        override fun head(profiled: Profiled): TransformedItemStack = head.asItem(ArmorResource.HELMET, profiled.armors().helmet())
        override fun hip(profiled: Profiled): TransformedItemStack = hip.asItem(ArmorResource.HIP, profiled.armors().leggings())
        override fun waist(profiled: Profiled): TransformedItemStack = waist.asItem(ArmorResource.WAIST, profiled.armors().chestplate())
        override fun chest(profiled: Profiled): TransformedItemStack = chest.asItem(ArmorResource.CHEST, profiled.armors().chestplate())
        override fun leftArm(profiled: Profiled): TransformedItemStack = leftArm.asItem(ArmorResource.LEFT_ARM, profiled.armors().chestplate())
        override fun rightArm(profiled: Profiled): TransformedItemStack = rightArm.asItem(ArmorResource.RIGHT_ARM, profiled.armors().chestplate())
        override fun leftLeg(profiled: Profiled): TransformedItemStack = leftLeg.asItem(ArmorResource.LEFT_LEG, profiled.armors().leggings())
        override fun rightLeg(profiled: Profiled): TransformedItemStack = rightLeg.asItem(ArmorResource.RIGHT_LEG, profiled.armors().leggings())
        override fun leftForeLeg(profiled: Profiled): TransformedItemStack = leftForeLeg.asItem(ArmorResource.LEFT_FORELEG, profiled.armors().boots())
        override fun rightForeLeg(profiled: Profiled): TransformedItemStack = rightForeLeg.asItem(ArmorResource.RIGHT_FORELEG, profiled.armors().boots())
        override fun leftForeArm(): TransformedItemStack = leftForeArm
        override fun rightForeArm(): TransformedItemStack = rightForeArm
        override fun cape(profiled: Profiled): TransformedItemStack? = if (profiled.armors().chestplate() != null) cape?.offset(Vector3f(0F, 0F, -1F / 16F)) else cape

        fun refresh() = SkinDataImpl(isSlim, skinImage, capeImage, original)
    }

    override fun reload(pipeline: ReloadPipeline, zipper: PackZipper) {
        uvNamespace = UVNamespace(
            CONFIG.namespace(),
            "player_limb"
        )
        if (!CONFIG.module().playerAnimation) return
        if (supported()) write { resource ->
            zipper.modern().add(resource.path(), resource.estimatedSize()) {
                resource.build()
            }
        } else PLUGIN.loadAssets(pipeline, "pack") { s, i ->
            val read = i.readAllBytes()
            zipper.legacy().add(s) {
                read
            }
        }
        profileCache.asMap().entries.forEach {
            it.setValue(it.value.refresh())
        }
    }
}