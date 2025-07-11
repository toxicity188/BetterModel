package kr.toxicity.model.api.tracker;

import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public record EntityHideOption(
        boolean equipment,
        boolean fire,
        boolean visibility,
        boolean glowing
) {
    public static final EntityHideOption DEFAULT = new EntityHideOption(
            true,
            true,
            true,
            true
    );
    public static final EntityHideOption FALSE = builder().build();

    public static @NotNull EntityHideOption composite(@NotNull Stream<EntityHideOption> options) {
        return builder()
                .composite(options)
                .build();
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean equipment;
        private boolean fire;
        private boolean visibility;
        private boolean glowing;

        private Builder() {

        }

        public @NotNull Builder composite(@NotNull Stream<EntityHideOption> options) {
            options.forEach(this::or);
            return this;
        }

        public @NotNull Builder or(@NotNull EntityHideOption option) {
            equipment = equipment || option.equipment;
            fire = fire || option.fire;
            visibility = visibility || option.visibility;
            glowing = glowing || option.glowing;
            return this;
        }

        public @NotNull EntityHideOption build() {
            return new EntityHideOption(
                    equipment,
                    fire,
                    visibility,
                    glowing
            );
        }
    }
}
