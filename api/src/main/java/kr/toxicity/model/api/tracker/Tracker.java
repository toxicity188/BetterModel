package kr.toxicity.model.api.tracker;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import kr.toxicity.model.api.ModelRenderer;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import kr.toxicity.model.api.entity.TrackerMovement;
import kr.toxicity.model.api.nms.PacketBundler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public abstract class Tracker implements AutoCloseable {
    public static final NamespacedKey TRACKING_ID = Objects.requireNonNull(NamespacedKey.fromString("betterengine_tracker"));

    protected final RenderInstance instance;
    private final ScheduledTask task;
    private final AtomicBoolean runningSingle = new AtomicBoolean();

    private TrackerMovement previousMovement;

    public Tracker(@NotNull Supplier<TrackerMovement> movement, @NotNull RenderInstance instance) {
        this.instance = instance;
        task = Bukkit.getAsyncScheduler().runAtFixedRate(ModelRenderer.inst(), task -> {
            var bundle = ModelRenderer.inst().nms().createBundler();
            instance.move(isRunningSingleAnimation() && previousMovement != null ? previousMovement : (previousMovement = movement.get()), bundle);
            for (Player player : instance.viewedPlayer()) {
                bundle.send(player);
            }
        }, 50, 50, TimeUnit.MILLISECONDS);
        tint(false);
        instance.move(movement.get(), ModelRenderer.inst().nms().createBundler());
    }

    @Override
    public void close() throws Exception {
        task.cancel();
        instance.close();
    }

    public boolean isRunningSingleAnimation() {
        return runningSingle.get();
    }

    protected void spawn(@NotNull Player player, @NotNull PacketBundler bundler) {
        instance.spawn(player, bundler);
    }
    public void remove(@NotNull Player player) {
        instance.remove(player);
    }

    public int viewedPlayerSize() {
        return instance.viewedPlayerSize();
    }

    public @NotNull List<Player> viewedPlayer() {
        return instance.viewedPlayer();
    }

    public void tint(boolean toggle) {
        instance.tint(toggle);
    }

    public abstract @NotNull Location location();
    public abstract @NotNull UUID uuid();

    public boolean animateLoop(@NotNull String animation) {
        return animateLoop(animation, () -> true);
    }

    public boolean animateLoop(@NotNull String animation, Supplier<Boolean> predicate) {
        return animateLoop(animation, predicate, () -> {});
    }

    public boolean animateLoop(@NotNull String animation, Supplier<Boolean> predicate, Runnable removeTask) {
        return instance.animateLoop(animation, predicate, removeTask);
    }

    public boolean animateSingle(@NotNull String animation) {
        return animateSingle(animation, () -> true);
    }

    public boolean animateSingle(@NotNull String animation, Supplier<Boolean> predicate) {
        return animateSingle(animation, predicate, () -> {});
    }

    public boolean animateSingle(@NotNull String animation, Supplier<Boolean> predicate, Runnable removeTask) {
        var success = instance.animateSingle(animation, predicate, wrapToSingle(removeTask));
        if (success) runningSingle.set(true);
        return success;
    }

    private Runnable wrapToSingle(@NotNull Runnable runnable) {
        return () -> {
            runnable.run();
            runningSingle.set(false);
        };
    }
}
