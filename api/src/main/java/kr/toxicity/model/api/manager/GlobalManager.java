package kr.toxicity.model.api.manager;

import org.jetbrains.annotations.NotNull;

/**
 * Global manager
 */
public interface GlobalManager {
    /**
     * Executes reload
     * @param info reload info
     */
    void reload(@NotNull ReloadInfo info);
}
