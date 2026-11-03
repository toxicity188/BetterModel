/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.data.renderer.RenderPipeline;
import kr.toxicity.model.api.nms.PacketBundler;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class TrackerBundlerSet {

    private final RenderPipeline pipeline;

    private PacketBundler tickBundler = pipeline.createBundler();
    private PacketBundler dataBundler = pipeline.createBundler();

    void send() {
        if (tickBundler.isNotEmpty()) {
            pipeline.forEachAllPlayer(tickBundler::send);
            tickBundler = pipeline.createBundler();
        }
        if (dataBundler.isNotEmpty()) {
            pipeline.forEachNonHidePlayer(dataBundler::send);
            dataBundler = pipeline.createBundler();
        }
    }

    @NotNull
    public PacketBundler dataBundler() {
        return dataBundler;
    }

    @NotNull
    public PacketBundler tickBundler() {
        return tickBundler;
    }
}
