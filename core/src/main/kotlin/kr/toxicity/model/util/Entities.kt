package kr.toxicity.model.util

import kr.toxicity.model.api.nms.NMSVersion
import org.bukkit.NamespacedKey
import org.bukkit.Registry

val ATTRIBUTE_SCALE by lazy {
    Registry.ATTRIBUTE.get(NamespacedKey.minecraft(if (PLUGIN.nms().version() >= NMSVersion.V1_21_R2) "scale" else "generic.scale"))!!
}