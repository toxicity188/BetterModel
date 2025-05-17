package kr.toxicity.model.manager

import com.google.gson.JsonParser
import com.mojang.authlib.GameProfile
import kr.toxicity.library.dynamicuv.*
import kr.toxicity.model.api.manager.ReloadInfo
import kr.toxicity.model.api.manager.SkinManager
import kr.toxicity.model.api.skin.SkinData
import kr.toxicity.model.api.util.HttpUtil
import kr.toxicity.model.api.util.TransformedItemStack
import kr.toxicity.model.api.version.MinecraftVersion
import kr.toxicity.model.util.PLUGIN
import kr.toxicity.model.util.handleException
import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap
import java.awt.image.BufferedImage
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

object SkinManagerImpl : SkinManager, GlobalManagerImpl {

    private var uvNamespace = UVNamespace(
        ConfigManagerImpl.namespace(),
        "player_limb"
    )

    private val HEAD = UVModel(
        { uvNamespace },
        "head"
    ).addElement(
        UVElement(
            ElementVector(0.5f, 0.5f, 0.5f),
            ElementVector(0f, 0.25f, 0f),
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
            ElementVector(0.5f, 0.5f, 0.5f).inflate(0.5f),
            ElementVector(0f, 0.25f, 0f),
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
            ElementVector(8f, 4f, 4f).div(16f),
            ElementVector(0f, 2f, 0f).div(16f),
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
            ElementVector(8f, 4f, 4f).div(16f).inflate(0.25f),
            ElementVector(0f, 2f, 0f).div(16f),
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
            ElementVector(8f, 4f, 4f).div(16f),
            ElementVector(0f, 2f, 0f).div(16f),
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
            ElementVector(8f, 4f, 4f).div(16f).inflate(0.25f),
            ElementVector(0f, 2f, 0f).div(16f),
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
            ElementVector(8f, 4f, 4f).div(16f),
            ElementVector(0f, 2f, 0f).div(16f),
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
            ElementVector(8f, 4f, 4f).div(16f).inflate(0.25f),
            ElementVector(0f, 2f, 0f).div(16f),
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
            ElementVector(4f, 6f, 4f).div(16f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(4f, 6f, 4f).div(16f).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(4f, 6f, 4f).div(16f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(4f, 6f, 4f).div(16f).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(4f, 6f, 4f).div(16f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(4f, 6f, 4f).div(16f).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(4f, 6f, 4f).div(16f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(4f, 6f, 4f).div(16f).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(4f, 6f, 4f).div(16f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(4f, 6f, 4f).div(16f).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(4f, 6f, 4f).div(16f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(4f, 6f, 4f).div(16f).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(4f, 6f, 4f).div(16f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(4f, 6f, 4f).div(16f).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(4f, 6f, 4f).div(16f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(4f, 6f, 4f).div(16f).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(3f, 6f, 4f).div(16f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(3f, 6f, 4f).div(16f).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(3f, 6f, 4f).div(16f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(3f, 6f, 4f).div(16f).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(3f, 6f, 4f).div(16f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(3f, 6f, 4f).div(16f).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(3f, 6f, 4f).div(16f),
            ElementVector(0f, -3f, 0f).div(16f),
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
            ElementVector(3f, 6f, 4f).div(16f).inflate(0.25f),
            ElementVector(0f, -3f, 0f).div(16f),
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

    private fun UVModel.asItem(image: BufferedImage): TransformedItemStack {
        val data = write(image)
        return PLUGIN.nms().createSkinItem(
            itemModelNamespace(),
            data.flags,
            data.colors
        )
    }

    fun write(block: (String, () -> ByteArray) -> Unit) {
        if (!supported()) return
        fun UVModel.write(block: (String, () -> ByteArray) -> Unit) {
            asJson("one_pixel").forEach {
                block(it.path()) {
                    it.build()
                }
            }
        }
        HEAD.write(block)
        CHEST.write(block)
        WAIST.write(block)
        HIP.write(block)
        LEFT_LEG.write(block)
        LEFT_FORELEG.write(block)
        RIGHT_LEG.write(block)
        RIGHT_FORELEG.write(block)
        LEFT_ARM.write(block)
        LEFT_FOREARM.write(block)
        RIGHT_ARM.write(block)
        RIGHT_FOREARM.write(block)
        SLIM_LEFT_ARM.write(block)
        SLIM_LEFT_FOREARM.write(block)
        SLIM_RIGHT_ARM.write(block)
        SLIM_RIGHT_FOREARM.write(block)
        UVByteBuilder.emptyImage(uvNamespace, "one_pixel").run {
            block(path()) {
                build()
            }
        }
    }

    private val profileMap = ExpiringMap.builder()
        .expirationPolicy(ExpirationPolicy.ACCESSED)
        .expiration(5, TimeUnit.MINUTES)
        .build<UUID, SkinData>()
    private val fallback = PLUGIN.getResource("fallback_skin.png")!!.buffered().use {
        SkinDataImpl(false, ImageIO.read(it))
    }

    override fun supported(): Boolean = PLUGIN.version() >= MinecraftVersion.V1_21_4

    override fun getOrCreate(profile: GameProfile): SkinData {
        val id = profile.id
        return profileMap.computeIfAbsent(id) {
            HttpUtil.client { client ->
                client.sendAsync(HttpRequest.newBuilder()
                    .uri(
                        URI.create(
                            JsonParser.parseString(String(Base64.getDecoder().decode(profile.properties["textures"].first().value)))
                                .asJsonObject
                                .getAsJsonObject("textures")
                                .getAsJsonObject("SKIN")
                                .getAsJsonPrimitive("url")
                                .asString
                    ))
                    .GET()
                    .build(),
                    HttpResponse.BodyHandlers.ofInputStream()
                )
            }.orElse {
                it.handleException("Unable to read this profile: ${profile.name}")
                CompletableFuture.completedFuture(null)
            }.thenAccept {
                it.body().use { stream ->
                    profileMap[id] = SkinDataImpl(
                        PLUGIN.nms().isSlim(profile),
                        ImageIO.read(stream)
                    )
                }
            }
            fallback
        }
    }

    private class SkinDataImpl(
        isSlim: Boolean,
        image: BufferedImage
    ) : SkinData {

        private val head = HEAD.asItem(image)
        private val hip = HIP.asItem(image)
        private val waist = WAIST.asItem(image)
        private val chest = CHEST.asItem(image)
        private val leftArm = (if (isSlim) SLIM_LEFT_ARM else LEFT_ARM).asItem(image)
        private val leftForeArm = (if (isSlim) SLIM_LEFT_FOREARM else LEFT_FOREARM).asItem(image)
        private val rightArm = (if (isSlim) SLIM_RIGHT_ARM else RIGHT_ARM).asItem(image)
        private val rightForeArm = (if (isSlim) SLIM_RIGHT_FOREARM else RIGHT_FOREARM).asItem(image)
        private val leftLeg = LEFT_LEG.asItem(image)
        private val leftForeLeg = LEFT_FORELEG.asItem(image)
        private val rightLeg = RIGHT_LEG.asItem(image)
        private val rightForeLeg = RIGHT_FORELEG.asItem(image)

        override fun head(): TransformedItemStack = head
        override fun hip(): TransformedItemStack = hip
        override fun waist(): TransformedItemStack = waist
        override fun chest(): TransformedItemStack = chest
        override fun leftArm(): TransformedItemStack = leftArm
        override fun leftForeArm(): TransformedItemStack = leftForeArm
        override fun rightArm(): TransformedItemStack = rightArm
        override fun rightForeArm(): TransformedItemStack = rightForeArm
        override fun leftLeg(): TransformedItemStack = leftLeg
        override fun leftForeLeg(): TransformedItemStack = leftForeLeg
        override fun rightLeg(): TransformedItemStack = rightLeg
        override fun rightForeLeg(): TransformedItemStack = rightForeLeg
    }

    override fun reload(info: ReloadInfo) {
        uvNamespace = UVNamespace(
            ConfigManagerImpl.namespace(),
            "player_limb"
        )
    }
}