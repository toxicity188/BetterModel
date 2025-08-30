package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.nms.PlayerChannelHandler;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Player manager
 */
public interface PlayerManager {
    /**
     * Gets player channel handler
     * @param uuid player's uuid
     * @return channel handler or null
     */
    @Nullable PlayerChannelHandler player(@NotNull UUID uuid);

    /**
     * Get or creates channel handler
     * Do not use this with fake player, instead use PlayerManager#player(UUID)
     * @param player player
     * @return channel handler
     */
    @NotNull PlayerChannelHandler player(@NotNull Player player);

    @Deprecated(forRemoval = true)
    @Nullable
    default ModelRenderer limb(@NotNull String name) {
        return BetterModel.limbOrNull(name);
    }

    @Deprecated(forRemoval = true)
    default boolean animate(@NotNull Player player, @NotNull String model, @NotNull String animation) {
        return BetterModel.plugin().modelManager().animate(player, model, animation);
    }

    @Deprecated(forRemoval = true)
    default boolean animate(@NotNull Player player, @NotNull String model, @NotNull String animation, @NotNull AnimationModifier modifier) {
        return BetterModel.plugin().modelManager().animate(player, model, animation, modifier);
    }
}