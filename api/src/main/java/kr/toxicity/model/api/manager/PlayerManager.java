package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.nms.PlayerChannelHandler;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Set;
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
     * @return whether to success
     */
    default boolean animate(@NotNull Player player, @NotNull String model, @NotNull String animation) {
        return animate(player, model, animation, AnimationModifier.DEFAULT_WITH_PLAY_ONCE);
    }

    /**
     * Play's animation to this player with a specific loop type.
     * @param player player
     * @param model model name
     * @param animation animation name
     * @param modifier modifier
     * @return whether to success
     */
    boolean animate(@NotNull Player player, @NotNull String model, @NotNull String animation, @NotNull AnimationModifier modifier);


    /**
     * Gets all renderers for player animation.
     * @return renderers
     */
    @NotNull @Unmodifiable
    Collection<ModelRenderer> limbs();

    /**
     * Gets renderer for player animation by name.
     * @param name renderer's name
     * @return renderer or null
     */
    @Nullable ModelRenderer limb(@NotNull String name);

    /**
     * Gets all key of renderer
     * @return keys
     */
    @NotNull @Unmodifiable
    Set<String> keys();
}