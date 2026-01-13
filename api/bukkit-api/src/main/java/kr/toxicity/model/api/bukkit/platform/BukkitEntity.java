package kr.toxicity.model.api.bukkit.platform;

import kr.toxicity.model.api.platform.PlatformEntity;
import kr.toxicity.model.api.platform.PlatformLocation;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@ToString
@EqualsAndHashCode
public class BukkitEntity implements PlatformEntity {

    private final Entity source;

    public BukkitEntity(@NotNull Entity source) {
        this.source = source;
    }

    public Entity source() {
        return source;
    }

    @Override
    public @NotNull UUID uuid() {
        return source.getUniqueId();
    }

    @Override
    public @NotNull PlatformLocation location() {
        return BukkitAdapter.adapt(source.getLocation());
    }
}
