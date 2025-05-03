package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.tracker.*;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public sealed interface RenderSource {

    static @NotNull Dummy of(@NotNull Location location) {
        return new Dummy(location);
    }
    static @NotNull BaseEntity of(@NotNull Entity entity) {
        return new BaseEntity(entity);
    }

    @NotNull Tracker create(@NotNull RenderInstance instance, @NotNull TrackerModifier modifier);

    record Dummy(@NotNull Location location) implements RenderSource {
        @NotNull
        @Override
        public VoidTracker create(@NotNull RenderInstance instance, @NotNull TrackerModifier modifier) {
            return new VoidTracker(this, instance, modifier);
        }
    }

    record BaseEntity(@NotNull Entity entity) implements RenderSource {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BaseEntity(Entity other))) return false;
            if (entity == other) return true;
            return Objects.equals(entity.getUniqueId(), other.getUniqueId());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(entity.getUniqueId());
        }

        @NotNull
        @Override
        public EntityTracker create(@NotNull RenderInstance instance, @NotNull TrackerModifier modifier) {
            var tracker = EntityTracker.tracker(entity.getUniqueId());
            if (tracker != null) return tracker;
            return entity instanceof Player ? new PlayerTracker(this, instance, modifier) : new EntityTracker(this, instance, modifier);
        }
    }
}
