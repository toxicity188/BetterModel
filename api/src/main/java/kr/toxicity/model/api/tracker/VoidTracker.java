package kr.toxicity.model.api.tracker;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import kr.toxicity.model.api.ModelRenderer;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import kr.toxicity.model.api.entity.EntityMovement;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class VoidTracker implements Tracker {
    @Getter
    @Setter
    private EntityMovement movement = new EntityMovement(new Vector3f(), new Vector3f(1), new Quaternionf(), new Vector3f());
    @Getter
    private Location location;
    private final RenderInstance instance;
    private final ScheduledTask task;

    public VoidTracker(@NotNull RenderInstance instance, @NotNull Location location) {
        this.instance = instance;
        this.location = location;
        task = Bukkit.getAsyncScheduler().runAtFixedRate(ModelRenderer.inst(), task -> {
            instance.move(movement);
        }, 50, 50, TimeUnit.MILLISECONDS);
    }

    public void setLocation(Location location) {
        this.location = Objects.requireNonNull(location, "location");
        instance.teleport(location);
    }

    public @NotNull RenderInstance instance() {
        return instance;
    }

    @Override
    public void close() throws Exception {
        task.cancel();
        instance.close();
    }
}
