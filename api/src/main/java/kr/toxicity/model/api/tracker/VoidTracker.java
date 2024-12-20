package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.data.renderer.RenderInstance;
import kr.toxicity.model.api.entity.TrackerMovement;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Objects;

public class VoidTracker extends Tracker {
    private Location location;
    public VoidTracker(@NotNull RenderInstance instance, @NotNull Location location) {
        super(() -> new TrackerMovement(new Vector3f(), new Vector3f(1), new Vector3f()), instance);
        this.location = location;
    }
    public void setLocation(Location location) {
        this.location = Objects.requireNonNull(location, "location");
        instance.teleport(location);
    }
    @Override
    public @NotNull Location location() {
        return location;
    }
}
