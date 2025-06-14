package kr.toxicity.model.api.data.renderer;

import com.mojang.authlib.GameProfile;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

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

    @NotNull Tracker create(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier);

    sealed interface Based extends RenderSource {
        @NotNull Entity entity();

        @NotNull
        @Override
        EntityTracker create(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier);
        @NotNull
        EntityTracker getOrCreate(@NotNull String name, @NotNull Supplier<RenderPipeline> supplier, @NotNull TrackerModifier modifier);
    }

    sealed interface Located extends RenderSource {
        @NotNull Location location();

        @NotNull
        @Override
        DummyTracker create(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier);
    }

    sealed interface Profiled extends RenderSource {
        @NotNull GameProfile profile();
        boolean slim();
    }

    record Dummy(@NotNull Location location) implements Located {
        @NotNull
        @Override
        public DummyTracker create(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier) {
            return new DummyTracker(location, pipeline, modifier);
        }
    }

    record ProfiledDummy(@NotNull Location location, @NotNull GameProfile profile, boolean slim) implements Profiled, Located {
        @NotNull
        @Override
        public DummyTracker create(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier) {
            return new DummyTracker(location, pipeline, modifier);
        }
    }

    record BaseEntity(@NotNull Entity entity) implements Based {

        @NotNull
        @Override
        public EntityTracker create(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier) {
            return EntityTrackerRegistry.registry(entity).create(pipeline.name(), r -> new EntityTracker(r, pipeline, modifier));
        }

        @Override
        public @NotNull EntityTracker getOrCreate(@NotNull String name, @NotNull Supplier<RenderPipeline> supplier, @NotNull TrackerModifier modifier) {
            return EntityTrackerRegistry.registry(entity).getOrCreate(name, r -> new EntityTracker(r, supplier.get(), modifier));
        }
    }

    record BasePlayer(@NotNull Player entity) implements Based, Profiled {

        @NotNull
        @Override
        public EntityTracker create(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier) {
            return EntityTrackerRegistry.registry(entity).create(pipeline.name(), r -> new PlayerTracker(r, pipeline, modifier));
        }

        @Override
        public @NotNull EntityTracker getOrCreate(@NotNull String name, @NotNull Supplier<RenderPipeline> supplier, @NotNull TrackerModifier modifier) {
            return EntityTrackerRegistry.registry(entity).getOrCreate(name, r -> new PlayerTracker(r, supplier.get(), modifier));
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
