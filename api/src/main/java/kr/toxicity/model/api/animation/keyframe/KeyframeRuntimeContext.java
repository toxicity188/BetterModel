/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.animation.keyframe;

import kr.toxicity.model.api.util.function.FloatSupplier;
import org.jetbrains.annotations.NotNull;

/**
 * Provides values for runtime Molang queries during keyframe evaluation.
 * <pre>{@code
 * var context = evaluator.newContext();
 * context.set("health", 20F);
 * float health = evaluator.compile("query.health").eval(context);
 * }</pre>
 *
 * @since 3.7.0
 */
public interface KeyframeRuntimeContext {

    /**
     * Gets a context that ignores query registrations.
     * <pre>{@code
     * float value = Keyframe.of(1F).eval(KeyframeRuntimeContext.empty());
     * }</pre>
     *
     * @return the shared empty context
     * @since 3.7.0
     */
    static @NotNull KeyframeRuntimeContext empty() {
        return Empty.INSTANCE;
    }

    /**
     * Binds a fixed numeric value to a Molang query in this context.
     * The default implementation ignores the binding.
     * <pre>{@code
     * context.set("health", 20F);
     * }</pre>
     *
     * @param key the query name without the {@code query.} or {@code q.} prefix
     * @param value the value to return when the query is read
     * @since 3.7.0
     */
    default void set(@NotNull String key, float value) {}

    /**
     * Binds a zero-argument Molang query to a supplier called when the query is evaluated.
     * The default implementation ignores the binding.
     * <pre>{@code
     * context.setFunction("health", () -> 20F);
     * float health = evaluator.compile("q.health()").eval(context);
     * }</pre>
     *
     * @param key the query name without the {@code query.} or {@code q.} prefix
     * @param supplier the supplier that computes the query value
     * @since 3.7.0
     */
    default void setFunction(@NotNull String key, FloatSupplier supplier) {}

    /**
     * The shared runtime context that ignores query registrations.
     * <pre>{@code
     * var context = KeyframeRuntimeContext.Empty.INSTANCE;
     * }</pre>
     *
     * @since 3.7.0
     */
    enum Empty implements KeyframeRuntimeContext {
        /**
         * The singleton returned by {@link KeyframeRuntimeContext#empty()}.
         * @since 3.7.0
         */
        INSTANCE
    }
}
