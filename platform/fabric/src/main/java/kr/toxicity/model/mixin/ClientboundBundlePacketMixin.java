/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.mixin;

import kr.toxicity.model.impl.fabric.network.BetterModelBundlePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = ClientboundBundlePacket.class)
public abstract class ClientboundBundlePacketMixin implements BetterModelBundlePacket {
    @Unique
    private boolean bettermodel$isBetterModelPacket;

    @Override
    public boolean bettermodel$isBetterModelPacket() {
        return bettermodel$isBetterModelPacket;
    }

    @Override
    public void bettermodel$setBetterModelPacket(boolean isBetterModel) {
        bettermodel$isBetterModelPacket = isBetterModel;
    }
}
