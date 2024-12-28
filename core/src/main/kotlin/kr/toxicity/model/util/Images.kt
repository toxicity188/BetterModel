package kr.toxicity.model.util

import java.awt.image.RenderedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

fun RenderedImage.save(file: File) {
    ImageIO.write(this, "png", file)
}

fun RenderedImage.toByteArray(): ByteArray {
    val byte = ByteArrayOutputStream()
    byte.buffered().use { buffer ->
        ImageIO.write(this, "png", buffer)
    }
    return byte.toByteArray()
}
