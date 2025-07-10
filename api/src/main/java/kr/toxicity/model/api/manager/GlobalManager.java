package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.pack.PackZipper;
import org.jetbrains.annotations.NotNull;

/**
 * Global manager
 */
public interface GlobalManager {
    /**
     * Executes reload
     * @param info reload info
     * @param zipper pack zipper
     */
    void reload(@NotNull ReloadInfo info, @NotNull PackZipper zipper);
}
