package kr.toxicity.model.api.event;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface ModelDamageSource {
    @Nullable Entity getCausingEntity();
    @Nullable Entity getDirectEntity();
    @Nullable Location getDamageLocation();
    @Nullable Location getSourceLocation();
    boolean isIndirect();
    float getFoodExhaustion();
    boolean scalesWithDifficulty();
}
