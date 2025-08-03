package kr.toxicity.model.api.nms;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Identifiable entity
 */
public interface Identifiable {
    /**
     * Gets entity id
     * @return id
     */
    int id();

    /**
     * Gets entity uuid
     * @return uuid
     */
    @NotNull UUID uuid();
}
