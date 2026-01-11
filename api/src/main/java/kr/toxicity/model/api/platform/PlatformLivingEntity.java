package kr.toxicity.model.api.platform;

import org.jetbrains.annotations.NotNull;

public interface PlatformLivingEntity extends PlatformEntity {

    @NotNull PlatformLocation eyeLocation();
}
