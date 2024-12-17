package kr.toxicity.model.util

import java.awt.image.RenderedImage
import java.io.File
import javax.imageio.ImageIO

fun RenderedImage.save(file: File) {
    ImageIO.write(this, "png", file)
}