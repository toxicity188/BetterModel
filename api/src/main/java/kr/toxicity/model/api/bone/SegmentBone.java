/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bone;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public interface SegmentBone {
    void setBounded(boolean bounded);
    boolean isBounded();

    void setMinX(float minX);
    float getMinX();

    void setMaxX(float maxX);
    float getMaxX();

    void setMinY(float minY);
    float getMinY();

    void setMaxY(float maxY);
    float getMaxY();

    void setMinZ(float minZ);
    float getMinZ();

    void setMaxZ(float maxZ);
    float getMaxZ();

    void setAlignRate(float alignRate);
    float getAlignRate();

    void setElasticity(float elasticity);
    float getElasticity();

    @NotNull Vector3f getWorldLocation();
    @NotNull Vector3f getDelta();
}
