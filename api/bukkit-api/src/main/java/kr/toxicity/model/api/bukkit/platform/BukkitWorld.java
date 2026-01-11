package kr.toxicity.model.api.bukkit.platform;

import kr.toxicity.model.api.platform.PlatformWorld;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public record BukkitWorld(@NotNull World source) implements PlatformWorld {
}
