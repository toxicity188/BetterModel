package kr.toxicity.model.api.platform;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.scheduler.ModelTask;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.EntityTrackerRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public interface PlatformEntity extends PlatformRegionHolder {

    @NotNull UUID uuid();

    @NotNull PlatformLocation location();

    @Override
    default @Nullable ModelTask task(@NotNull Runnable runnable) {
        return location().task(runnable);
    }

    @Override
    default @Nullable ModelTask taskLater(long delay, @NotNull Runnable runnable) {
        return location().taskLater(delay, runnable);
    }

    default @NotNull Optional<EntityTrackerRegistry> registry() {
        return BetterModel.registry(uuid());
    }

    default @NotNull Optional<EntityTracker> tracker(@NotNull String name) {
        return registry().map(registry -> registry.tracker(name));
    }
}
