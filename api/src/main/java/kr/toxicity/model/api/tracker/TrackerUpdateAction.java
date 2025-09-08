package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.util.TransformedItemStack;
import kr.toxicity.model.api.util.function.BonePredicate;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Display;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

/**
 * Tracker update action
 */
public sealed interface TrackerUpdateAction extends BiPredicate<RenderedBone, BonePredicate> {

    /**
     * Creates brightness action
     * @param block block brightness
     * @param sky sky brightness
     * @return brightness action
     */
    static @NotNull Brightness brightness(int block, int sky) {
        return new Brightness(block, sky);
    }

    /**
     * Creates glow action
     * @param glow should be applying a glow
     * @return glow action
     */
    static @NotNull Glow glow(boolean glow) {
        return glow ? Glow.TRUE : Glow.FALSE;
    }

    /**
     * Creates glow color action
     * @param glowColor glow color
     * @return glow color action
     */
    static @NotNull GlowColor glowColor(int glowColor) {
        return new GlowColor(glowColor);
    }

    /**
     * Creates view range action
     * @param viewRange view range
     * @return view range action
     */
    static @NotNull ViewRange viewRange(float viewRange) {
        return new ViewRange(viewRange);
    }

    /**
     * Creates tint action
     * @param rgb rgb
     * @return tint action
     */
    static @NotNull Tint tint(int rgb) {
        return new Tint(rgb);
    }

    /**
     * Gets enchant action
     * @param enchant should be enchanted
     * @return enchant action
     */
    static @NotNull Enchant enchant(boolean enchant) {
        return enchant ? Enchant.TRUE : Enchant.FALSE;
    }

    /**
     * Gets toggle part action
     * @param toggle should be visible
     * @return toggle part action
     */
    static @NotNull TogglePart togglePart(boolean toggle) {
        return toggle ? TogglePart.TRUE : TogglePart.FALSE;
    }

    /**
     * Creates item stack action
     * @param itemStack item stack
     * @return item stack action
     */
    static @NotNull ItemStack itemStack(@NotNull TransformedItemStack itemStack) {
        Objects.requireNonNull(itemStack);
        return new ItemStack(itemStack);
    }

    /**
     * Creates billboard action
     * @param billboard billboard
     * @return billboard action
     */
    static @NotNull Billboard billboard(@NotNull Display.Billboard billboard) {
        Objects.requireNonNull(billboard);
        return new Billboard(billboard);
    }

    /**
     * Gets item mapping action
     * @return item mapping action
     */
    static @NotNull ItemMapping itemMapping() {
        return ItemMapping.INSTANCE;
    }

    /**
     * Gets composited action
     * @return composited action
     */
    static @NotNull Composite composite(@NotNull TrackerUpdateAction... actions) {
        return new Composite(Arrays.stream(actions).flatMap(TrackerUpdateAction::stream).toList());
    }

    @Override
    boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate);

    /**
     * Adds other actions to this update action
     * @param action action
     * @return merged action
     */
    default @NotNull TrackerUpdateAction then(@NotNull TrackerUpdateAction action) {
        return composite(this, action);
    }

    /**
     * Gets action stream
     * @return stream
     */
    default @NotNull Stream<TrackerUpdateAction> stream() {
        return this instanceof Composite(List<TrackerUpdateAction> actions) ? actions.stream().flatMap(TrackerUpdateAction::stream) : Stream.of(this);
    }

    /**
     * Brightness
     * @param block block brightness
     * @param sky sky brightness
     */
    record Brightness(int block, int sky) implements TrackerUpdateAction {
        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return bone.brightness(predicate, block, sky);
        }
    }

    /**
     * Glow
     */
    @RequiredArgsConstructor
    enum Glow implements TrackerUpdateAction {
        /**
         * True
         */
        TRUE(true),
        /**
         * False
         */
        FALSE(false)
        ;
        private final boolean value;

        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return bone.glow(predicate, value);
        }
    }

    /**
     * Glow color
     * @param glowColor glow color
     */
    record GlowColor(int glowColor) implements TrackerUpdateAction {
        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return bone.glowColor(predicate, glowColor);
        }
    }

    /**
     * View range
     * @param viewRange view range
     */
    record ViewRange(float viewRange) implements TrackerUpdateAction {
        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return bone.viewRange(predicate, viewRange);
        }
    }

    /**
     * Enchant
     */
    @RequiredArgsConstructor
    enum Enchant implements TrackerUpdateAction {
        /**
         * True
         */
        TRUE(true),
        /**
         * False
         */
        FALSE(false)
        ;
        private final boolean value;

        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return bone.enchant(predicate, value);
        }
    }

    /**
     * Tint
     * @param rgb rgb
     */
    record Tint(int rgb) implements TrackerUpdateAction {
        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return bone.tint(predicate, rgb);
        }
    }

    /**
     * Toggle part
     */
    @RequiredArgsConstructor
    enum TogglePart implements TrackerUpdateAction {
        /**
         * True
         */
        TRUE(true),
        /**
         * False
         */
        FALSE(false)
        ;
        private final boolean value;

        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return bone.togglePart(predicate, value);
        }
    }

    /**
     * Item stack
     * @param itemStack item stack
     */
    record ItemStack(@NotNull TransformedItemStack itemStack) implements TrackerUpdateAction {
        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return bone.itemStack(predicate, itemStack);
        }
    }

    /**
     * Billboard
     * @param billboard billboard
     */
    record Billboard(@NotNull Display.Billboard billboard) implements TrackerUpdateAction {
        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return bone.billboard(predicate, billboard);
        }
    }

    /**
     * Item mapping
     */
    enum ItemMapping implements TrackerUpdateAction {
        /**
         * Instance
         */
        INSTANCE
        ;

        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return bone.updateItem(predicate);
        }
    }

    /**
     * Composited action
     * @param actions actions
     */
    record Composite(@NotNull @Unmodifiable List<TrackerUpdateAction> actions) implements TrackerUpdateAction {
        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return actions.stream().anyMatch(action -> action.test(bone, predicate));
        }
    }
}
