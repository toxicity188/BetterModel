package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.util.TransformedItemStack;
import kr.toxicity.model.api.util.function.BonePredicate;
import org.bukkit.entity.Display;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Tracker update action
 */
public sealed interface TrackerUpdateAction extends BiPredicate<RenderedBone, BonePredicate>  {

    @Override
    boolean test(@NotNull RenderedBone b, @NotNull BonePredicate p);

    /**
     * Creates brightness data
     * @param block block brightness
     * @param sky sky brightness
     * @return brightness data
     */
    static @NotNull Brightness brightness(int block, int sky) {
        return new Brightness(block, sky);
    }

    /**
     * Creates glow data
     * @param glow should be applying a glow
     * @param glowColor glow color
     * @return glow data
     */
    static @NotNull Glow glow(boolean glow, int glowColor) {
        return new Glow(glow, glowColor);
    }

    /**
     * Creates tint data
     * @param rgb rgb
     * @return tint data
     */
    static @NotNull Tint tint(int rgb) {
        return new Tint(rgb);
    }

    /**
     * Gets enchant data
     * @param enchant should be enchanted
     * @return enchant data
     */
    static @NotNull Enchant enchant(boolean enchant) {
        return enchant ? Enchant.TRUE : Enchant.FALSE;
    }

    /**
     * Gets toggle part data
     * @param toggle should be visible
     * @return toggle part data
     */
    static @NotNull TogglePart togglePart(boolean toggle) {
        return toggle ? TogglePart.TRUE : TogglePart.FALSE;
    }

    /**
     * Creates item stack data
     * @param itemStack item stack
     * @return item stack data
     */
    static @NotNull ItemStack itemStack(@NotNull TransformedItemStack itemStack) {
        Objects.requireNonNull(itemStack);
        return new ItemStack(itemStack);
    }

    /**
     * Creates billboard data
     * @param billboard billboard
     * @return billboard data
     */
    static @NotNull Billboard billboard(@NotNull Display.Billboard billboard) {
        Objects.requireNonNull(billboard);
        return new Billboard(billboard);
    }

    /**
     * Brightness
     * @param block block brightness
     * @param sky sky brightness
     */
    record Brightness(int block, int sky) implements TrackerUpdateAction {
        @Override
        public boolean test(@NotNull RenderedBone b, @NotNull BonePredicate p) {
            return b.brightness(p, block, sky);
        }
    }

    /**
     * Glow
     * @param glow should be applying a glow
     * @param glowColor glow color
     */
    record Glow(boolean glow, int glowColor) implements TrackerUpdateAction {
        @Override
        public boolean test(@NotNull RenderedBone b, @NotNull BonePredicate p) {
            return b.glow(p, glow, glowColor);
        }
    }

    /**
     * Enchant
     */
    enum Enchant implements TrackerUpdateAction {
        /**
         * True
         */
        TRUE,
        /**
         * False
         */
        FALSE
        ;

        @Override
        public boolean test(@NotNull RenderedBone b, @NotNull BonePredicate p) {
            return b.enchant(p, this == TRUE);
        }
    }

    /**
     * Tint
     * @param rgb rgb
     */
    record Tint(int rgb) implements TrackerUpdateAction {
        @Override
        public boolean test(@NotNull RenderedBone b, @NotNull BonePredicate p) {
            return b.tint(p, rgb);
        }
    }

    /**
     * Toggle part
     */
    enum TogglePart implements TrackerUpdateAction {
        /**
         * True
         */
        TRUE,
        /**
         * False
         */
        FALSE
        ;

        @Override
        public boolean test(@NotNull RenderedBone b, @NotNull BonePredicate p) {
            return b.togglePart(p, this == TRUE);
        }
    }

    /**
     * Item stack
     * @param itemStack item stack
     */
    record ItemStack(@NotNull TransformedItemStack itemStack) implements TrackerUpdateAction {
        @Override
        public boolean test(@NotNull RenderedBone b, @NotNull BonePredicate p) {
            return b.itemStack(p, itemStack);
        }
    }

    /**
     * Billboard
     * @param billboard billboard
     */
    record Billboard(@NotNull Display.Billboard billboard) implements TrackerUpdateAction {
        @Override
        public boolean test(@NotNull RenderedBone b, @NotNull BonePredicate p) {
            return b.billboard(p, billboard);
        }
    }
}
