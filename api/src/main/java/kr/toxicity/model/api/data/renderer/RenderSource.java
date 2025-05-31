package kr.toxicity.model.api.data.renderer;

import com.mojang.authlib.GameProfile;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public sealed interface RenderSource {

    @ApiStatus.Internal
    static @NotNull Located of(@NotNull Location location) {
        return new Dummy(location);
    }
    @ApiStatus.Internal
    static @NotNull Located of(@NotNull Location location, @NotNull GameProfile profile, boolean slim) {
        return new ProfiledDummy(location, profile, slim);
    }
    @ApiStatus.Internal
    static @NotNull Based of(@NotNull Entity entity) {
        return entity instanceof Player player ? new BasePlayer(player) : new BaseEntity(entity);
    }

    @NotNull Tracker create(@NotNull RenderInstance instance, @NotNull TrackerModifier modifier);

    sealed interface Based extends RenderSource {
        @NotNull Entity entity();

        @NotNull
        @Override
        EntityTracker create(@NotNull RenderInstance instance, @NotNull TrackerModifier modifier);
    }

    sealed interface Located extends RenderSource {
        @NotNull Location location();

        @NotNull
        @Override
        DummyTracker create(@NotNull RenderInstance instance, @NotNull TrackerModifier modifier);
    }

    sealed interface Profiled extends RenderSource {
        @NotNull GameProfile profile();
        boolean slim();
    }

    record Dummy(@NotNull Location location) implements Located {
        @NotNull
        @Override
        public DummyTracker create(@NotNull RenderInstance instance, @NotNull TrackerModifier modifier) {
            return new DummyTracker(this, instance, modifier);
        }
    }

    record ProfiledDummy(@NotNull Location location, @NotNull GameProfile profile, boolean slim) implements Profiled, Located {
        @NotNull
        @Override
        public DummyTracker create(@NotNull RenderInstance instance, @NotNull TrackerModifier modifier) {
            return new DummyTracker(this, instance, modifier);
        }
    }

    record BaseEntity(@NotNull Entity entity) implements Based {

        @NotNull
        @Override
        public EntityTracker create(@NotNull RenderInstance instance, @NotNull TrackerModifier modifier) {
            var tracker = EntityTracker.tracker(entity.getUniqueId());
            if (tracker != null) tracker.close();
            return new EntityTracker(this, instance, modifier);
        }
    }

    record BasePlayer(@NotNull Player entity) implements Based, Profiled {

        @NotNull
        @Override
        public EntityTracker create(@NotNull RenderInstance instance, @NotNull TrackerModifier modifier) {
            var tracker = EntityTracker.tracker(entity.getUniqueId());
            if (tracker != null) tracker.close();
            return new PlayerTracker(this, instance, modifier);
        }

        @NotNull
        @Override
        public GameProfile profile() {
            return BetterModel.plugin().nms().profile(entity);
        }

        @Override
        public boolean slim() {
            var channel = BetterModel.plugin().playerManager().player(entity.getUniqueId());
            return channel != null && channel.isSlim();
        }
    }
}
