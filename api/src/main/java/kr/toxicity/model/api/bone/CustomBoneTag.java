package kr.toxicity.model.api.bone;

import kr.toxicity.model.api.util.TransformedItemStack;
import lombok.Getter;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

/**
 * Custom bone tag
 * @param name name
 * @param tags tags
 * @param itemMapper item mapper
 */
public record CustomBoneTag(
        @NotNull String name,
        @Getter @NotNull String[] tags,
        @Getter @Nullable BoneItemMapper itemMapper
) implements BoneTag {

    /**
     * Creates bone tag for player
     * @param name name
     * @param transform item transformation
     * @param tags tags
     * @param mapper item mapper
     */
    public CustomBoneTag(
            @NotNull String name,
            @NotNull ItemDisplay.ItemDisplayTransform transform,
            @NotNull String[] tags,
            @NotNull Function<Player, TransformedItemStack> mapper
    ) {
        this(name, tags, BoneItemMapper.player(transform, mapper));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CustomBoneTag that)) return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
