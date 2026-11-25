/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
@file:Suppress("UnstableApiUsage")

package kr.toxicity.model.impl.fabric.attachment

import com.mojang.serialization.Codec
import kr.toxicity.model.impl.fabric.modId
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.minecraft.resources.Identifier

object BetterModelAttachments {
    @JvmField
    val MODEL_DATA: AttachmentType<String> = AttachmentRegistry.create(
        Identifier.fromNamespaceAndPath(modId(), "model_data")
    ) { builder ->
        builder
            .persistent(Codec.STRING)
            .copyOnDeath()
    }

    internal fun init() = Unit
}
