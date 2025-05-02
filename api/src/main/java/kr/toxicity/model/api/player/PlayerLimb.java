package kr.toxicity.model.api.player;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.util.TransformedItemStack;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Player limb data
 */
@RequiredArgsConstructor
@Getter
public enum PlayerLimb {
    /**
     * Head
     */
    HEAD(position(0), scale(7.4688F, 0.5F), scale(7.4688F, 0.5F), offset(0, 7.5F, 0, 0.5F), ItemDisplay.ItemDisplayTransform.FIXED),
    /**
     * Right arm
     */
    RIGHT_ARM(position(1), scale(3.7188F,5.5938F,3.7188F, 0.25F), scale(2.7891F,5.5938F,3.7188F, 0.25F), offset(-0.625F, 1.5F, 0, 0.25F), ItemDisplay.ItemDisplayTransform.FIXED),
    /**
     * Right forearm
     */
    RIGHT_FOREARM(position(2), scale(3.7188F,5.5938F,3.7188F, 0.25F), scale(2.7891F,5.5938F,3.7188F, 0.25F), offset(-0.625F, 1.5F, 0, 0.25F), ItemDisplay.ItemDisplayTransform.FIXED),
    /**
     * Left arm
     */
    LEFT_ARM(position(3), scale(3.7188F,5.5938F,3.7188F, 0.25F), scale(2.7891F,5.5938F,3.7188F, 0.25F), offset(0.625F, 1.5F, 0, 0.25F), ItemDisplay.ItemDisplayTransform.FIXED),
    /**
     * Left forearm
     */
    LEFT_FOREARM(position(4), scale(3.7188F,5.5938F,3.7188F, 0.25F), scale(2.7891F,5.5938F,3.7188F, 0.25F), offset(0.625F, 1.5F, 0, 0.25F), ItemDisplay.ItemDisplayTransform.FIXED),
    /**
     * Hip
     */
    HIP(position(5), scale(7.4688F,3.7188F,3.7188F, 0.25F), scale(7.4688F,3.7188F,3.7188F, 0.25F), offset(0, 5.75F, 0, 0.25F), ItemDisplay.ItemDisplayTransform.FIXED),
    /**
     * Waist
     */
    WAIST(position(6), scale(7.4688F,3.7188F,3.7188F, 0.25F), scale(7.4688F,3.7188F,3.7188F, 0.25F), offset(0, 5.75F, 0, 0.25F), ItemDisplay.ItemDisplayTransform.FIXED),
    /**
     * Chest
     */
    CHEST(position(7), scale(7.4688F,3.7188F,3.7188F, 0.25F), scale(7.4688F,3.7188F,3.7188F, 0.25F), offset(0, 5.75F, 0, 0.25F), ItemDisplay.ItemDisplayTransform.FIXED),
    /**
     * Right leg
     */
    RIGHT_LEG(position(8), scale(3.7188F,5.5938F,3.7188F, 0.25F), scale(3.7188F,5.5938F,3.7188F, 0.25F), offset(0, 1.12F, 0, 0.25F), ItemDisplay.ItemDisplayTransform.FIXED),
    /**
     * Right foreleg
     */
    RIGHT_FORELEG(position(9), scale(3.7188F,5.5938F,3.7188F, 0.25F), scale(3.7188F,5.5938F,3.7188F, 0.25F), offset(0, 1.12F, 0, 0.25F), ItemDisplay.ItemDisplayTransform.FIXED),
    /**
     * LEft leg
     */
    LEFT_LEG(position(10), scale(3.7188F,5.5938F,3.7188F, 0.25F), scale(3.7188F,5.5938F,3.7188F, 0.25F), offset(0, 1.12F, 0, 0.25F), ItemDisplay.ItemDisplayTransform.FIXED),
    /**
     * Left foreleg
     */
    LEFT_FORELEG(position(11), scale(3.7188F,5.5938F,3.7188F, 0.25F), scale(3.7188F,5.5938F,3.7188F, 0.25F), offset(0, 1.12F, 0, 0.25F), ItemDisplay.ItemDisplayTransform.FIXED),
    /**
     * Left item
     */
    LEFT_ITEM(new Vector3f(), new Vector3f(1), new Vector3f(1), new Vector3f(), ItemDisplay.ItemDisplayTransform.THIRDPERSON_LEFTHAND) {
        @NotNull
        @Override
        public TransformedItemStack createItem(@NotNull Player player) {
            return TransformedItemStack.of(player.getInventory().getItemInOffHand());
        }
    },
    /**
     * Right item
     */
    RIGHT_ITEM(new Vector3f(), new Vector3f(1), new Vector3f(1), new Vector3f(), ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND) {
        @NotNull
        @Override
        public TransformedItemStack createItem(@NotNull Player player) {
            return TransformedItemStack.of(player.getInventory().getItemInMainHand());
        }
    },
    ;

    private static final Map<String, PlayerLimb> PLAYER_LIMBS = new HashMap<>();

    static {
        for (PlayerLimb value : values()) {
            var name = value.name();
            if (name.endsWith("ITEM")) continue;
            var key = "PLAYER_" + name;
            PLAYER_LIMBS.put(key, value);
        }
    }

    public static @Nullable PlayerLimb getLimb(@NotNull String key) {
        return PLAYER_LIMBS.get(key);
    }

    private static Vector3f position(int mul) {
        return new Vector3f(0, -512, 0).mul(mul);
    }

    private static Vector3f scale(float scale, float inflate) {
        return scale(scale, scale, scale, inflate);
    }

    private static Vector3f scale(float x, float y, float z, float inflate) {
        return new Vector3f(x, y, z).div(8).add(new Vector3f(inflate).div(8));
    }

    private static Vector3f offset(float x, float y, float z, float inflate) {
        return new Vector3f(0, -0.25F, 0).add(new Vector3f(x, y, z).div(16)).add(new Vector3f(0, inflate, 0).div(32));
    }

    private final @NotNull Vector3f position;
    private final @NotNull Vector3f scale;
    private final @NotNull Vector3f slimScale;
    private final @NotNull Vector3f offset;
    private final @NotNull ItemDisplay.ItemDisplayTransform transform;

    /**
     * Generates transformed item from player
     * @param player target player
     * @return item
     */
    public @NotNull TransformedItemStack createItem(@NotNull Player player) {
        var item = new ItemStack(Material.PLAYER_HEAD);
        var meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        item.setItemMeta(meta);
        return TransformedItemStack.of(offset, skinScale(player), item);
    }

    public @NotNull TransformedItemStack createItem() {
        var item = new ItemStack(Material.PLAYER_HEAD);
        return TransformedItemStack.of(offset, skinScale(null), item);
    }

    public @NotNull Vector3f skinScale(@Nullable Player player) {
        if (player != null) {
            var channel = BetterModel.inst().playerManager().player(player.getUniqueId());
            return channel == null || channel.isSlim() ? slimScale : scale;
        }
        return scale;
    }
}