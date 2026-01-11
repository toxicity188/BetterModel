package kr.toxicity.model.api.platform;

import org.jetbrains.annotations.NotNull;

public interface PlatformLocation extends PlatformRegionHolder {

    @NotNull PlatformWorld world();

    double x();
    double y();
    double z();

    float pitch();
    float yaw();

    @NotNull PlatformLocation add(double x, double y, double z)
}
