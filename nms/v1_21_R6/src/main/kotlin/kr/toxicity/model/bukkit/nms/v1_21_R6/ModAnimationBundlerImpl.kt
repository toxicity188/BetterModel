/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.nms.v1_21_R6

import kr.toxicity.model.api.nms.ModAnimationBundler
import kr.toxicity.model.api.platform.PlatformPlayer
import kr.toxicity.model.api.util.MathUtil
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
import net.minecraft.server.MinecraftServer
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.joml.Quaternionf
import org.joml.Vector3f

internal class ModAnimationBundlerImpl(initialCapacity: Int) : ModAnimationBundler {

    companion object {

        const val KEY = "modelengine:bulk_data"

        const val PACKET_TYPE_BULK_DATA = 0x00

        const val FIELD_TRANSLATION = 1 shl 0
        const val FIELD_LEFT_ROTATION = 1 shl 1
        const val FIELD_SCALE = 1 shl 2
        const val FIELD_TRANSFORM_DURATION = 1 shl 4

        @Suppress("UNCHECKED_CAST")
        private val createPayload = (ClientboundCustomPayloadPacket::class.java.fields[0].apply {
            isAccessible = true
        }.get(null) as StreamCodec<RegistryFriendlyByteBuf, ClientboundCustomPayloadPacket>).let {
            { buf: RegistryFriendlyByteBuf -> it.decode(buf) }
        }
        private val EMPTY_BUILD_TASK: (FriendlyByteBuf) -> Unit = {}
    }

    private val packet by lazy {
        useByteBuf { buffer ->
            createPayload(
                RegistryFriendlyByteBuf(
                    buffer,
                    MinecraftServer.getServer().registryAccess()
                ).apply {
                    writeUtf(KEY)
                    useByteBuf {
                        it.writeByte(PACKET_TYPE_BULK_DATA)
                        it.writeVarInt(builderList.size)
                        builderList.forEach { builder -> builder(it) }
                        writeBytes(it)
                    }
                }
            )
        }
    }

    private val builderList = ArrayList<(FriendlyByteBuf) -> Unit>(initialCapacity)

    override fun send(player: PlatformPlayer) {
        (player.unwarp() as CraftPlayer).handle.connection.send(packet)
    }

    fun append(id: Int, scope: Appender.() -> Unit) {
        val build = Appender(id).apply(scope).build()
        if (build !== EMPTY_BUILD_TASK) builderList += build
    }

    class Appender(
        val entityId: Int,
    ) {
        private var mask = 0
        private var buildTask = EMPTY_BUILD_TASK
        private val isEmpty get() = buildTask === EMPTY_BUILD_TASK

        fun appendPosition(vector: Vector3f) {
            mask = mask or FIELD_TRANSLATION
            task {
                writeFloat(it, vector.x)
                writeFloat(it, vector.y)
                writeFloat(it, vector.z)
            }
        }

        fun appendScale(vector: Vector3f) {
            mask = mask or FIELD_SCALE
            task {
                writeFloat(it, vector.x)
                writeFloat(it, vector.y)
                writeFloat(it, vector.z)
            }
        }

        fun appendRotation(quaternion: Quaternionf) {
            mask = mask or FIELD_LEFT_ROTATION
            task {
                writeFloat(it, quaternion.x)
                writeFloat(it, quaternion.y)
                writeFloat(it, quaternion.z)
                writeFloat(it, quaternion.w)
            }
        }

        fun appendDuration(duration: Int) {
            mask = mask or FIELD_TRANSFORM_DURATION
            task {
                writeVarInt(it, duration)
            }
        }

        fun build(): (FriendlyByteBuf) -> Unit {
            if (isEmpty) return EMPTY_BUILD_TASK
            val m = mask
            val t = buildTask
            return {
                writeVarInt(it,entityId)
                writeByte(it, m)
                t(it)
            }
        }

        private fun task(task: (FriendlyByteBuf) -> Unit) {
            if (isEmpty) {
                buildTask = task
                return
            }
            val last = buildTask
            buildTask = {
                last(it)
                task(it)
            }
        }

        private fun writeFloat(buf: FriendlyByteBuf, float: Float) {
            buf.writeShort(MathUtil.floatToHalf(float).toInt())
        }

        private fun writeVarInt(buf: FriendlyByteBuf, duration: Int) {
            buf.writeVarInt(duration)
        }

        private fun writeByte(buf: FriendlyByteBuf, duration: Int) {
            buf.writeByte(duration)
        }
    }
}
