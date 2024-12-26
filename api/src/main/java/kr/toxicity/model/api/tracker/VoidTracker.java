package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public final class VoidTracker extends Tracker {
    private Location location;
    private final UUID uuid;

    public VoidTracker(@NotNull UUID uuid, @NotNull RenderInstance instance, @NotNull Location location) {
        super(instance);
        this.uuid = uuid;
        this.location = location;
    }

    public void location(Location location) {
        this.location = Objects.requireNonNull(location, "location");
        var bundler = BetterModel.inst().nms().createBundler();
        instance.teleport(location, bundler);
        if (!bundler.isEmpty()) for (Player player : viewedPlayer()) {
            bundler.send(player);
        }
    }

    @Override
    public @NotNull Location location() {
        return location;
    }

    @Override
    public @NotNull UUID uuid() {
        return uuid;
    }

    public void spawn(@NotNull Player player) {
        var bundler = BetterModel.inst().nms().createBundler();
        spawn(player, bundler);
        bundler.send(player);
    }
}
