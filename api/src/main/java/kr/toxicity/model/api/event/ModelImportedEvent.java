package kr.toxicity.model.api.event;


import kr.toxicity.model.api.data.renderer.ModelRenderer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Model imported event
 */
@RequiredArgsConstructor
@Getter
public final class ModelImportedEvent extends AbstractModelEvent {

    /**
     * Handler list
     */
    public static final HandlerList HANDLER_LIST = new HandlerList();

    private final ModelRenderer renderer;

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    /**
     * Gets a handler list
     * @return handler list
     */
    @SuppressWarnings("unused") //This method is necessary for event API.
    public static @NotNull HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
