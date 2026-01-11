package kr.toxicity.model.api.platform;

import org.jetbrains.annotations.NotNull;

public interface PlatformItemStack {

    boolean isAir();

    @NotNull PlatformItemStack enchant(boolean enchant);

    @NotNull PlatformItemStack modelData(int customModelData, @NotNull PlatformNamespace namespace);

    @NotNull PlatformItemStack clone();
}
