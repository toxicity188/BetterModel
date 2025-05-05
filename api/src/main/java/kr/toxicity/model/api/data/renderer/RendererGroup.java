package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bone.*;
import kr.toxicity.model.api.data.blueprint.BlueprintChildren;
import kr.toxicity.model.api.data.blueprint.NamedBoundingBox;
import kr.toxicity.model.api.mount.MountController;
import kr.toxicity.model.api.mount.MountControllers;
import kr.toxicity.model.api.tracker.TrackerModifier;
import kr.toxicity.model.api.util.MathUtil;
import kr.toxicity.model.api.util.TransformedItemStack;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Vector3f;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * A group of models.
 */
@RequiredArgsConstructor
public final class RendererGroup {

    @Getter
    private final BoneName name;
    @Getter
    private final BlueprintChildren.BlueprintGroup parent;
    @Getter
    private final Vector3f position;
    private final Vector3f rotation;
    private final TransformedItemStack itemStack;
    @Getter
    @Unmodifiable
    private final Map<BoneName, RendererGroup> children;
    @Getter
    private final @Nullable NamedBoundingBox hitBox;

    @Getter
    private final Vector3f center;
    @Getter
    private final @NotNull BoneItemMapper itemMapper;

    @Getter
    private final @NotNull MountController mountController;

    /**
     * Creates group instance.
     * @param name name
     * @param scale scale
     * @param itemStack item
     * @param group parent
     * @param children children
     * @param box hit-box
     */
    public RendererGroup(
            @NotNull BoneName name,
            float scale,
            @Nullable ItemStack itemStack,
            @NotNull BlueprintChildren.BlueprintGroup group,
            @NotNull Map<BoneName, RendererGroup> children,
            @Nullable NamedBoundingBox box
    ) {
        this.name = name;
        this.itemMapper = name.toItemMapper();
        this.parent = group;
        this.children = children;
        this.itemStack = new TransformedItemStack(
                new Vector3f(),
                new Vector3f(),
                new Vector3f(scale),
                itemStack != null ? itemStack : new ItemStack(Material.AIR)
        );
        position = MathUtil.blockBenchToDisplay(group.origin().toVector()
                .div(16));
        this.hitBox = box;
        rotation = group.rotation().toVector();
        center = hitBox != null ? hitBox.centerPoint() : new Vector3f();
        if (name.tagged(BoneTag.SEAT)) {
            mountController = BetterModel.inst().configManager().defaultMountController();
        } else if (name.tagged(BoneTag.SUB_SEAT)) {
            mountController = MountControllers.NONE;
        } else mountController = MountControllers.INVALID;
    }

    /**
     * Creates entity.
     * @param source source
     * @param location location
     * @return entity
     */
    public @NotNull RenderedBone create(@NotNull RenderSource source, @NotNull TrackerModifier modifier, @NotNull Location location) {
        return create(source, modifier, null, location);
    }
    private @NotNull RenderedBone create(@NotNull RenderSource source, @NotNull TrackerModifier modifier, @Nullable RenderedBone entityParent, @NotNull Location location) {
        return new RenderedBone(
                this,
                entityParent,
                itemMapper.apply(source, itemStack),
                itemMapper.transform(),
                location,
                new BoneMovement(
                        entityParent != null ? new Vector3f(position).sub(entityParent.getGroup().position) : new Vector3f(),
                        new Vector3f(1),
                        MathUtil.toQuaternion(MathUtil.blockBenchToDisplay(rotation)),
                        rotation
                ),
                modifier,
                parent1 -> children.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().create(source, modifier, parent1, location)))
        );
    }

    /**
     * Gets display item.
     * @return item
     */
    public @NotNull TransformedItemStack getItemStack() {
        return itemStack.copy();
    }
}
