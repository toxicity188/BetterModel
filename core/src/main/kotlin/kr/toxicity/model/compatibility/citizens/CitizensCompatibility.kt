/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.compatibility.citizens

import kr.toxicity.model.compatibility.Compatibility
import kr.toxicity.model.compatibility.citizens.command.AnimateCommand
import kr.toxicity.model.compatibility.citizens.command.LimbCommand
import kr.toxicity.model.compatibility.citizens.command.ModelCommand
import kr.toxicity.model.compatibility.citizens.trait.ModelTrait
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.trait.TraitInfo

class CitizensCompatibility : Compatibility {
    override fun start() {
        CitizensAPI.getTraitFactory()
            .registerTrait(TraitInfo.create(ModelTrait::class.java))
        CitizensAPI.getCommandManager().run {
            register(ModelCommand::class.java)
            register(AnimateCommand::class.java)
            register(LimbCommand::class.java)
        }
    }
}