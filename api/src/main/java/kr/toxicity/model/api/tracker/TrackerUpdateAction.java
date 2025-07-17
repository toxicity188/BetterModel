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
 * @param <T> data type
 */
public interface TrackerUpdateAction<T extends TrackerUpdateAction.ActionData> {

    /**
     * Tint
     */
    TrackerUpdateAction<Tint> TINT = t -> (b, p) -> b.tint(p, t.rgb);
    /**
     * Brightness
     */
    TrackerUpdateAction<Brightness> BRIGHTNESS = t -> (b, p) -> b.brightness(p, t.block, t.sky);
    /**
     * Glow
     */
    TrackerUpdateAction<Glow> GLOW = t -> (b, p) -> b.glow(p, t.glow, t.glowColor);
    /**
     * Enchant
     */
    TrackerUpdateAction<Enchant> ENCHANT = t -> (b, p) -> b.enchant(p, t == Enchant.TRUE);
    /**
     * Toggle part
     */
    TrackerUpdateAction<TogglePart> TOGGLE_PART = t -> (b, p) -> b.togglePart(p, t == TogglePart.TRUE);
    /**
     * Item stack
     */
    TrackerUpdateAction<ItemStack> ITEM_STACK = t -> (b, p) -> b.itemStack(p, t.itemStack);
    /**
     * Billboard
     */
    TrackerUpdateAction<Billboard> BILLBOARD = t -> (b, p) -> b.billboard(p, t.billboard);

    /**
     * Creates condition by given data
     * @param t data
     * @return condition
     */
    @NotNull TrackerUpdateAction.TrackerActionCondition create(@NotNull T t);

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
     * Action data
     */
    sealed interface ActionData {
    }

    /**
     * Brightness
     * @param block block brightness
     * @param sky sky brightness
     */
    record Brightness(int block, int sky) implements ActionData {}

    /**
     * Glow
     * @param glow should be applying a glow
     * @param glowColor glow color
     */
    record Glow(boolean glow, int glowColor) implements ActionData {}

    /**
     * Enchant
     */
    enum Enchant implements ActionData {
        /**
         * True
         */
        TRUE,
        /**
         * False
         */
        FALSE
    }

    /**
     * Tint
     * @param rgb rgb
     */
    record Tint(int rgb) implements ActionData {}

    /**
     * Toggle part
     */
    enum TogglePart implements ActionData {
        /**
         * True
         */
        TRUE,
        /**
         * False
         */
        FALSE
    }

    /**
     * Item stack
     * @param itemStack item stack
     */
    record ItemStack(@NotNull TransformedItemStack itemStack) implements ActionData {}

    /**
     * Billboard
     * @param billboard billboard
     */
    record Billboard(@NotNull Display.Billboard billboard) implements ActionData {}

    /**
     * Tracker action condition
     */
    @FunctionalInterface
    interface TrackerActionCondition extends BiPredicate<RenderedBone, BonePredicate> {
    }
}
