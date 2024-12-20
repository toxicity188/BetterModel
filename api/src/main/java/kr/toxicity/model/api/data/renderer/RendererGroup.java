package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.ModelRenderer;
import kr.toxicity.model.api.data.blueprint.BlueprintChildren;
import kr.toxicity.model.api.entity.EntityMovement;
import kr.toxicity.model.api.entity.RenderedEntity;
import kr.toxicity.model.api.nms.ModelDisplay;
import kr.toxicity.model.api.util.MathUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RendererGroup {

    @Getter
    private final String name;
    private final float scale;
    private final ItemStack itemStack;
    private final Vector3f position;
    private final Vector3f rotation;
    private final Map<String, RendererGroup> children;

    public RendererGroup(
            @NotNull String name,
            float scale,
            @Nullable ItemStack itemStack,
            @NotNull BlueprintChildren.BlueprintGroup group,
            @NotNull Map<String, RendererGroup> children
    ) {
        this.name = name;
        this.scale = scale;
        this.itemStack = itemStack;
        this.children = children;
        position = MathUtil.blockBenchToDisplay(group.origin().toVector().div(16).div(scale));
        rotation = group.rotation().toVector();
    }

    public @NotNull RenderedEntity create(@NotNull Location location) {
        return create(null, location);
    }
    private @NotNull RenderedEntity create(@Nullable RenderedEntity entityParent, @NotNull Location location) {
        ModelDisplay display;
        if (itemStack != null) {
            display = ModelRenderer.inst().nms().create(location);
            display.item(itemStack);
        } else {
            display = null;
        }
        var entity = new RenderedEntity(
                this,
                entityParent,
                display,
                new EntityMovement(
                        entityParent != null ? new Vector3f(position).sub(entityParent.getGroup().position) : position,
                        entityParent != null ? new Vector3f(1) : new Vector3f(scale),
                        MathUtil.toQuaternion(MathUtil.blockBenchToDisplay(rotation)),
                        rotation
                )
        );
        entity.setChildren(children.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().create(entity, location))));
        return entity;
    }
}
