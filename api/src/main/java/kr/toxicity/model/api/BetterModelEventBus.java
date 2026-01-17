/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api;

import kr.toxicity.model.api.event.ModelEvent;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface BetterModelEventBus {

    <T extends ModelEvent> void subscribe(@NotNull Class<T> eventClass, @NotNull Consumer<T> consumer);

    <T extends ModelEvent> @NotNull Result call(@NotNull Class<? extends T> eventClass, @NotNull Supplier<T> eventSupplier);

    default @NotNull Result call(@NotNull ModelEvent event) {
        return call(event.getClass(), () -> event);
    }

    @RequiredArgsConstructor
    enum Result {
        SUCCESS(true),
        FAIL(false),
        NO_EVENT_HANDLER(true)
        ;

        private final boolean triggered;

        public boolean triggered() {
            return triggered;
        }
    }
}
