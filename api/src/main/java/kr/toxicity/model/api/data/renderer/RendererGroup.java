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
        this.itemStack = TransformedItemStack.of(
                new Vector3f(),
                new Vector3f(),
                new Vector3f(scale).div(4),
                itemStack != null ? itemStack : new ItemStack(Material.AIR)
        );
        position = group.origin().toVector().div(16);
        this.hitBox = box;
        rotation = group.rotation().toVector();
        if (name.tagged(BoneTags.SEAT)) {
            mountController = BetterModel.plugin().configManager().defaultMountController();
        } else if (name.tagged(BoneTags.SUB_SEAT)) {
            mountController = MountControllers.NONE;
        } else mountController = MountControllers.INVALID;
    }

    /**
     * Creates entity.
     * @param source source
     * @param location location
     * @return entity
     */
    public @NotNull RenderedBone create(@NotNull RenderSource<?> source, @NotNull TrackerModifier modifier, @NotNull Location location) {
        return create(source, modifier, null, location);
    }
    private @NotNull RenderedBone create(@NotNull RenderSource<?> source, @NotNull TrackerModifier modifier, @Nullable RenderedBone parentBone, @NotNull Location location) {
        return new RenderedBone(
                this,
                parentBone,
                itemMapper.apply(source, itemStack),
                itemMapper.transform(),
                location,
                new BoneMovement(
                        parentBone != null ? new Vector3f(position).sub(parentBone.getGroup().position) : new Vector3f(),
                        new Vector3f(1),
                        MathUtil.toQuaternion(rotation),
                        rotation
                ),
                modifier,
                parent -> children.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().create(source, modifier, parent, location)))
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
