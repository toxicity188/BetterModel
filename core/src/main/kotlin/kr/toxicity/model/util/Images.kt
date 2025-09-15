/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.util

import java.awt.image.RenderedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

fun RenderedImage.toByteArray(): ByteArray {
    return ByteArrayOutputStream().use { buffer ->
        ImageIO.write(this, "png", buffer)
        buffer.toByteArray()
    }
}
