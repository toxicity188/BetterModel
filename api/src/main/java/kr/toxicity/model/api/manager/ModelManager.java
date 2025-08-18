package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.data.renderer.ModelRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Set;

/**
 * Model Manager
 */
public interface ModelManager {

    /**
     * Gets renderer by name
     * @param name name
     * @return renderer or null
     */
    @Nullable ModelRenderer renderer(@NotNull String name);

    /**
     * Gets all renderers
     * @return all renderers
     */
    @NotNull @Unmodifiable
    Collection<ModelRenderer> renderers();

    /**
     * Gets all key of renderer
     * @return keys
     */
    @NotNull @Unmodifiable
    Set<String> keys();
}
