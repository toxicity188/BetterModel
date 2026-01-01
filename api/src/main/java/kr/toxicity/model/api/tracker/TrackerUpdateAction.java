/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
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
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Represents an action that updates the state of a {@link RenderedBone}.
 * <p>
 * Actions can modify display properties like brightness, glow, item stack, and more.
 * They are applied to bones matching a specific predicate.
 * </p>
 *
 * @since 1.15.2
 */
public sealed interface TrackerUpdateAction extends BiPredicate<RenderedBone, BonePredicate> {

    /**
     * Creates an action to update display brightness.
     *
     * @param block the block light level
     * @param sky the skylight level
     * @return the action
     * @since 1.15.2
     */
    static @NotNull Brightness brightness(int block, int sky) {
        return new Brightness(block, sky);
    }

    /**
     * Creates an action to toggle the glowing effect.
     *
     * @param glow true to enable glow
     * @return the action
     * @since 1.15.2
     */
    static @NotNull Glow glow(boolean glow) {
        return glow ? Glow.TRUE : Glow.FALSE;
    }

    /**
     * Creates an action to set the glow color.
     *
     * @param glowColor the RGB glow color
     * @return the action
     * @since 1.15.2
     */
    static @NotNull GlowColor glowColor(int glowColor) {
        return new GlowColor(glowColor);
    }

    /**
     * Creates an action to set the view range.
     *
     * @param viewRange the view range
     * @return the action
     * @since 1.15.2
     */
    static @NotNull ViewRange viewRange(float viewRange) {
        return new ViewRange(viewRange);
    }

    /**
     * Creates an action to apply a tint color.
     *
     * @param rgb the RGB tint color
     * @return the action
     * @since 1.15.2
     */
    static @NotNull Tint tint(int rgb) {
        return new Tint(rgb);
    }

    /**
     * Creates an action to revert to the previous tint.
     *
     * @return the action
     * @since 1.15.2
     */
    static @NotNull PreviousTint previousTint() {
        return PreviousTint.INSTANCE;
    }

    /**
     * Creates an action to toggle the enchanted glint effect.
     *
     * @param enchant true to enable glint
     * @return the action
     * @since 1.15.2
     */
    static @NotNull Enchant enchant(boolean enchant) {
        return enchant ? Enchant.TRUE : Enchant.FALSE;
    }

    /**
     * Creates an action to toggle the visibility of a part.
     *
     * @param toggle true to show, false to hide
     * @return the action
     * @since 1.15.2
     */
    static @NotNull TogglePart togglePart(boolean toggle) {
        return toggle ? TogglePart.TRUE : TogglePart.FALSE;
    }

    /**
     * Creates an action to update the displayed item stack.
     *
     * @param itemStack the new item stack
     * @return the action
     * @since 1.15.2
     */
    static @NotNull ItemStack itemStack(@NotNull TransformedItemStack itemStack) {
        Objects.requireNonNull(itemStack);
        return new ItemStack(itemStack);
    }

    /**
     * Creates an action to set the billboard constraint.
     *
     * @param billboard the billboard type
     * @return the action
     * @since 1.15.2
     */
    static @NotNull Billboard billboard(@NotNull Display.Billboard billboard) {
        Objects.requireNonNull(billboard);
        return new Billboard(billboard);
    }

    /**
     * Creates an action to update the item mapping.
     *
     * @return the action
     * @since 1.15.2
     */
    static @NotNull ItemMapping itemMapping() {
        return ItemMapping.INSTANCE;
    }

    /**
     * Creates an action to set the movement interpolation duration.
     *
     * @param moveDuration the duration in ticks
     * @return the action
     * @since 1.15.2
     */
    static @NotNull MoveDuration moveDuration(int moveDuration) {
        return new MoveDuration(moveDuration);
    }

    /**
     * Combines multiple actions into a single composite action.
     *
     * @param actions the actions to combine
     * @return the composite action
     * @since 1.15.2
     */
    static @NotNull TrackerUpdateAction composite(@NotNull TrackerUpdateAction... actions) {
        return switch (actions.length) {
            case 0 -> none();
            case 1 -> actions[0];
            default -> new Composite(Arrays.stream(actions).flatMap(TrackerUpdateAction::stream).toList());
        };
    }

    /**
     * Creates an action that generates a specific action for each bone.
     *
     * @param builder the function to generate actions
     * @return the per-bone action
     * @since 1.15.2
     */
    static @NotNull PerBone perBone(@NotNull Function<RenderedBone, TrackerUpdateAction> builder) {
        return new PerBone(builder);
    }

    /**
     * Returns a no-op action.
     *
     * @return the none action
     * @since 1.15.2
     */
    static @NotNull None none() {
        return None.INSTANCE;
    }

    /**
     * Applies the action to a bone if it matches the predicate.
     *
     * @param bone the target bone
     * @param predicate the predicate to check against
     * @return true if the bone was updated
     * @since 1.15.2
     */
    @Override
    boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate);

    /**
     * Chains this action with another action.
     *
     * @param action the next action
     * @return the combined action
     * @since 1.15.2
     */
    default @NotNull TrackerUpdateAction then(@NotNull TrackerUpdateAction action) {
        return composite(this, action);
    }

    /**
     * Returns a stream of actions (useful for flattening composites).
     *
     * @return the stream
     * @since 1.15.2
     */
    default @NotNull Stream<TrackerUpdateAction> stream() {
        return Stream.of(this);
    }

    /**
     * Action to update brightness.
     * @param block the block light level
     * @param sky the skylight level
     * @since 1.15.2
     */
    record Brightness(int block, int sky) implements TrackerUpdateAction {
        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return bone.applyAtDisplay(predicate, display -> display.brightness(block, sky));
        }
    }

    /**
     * Action to update glow status.
     * @since 1.15.2
     */
    @RequiredArgsConstructor
    enum Glow implements TrackerUpdateAction {
        /**
         * Enable glow.
         * @since 1.15.2
         */
        TRUE(true),
        /**
         * Disable glow.
         * @since 1.15.2
         */
        FALSE(false)
        ;
        private final boolean value;

        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return bone.applyAtDisplay(predicate, display -> display.glow(value));
        }
    }

    /**
     * Action to update glow color.
     * @param glowColor the RGB glow color
     * @since 1.15.2
     */
    record GlowColor(int glowColor) implements TrackerUpdateAction {
        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return bone.applyAtDisplay(predicate, display -> display.glowColor(glowColor));
        }
    }

    /**
     * Action to update view range.
     * @param viewRange the view range
     * @since 1.15.2
     */
    record ViewRange(float viewRange) implements TrackerUpdateAction {
        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return bone.applyAtDisplay(predicate, display -> display.viewRange(viewRange));
        }
    }

    /**
     * Action to update enchantment glint.
     * @since 1.15.2
     */
    @RequiredArgsConstructor
    enum Enchant implements TrackerUpdateAction {
        /**
         * Enable glint.
         * @since 1.15.2
         */
        TRUE(true),
        /**
         * Disable glint.
         * @since 1.15.2
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
     * Action to apply a tint color.
     * @param rgb the RGB tint color
     * @since 1.15.2
     */
    record Tint(int rgb) implements TrackerUpdateAction {
        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return bone.tint(predicate, rgb);
        }
    }

    /**
     * Action to revert to previous tint.
     * @since 1.15.2
     */
    enum PreviousTint implements TrackerUpdateAction {
        /**
         * Instance.
         * @since 1.15.2
         */
        INSTANCE
        ;
        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return bone.tint(predicate);
        }
    }

    /**
     * Action to toggle part visibility.
     * @since 1.15.2
     */
    @RequiredArgsConstructor
    enum TogglePart implements TrackerUpdateAction {
        /**
         * Show part.
         * @since 1.15.2
         */
        TRUE(true),
        /**
         * Hide part.
         * @since 1.15.2
         */
        FALSE(false)
        ;
        private final boolean value;

        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return bone.applyAtDisplay(predicate, display -> display.invisible(!value));
        }
    }

    /**
     * Action to update the item stack.
     * @param itemStack the new item stack
     * @since 1.15.2
     */
    record ItemStack(@NotNull TransformedItemStack itemStack) implements TrackerUpdateAction {
        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return bone.itemStack(predicate, itemStack);
        }
    }

    /**
     * Action to update the billboard constraint.
     * @param billboard the billboard type
     * @since 1.15.2
     */
    record Billboard(@NotNull Display.Billboard billboard) implements TrackerUpdateAction {
        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return bone.applyAtDisplay(predicate, display -> display.billboard(billboard));
        }
    }

    /**
     * Action to update item mapping.
     * @since 1.15.2
     */
    enum ItemMapping implements TrackerUpdateAction {
        /**
         * Instance.
         * @since 1.15.2
         */
        INSTANCE
        ;

        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return bone.updateItem(predicate);
        }
    }

    /**
     * Action to update movement duration.
     * @param moveDuration the duration in ticks
     * @since 1.15.2
     */
    record MoveDuration(int moveDuration) implements TrackerUpdateAction {
        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return bone.applyAtDisplay(predicate, display -> display.moveDuration(moveDuration));
        }
    }

    /**
     * Composite action.
     * @param actions the list of actions
     * @since 1.15.2
     */
    record Composite(@NotNull @Unmodifiable List<TrackerUpdateAction> actions) implements TrackerUpdateAction {
        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            var result = false;
            for (TrackerUpdateAction action : actions) {
                if (action.test(bone, predicate)) result = true;
            }
            return result;
        }

        @Override
        public @NotNull Stream<TrackerUpdateAction> stream() {
            return actions.stream().flatMap(TrackerUpdateAction::stream);
        }
    }

    /**
     * Per-bone dynamic action.
     * @param builder the function to generate actions
     * @since 1.15.2
     */
    record PerBone(@NotNull Function<RenderedBone, TrackerUpdateAction> builder) implements TrackerUpdateAction {
        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return builder.apply(bone).test(bone, predicate);
        }
    }

    /**
     * No-op action.
     * @since 1.15.2
     */
    enum None implements TrackerUpdateAction {
        /**
         * Instance.
         * @since 1.15.2
         */
        INSTANCE
        ;

        @Override
        public boolean test(@NotNull RenderedBone bone, @NotNull BonePredicate predicate) {
            return false;
        }
    }
}
