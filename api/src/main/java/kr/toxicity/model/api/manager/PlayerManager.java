package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.nms.PlayerChannelHandler;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.UUID;

/**
 * Player manager
 */
public interface PlayerManager extends GlobalManager {
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

    /**
     * Play's animation to this player
     * @param player player
     * @param model model name
     * @param animation animation name
     */
    default void animate(@NotNull Player player, @NotNull String model, @NotNull String animation) {
        animate(player, model, animation, null);
    }

    /**
     * Play's animation to this player with a specific loop type.
     * @param player player
     * @param model model name
     * @param animation animation name
     * @param loopType the loop type to use, or null to use the animation's default.
     */
    void animate(@NotNull Player player, @NotNull String model, @NotNull String animation, @Nullable AnimationIterator.Type loopType);


    /**
     * Gets all renderers for player animation.
     * @return renderers
     */
    @NotNull @Unmodifiable
    List<ModelRenderer> limbs();

    /**
     * Gets renderer for player animation by name.
     * @param name renderer's name
     * @return renderer or null
     */
    @Nullable ModelRenderer limb(@NotNull String name);
}