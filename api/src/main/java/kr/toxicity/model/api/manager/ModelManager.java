package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Set;

/**
 * Model Manager
 */
public interface ModelManager {

    /**
     * Gets renderer by name
     * @param name name
     * @return renderer or null
     */
    @Nullable ModelRenderer model(@NotNull String name);


    /**
     * @deprecated Use ModelManager#model instead.
     * @param name name
     * @return renderer or null
     */
    @Deprecated
    @Nullable
    default ModelRenderer renderer(@NotNull String name) {
        return model(name);
    }

    /**
     * Gets all renderers
     * @return all renderers
     */
    @NotNull @Unmodifiable
    Collection<ModelRenderer> models();

    /**
     * Gets all key of renderer
     * @return keys
     */
    @NotNull @Unmodifiable
    Set<String> modelKeys();

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
    Set<String> limbKeys();


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
}
