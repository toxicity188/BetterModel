package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.data.blueprint.BlueprintChildren;
import kr.toxicity.model.api.data.blueprint.NamedBoundingBox;
import kr.toxicity.model.api.entity.EntityMovement;
import kr.toxicity.model.api.entity.RenderedEntity;
import kr.toxicity.model.api.nms.TransformSupplier;
import kr.toxicity.model.api.player.PlayerLimb;
import kr.toxicity.model.api.util.MathUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public final class RendererGroup implements TransformSupplier {

    @Getter
    private final String name;
    @Getter
    private final BlueprintChildren.BlueprintGroup parent;
    @Getter
    private final Vector3f scale;
    @Getter
    private final Vector3f position;
    private final Vector3f rotation;
    private final ItemStack itemStack;
    private final Map<String, RendererGroup> children;
    @Getter
    private final @Nullable NamedBoundingBox hitBox;

    @Getter
    private final Vector3f center;
    @Getter
    private final @Nullable PlayerLimb limb;

    public RendererGroup(
            @NotNull String name,
            float scale,
            @Nullable ItemStack itemStack,
            @NotNull BlueprintChildren.BlueprintGroup group,
            @NotNull Map<String, RendererGroup> children,
            @Nullable NamedBoundingBox box,
            @Nullable PlayerLimb limb
    ) {
        this.name = name;
        this.limb = limb;
        this.scale = limb != null ? new Vector3f(limb.getSlimScale()) : new Vector3f(scale);
        this.parent = group;
        this.children = children;
        this.itemStack = itemStack;
        position = MathUtil.blockBenchToDisplay(group.origin().toVector()
                .div(16));
        this.hitBox = box;
        rotation = group.rotation().toVector();
        center = hitBox != null ? new Vector3f(position).add(hitBox.centerVector()) : position;
    }

    public @NotNull RenderedEntity create(@Nullable Player player, @NotNull Location location) {
        return create(player, null, location);
    }
    private @NotNull RenderedEntity create(@Nullable Player player, @Nullable RenderedEntity entityParent, @NotNull Location location) {
        var entity = new RenderedEntity(
                this,
                entityParent,
                getItem(player),
                location,
                new EntityMovement(
                        entityParent != null ? new Vector3f(position).sub(entityParent.getGroup().position) : new Vector3f(),
                        new Vector3f(1),
                        MathUtil.toQuaternion(MathUtil.blockBenchToDisplay(rotation)),
                        rotation
                )
        );
        entity.setChildren(children.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().create(player, entity, location))));
        return entity;
    }

    @Nullable
    private ItemStack getItem(@Nullable Player player) {
        if (player != null) {
            return limb != null ? limb.createItem(player) : null;
        }
        return itemStack;
    }

    public @NotNull ItemStack getItemStack() {
        return itemStack != null ? itemStack.clone() : new ItemStack(Material.AIR);
    }

    @NotNull
    @Override
    public Vector3f supplyTransform() {
        return new Vector3f(position);
    }
}
