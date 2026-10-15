/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.nms.v1_21_R6

import kr.toxicity.model.api.bukkit.BetterModelBukkit
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

val PLUGIN get() = BetterModelBukkit.platform() as Plugin

context(plugin: Plugin)
fun Entity.runInRegion(task: () -> Unit) {
    if (BetterModelBukkit.IS_FOLIA && !Bukkit.isOwnedByCurrentRegion(this)) {
        scheduler.run(plugin, { task() }) {}
    } else task()
}

context(plugin: Plugin)
fun Entity.hide(to: Player) {
    to.hideEntity(plugin, this)
}

context(plugin: Plugin)
fun Entity.show(to: Player) {
    to.showEntity(plugin, this)
}
