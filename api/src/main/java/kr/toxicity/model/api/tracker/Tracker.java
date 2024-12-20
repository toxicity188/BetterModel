package kr.toxicity.model.api.tracker;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import kr.toxicity.model.api.ModelRenderer;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import kr.toxicity.model.api.entity.TrackerMovement;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public abstract class Tracker implements AutoCloseable {
    protected final RenderInstance instance;
    private final ScheduledTask task;

    public Tracker(@NotNull Supplier<TrackerMovement> movement, @NotNull RenderInstance instance) {
        this.instance = instance;
        task = Bukkit.getAsyncScheduler().runAtFixedRate(ModelRenderer.inst(), task -> {
            var bundle = ModelRenderer.inst().nms().createBundler();
            instance.move(movement.get(), bundle);
            for (Player player : instance.viewedPlayer()) {
                bundle.send(player);
            }
        }, 50, 50, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() throws Exception {
        task.cancel();
        instance.close();
    }

    public void spawn(@NotNull Player player) {
        instance.spawn(player);
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

    public abstract @NotNull Location location();
}
