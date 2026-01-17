/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.fabric.scheduler

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents

object FabricModelSchedulerManager {
    private var scheduler: FabricModelSchedulerImpl? = null

    fun get(): FabricModelSchedulerImpl {
        return scheduler ?: throw IllegalStateException("Scheduler is not initialized")
    }

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register {
            scheduler?.tick()
        }

        ServerLifecycleEvents.SERVER_STARTING.register {
            scheduler = FabricModelSchedulerImpl()
        }

        ServerLifecycleEvents.SERVER_STOPPING.register {
            scheduler?.shutdown()
            scheduler = null
        }
    }
}
