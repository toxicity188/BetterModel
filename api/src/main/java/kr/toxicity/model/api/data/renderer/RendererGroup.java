package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bone.*;
import kr.toxicity.model.api.data.blueprint.BlueprintChildren;
import kr.toxicity.model.api.data.blueprint.NamedBoundingBox;
import kr.toxicity.model.api.mount.MountController;
import kr.toxicity.model.api.mount.MountControllers;
import kr.toxicity.model.api.util.MathUtil;
import kr.toxicity.model.api.util.TransformedItemStack;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Vector3f;

import java.util.Map;
import java.util.stream.Stream;

import static kr.toxicity.model.api.util.CollectionUtil.mapValue;

/**
 * A group of models.
 */
@RequiredArgsConstructor
public final class RendererGroup {

    private static final Vector3f DEFAULT_SCALE = new Vector3f(1);

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
                new Vector3f(scale),
                itemStack != null ? itemStack : new ItemStack(Material.AIR)
        );
        position = group.origin().toBlockScale().toVector();
        this.hitBox = box;
        rotation = group.rotation().toVector();
        if (name.tagged(BoneTags.SEAT)) {
            mountController = BetterModel.config().defaultMountController();
        } else if (name.tagged(BoneTags.SUB_SEAT)) {
            mountController = MountControllers.NONE;
        } else mountController = MountControllers.INVALID;
    }

    public @NotNull Stream<RendererGroup> flatten() {
        return Stream.concat(
                Stream.of(this),
                children.values().stream().flatMap(RendererGroup::flatten)
        );
    }

    /**
     * Creates entity.
     * @param source source
     * @return entity
     */
    public @NotNull RenderedBone create(@NotNull RenderSource<?> source) {
        return create(source, null);
    }
    private @NotNull RenderedBone create(@NotNull RenderSource<?> source, @Nullable RenderedBone parentBone) {
        return new RenderedBone(
                this,
                parentBone,
                source,
                new BoneMovement(
                        parentBone != null ? position.sub(parentBone.getGroup().position, new Vector3f()) : new Vector3f(),
                        DEFAULT_SCALE,
                        MathUtil.toQuaternion(rotation),
                        rotation
                ),
                parent -> mapValue(children, value -> value.create(source, parent))
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
