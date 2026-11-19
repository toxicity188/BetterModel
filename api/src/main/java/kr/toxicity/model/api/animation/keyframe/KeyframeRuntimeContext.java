/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.animation.keyframe;

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
     * Binds a Molang query to a function that accepts zero to three numeric arguments.
     * The matching {@code setFunction} overload handles the function according to its arity.
     * The default implementation ignores the binding.
     * <pre>{@code
     * context.set("distance", (x, y, z) -> (float) Math.sqrt(x * x + y * y + z * z));
     * }</pre>
     *
     * @param key query name without {@code query.} or {@code q.} prefix
     * @param function function that computes the query value
     * @since 3.7.0
     */
    default void set(@NotNull String key, @NotNull FloatFunction function) {
        switch (function) {
            case FloatFunction0 function0 -> setFunction(key, function0);
            case FloatFunction1 function1 -> setFunction(key, function1);
            case FloatFunction2 function2 -> setFunction(key, function2);
            case FloatFunction3 function3 -> setFunction(key, function3);
            default -> throw new IllegalStateException("Unexpected value: " + function);
        }
    }

    /**
     * Binds a Molang query that accepts no arguments.
     * The function runs when Molang evaluates the query. The default implementation ignores the binding.
     * <pre>{@code
     * context.setFunction("health", () -> 20F);
     * float value = evaluator.compile("q.health()").eval(context);
     * }</pre>
     *
     * @param key the query name without the {@code query.} or {@code q.} prefix
     * @param function the function that computes the query value
     * @since 3.7.0
     */
    default void setFunction(@NotNull String key, @NotNull FloatFunction0 function) {}

    /**
     * Binds a Molang query that accepts one numeric argument.
     * The function runs when Molang evaluates the query. The default implementation ignores the binding.
     * <pre>{@code
     * context.setFunction("echo", x -> x);
     * float value = evaluator.compile("q.echo(1)").eval(context);
     * }</pre>
     *
     * @param key the query name without the {@code query.} or {@code q.} prefix
     * @param function the function that computes the query value
     * @since 3.7.0
     */
    default void setFunction(@NotNull String key, @NotNull FloatFunction1 function) {}

    /**
     * Binds a Molang query that accepts two numeric arguments.
     * The function runs when Molang evaluates the query. The default implementation ignores the binding.
     * <pre>{@code
     * context.setFunction("sum_two", (x, y) -> x + y);
     * float value = evaluator.compile("q.sum_two(1, 2)").eval(context);
     * }</pre>
     *
     * @param key the query name without the {@code query.} or {@code q.} prefix
     * @param function the function that computes the query value
     * @since 3.7.0
     */
    default void setFunction(@NotNull String key, @NotNull FloatFunction2 function) {}

    /**
     * Binds a Molang query that accepts three numeric arguments.
     * The function runs when Molang evaluates the query. The default implementation ignores the binding.
     * <pre>{@code
     * context.setFunction("sum_three", (x, y, z) -> x + y + z);
     * float value = evaluator.compile("q.sum_three(1, 2, 3)").eval(context);
     * }</pre>
     *
     * @param key the query name without the {@code query.} or {@code q.} prefix
     * @param function the function that computes the query value
     * @since 3.7.0
     */
    default void setFunction(@NotNull String key, @NotNull FloatFunction3 function) {}

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

    /**
     * Common type for Molang query functions accepting zero to three numeric arguments.
     * Use one of {@link FloatFunction0}, {@link FloatFunction1}, {@link FloatFunction2}, or
     * {@link FloatFunction3} to provide a function with a specific arity.
     * <pre>{@code
     * KeyframeRuntimeContext.FloatFunction function = value -> value;
     * context.set("echo", function);
     * }</pre>
     *
     * @since 3.7.0
     */
    interface FloatFunction {}

    /**
     * Computes a numeric query result from no arguments.
     * <pre>{@code
     * FloatFunction0 function = () -> 20F;
     * }</pre>
     *
     * @since 3.7.0
     */
    @FunctionalInterface
    interface FloatFunction0 extends FloatFunction {
        /**
         * Evaluates the query.
         * <pre>{@code
         * float value = function.get();
         * }</pre>
         *
         * @return the numeric query result
         * @since 3.7.0
         */
        float get();
    }

    /**
     * Computes a numeric query result from one numeric argument.
     * <pre>{@code
     * FloatFunction1 function = (p1) -> p1;
     * }</pre>
     *
     * @since 3.7.0
     */
    @FunctionalInterface
    interface FloatFunction1 extends FloatFunction {
        /**
         * Evaluates the query.
         * <pre>{@code
         * float value = function.get(1F);
         * }</pre>
         *
         * @param p1 the first numeric argument
         * @return the numeric query result
         * @since 3.7.0
         */
        float get(float p1);
    }

    /**
     * Computes a numeric query result from two numeric arguments.
     * <pre>{@code
     * FloatFunction2 function = (p1, p2) -> p1 + p2;
     * }</pre>
     *
     * @since 3.7.0
     */
    @FunctionalInterface
    interface FloatFunction2 extends FloatFunction {
        /**
         * Evaluates the query.
         * <pre>{@code
         * float value = function.get(1F, 2F);
         * }</pre>
         *
         * @param p1 the first numeric argument
         * @param p2 the second numeric argument
         * @return the numeric query result
         * @since 3.7.0
         */
        float get(float p1, float p2);
    }

    /**
     * Computes a numeric query result from three numeric arguments.
     * <pre>{@code
     * FloatFunction3 function = (p1, p2, p3) -> p1 + p2 + p3;
     * }</pre>
     *
     * @since 3.7.0
     */
    @FunctionalInterface
    interface FloatFunction3 extends FloatFunction {
        /**
         * Evaluates the query.
         * <pre>{@code
         * float value = function.get(1F, 2F, 3F);
         * }</pre>
         *
         * @param p1 the first numeric argument
         * @param p2 the second numeric argument
         * @param p3 the third numeric argument
         * @return the numeric query result
         * @since 3.7.0
         */
        float get(float p1, float p2, float p3);
    }
}
