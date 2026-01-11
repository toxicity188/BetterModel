package kr.toxicity.model.api.bukkit.platform;

import kr.toxicity.model.api.platform.PlatformPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;


public final class BukkitPlayer extends BukkitLivingEntity implements PlatformPlayer {

    public BukkitPlayer(@NotNull Player source) {
        super(source);
    }

    public @NotNull Player source() {
        return (Player) super.source();
    }

    @Override
    public @NotNull String name() {
        return source().getName();
    }
}
