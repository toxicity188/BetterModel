/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bone.*;
import kr.toxicity.model.api.data.blueprint.BlueprintElement;
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
import java.util.UUID;
import java.util.stream.Stream;

import static kr.toxicity.model.api.util.CollectionUtil.mapValue;

/**
 * A group of models.
 */
@RequiredArgsConstructor
public final class RendererGroup {

    private static final Vector3f DEFAULT_SCALE = new Vector3f(1);
    @Getter
    private final BlueprintElement.Bone parent;
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
     * @param scale scale
     * @param itemStack item
     * @param group parent
     * @param children children
     * @param box hit-box
     */
    public RendererGroup(
        float scale,
        @Nullable ItemStack itemStack,
        @NotNull BlueprintElement.Bone group,
        @NotNull Map<BoneName, RendererGroup> children,
        @Nullable NamedBoundingBox box
    ) {
        this.parent = group;
        this.children = children;
        this.itemStack = TransformedItemStack.of(
            new Vector3f(),
            new Vector3f(),
            new Vector3f(scale),
            itemStack != null ? itemStack : new ItemStack(Material.AIR)
        );
        this.itemMapper = name().toItemMapper();
        position = group.origin().toBlockScale().toVector();
        this.hitBox = box;
        rotation = group.rotation().toVector();
        if (name().tagged(BoneTags.SEAT)) {
            mountController = BetterModel.config().defaultMountController();
        } else if (name().tagged(BoneTags.SUB_SEAT)) {
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
     * Gets name
     * @return name
     */
    public @NotNull BoneName name() {
        return parent.name();
    }

    /**
     * Gets uuid
     * @return uuid
     */
    public @NotNull UUID uuid() {
        return parent.uuid();
    }

    /**
     * Creates entity.
     * @param source source
     * @return entity
     */
    public @NotNull RenderedBone create(@NotNull RenderSource<?> source) {
        return create(source.fallbackContext(), null);
    }

    private @NotNull RenderedBone create(@NotNull BoneRenderContext context, @Nullable RenderedBone parentBone) {
        return new RenderedBone(
            this,
            parentBone,
            context,
            new BoneMovement(
                parentBone != null ? position.sub(parentBone.getGroup().position, new Vector3f()) : new Vector3f(),
                DEFAULT_SCALE,
                MathUtil.toQuaternion(rotation),
                rotation
            ),
            parent -> mapValue(children, value -> value.create(context, parent))
        );
    }

    /**
     * Gets display item.
     * @return item
     */
    public @NotNull TransformedItemStack getItemStack() {
        return itemStack.copy();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RendererGroup that)) return false;
        return uuid().equals(that.uuid());
    }

    @Override
    public int hashCode() {
        return uuid().hashCode();
    }
}
