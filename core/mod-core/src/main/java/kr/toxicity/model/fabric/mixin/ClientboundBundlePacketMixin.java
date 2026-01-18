package kr.toxicity.model.fabric.mixin;

import kr.toxicity.model.fabric.network.BetterModelBundlePacket;
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
