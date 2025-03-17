package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.data.renderer.BlueprintRenderer;
import kr.toxicity.model.api.nms.PlayerChannelHandler;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.UUID;

public interface PlayerManager extends GlobalManager {
    @Nullable PlayerChannelHandler player(@NotNull UUID uuid);
    @NotNull PlayerChannelHandler player(@NotNull Player player);
    void animate(@NotNull Player player, @NotNull String model, @NotNull String animation);
    @NotNull @Unmodifiable
    List<BlueprintRenderer> limbs();
    @Nullable BlueprintRenderer limb(@NotNull String name);

}
