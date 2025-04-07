package kr.toxicity.model.api.manager;

import org.jetbrains.annotations.NotNull;

/**
 * Global manager
 */
public interface GlobalManager {
    /**
     * Executes reload
     */
    void reload(@NotNull ReloadInfo info);
}
