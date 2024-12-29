package kr.toxicity.model.compatibility.citizens

import kr.toxicity.model.compatibility.Compatibility
import kr.toxicity.model.compatibility.citizens.command.ModelCommand
import kr.toxicity.model.compatibility.citizens.trait.ModelTrait
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.trait.TraitInfo

class CitizensCompatibility : Compatibility {
    override fun start() {
        CitizensAPI.getTraitFactory()
            .registerTrait(TraitInfo.create(ModelTrait::class.java))
        CitizensAPI.getCommandManager()
            .register(ModelCommand::class.java)
    }
}