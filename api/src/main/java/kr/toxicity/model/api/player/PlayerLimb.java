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
import org.joml.Vector3f;

@RequiredArgsConstructor
@Getter
public enum PlayerLimb {
    HEAD(new Vector3f(0, 0F, 0), new Vector3f(0.9375F,0.9375F,0.9375F),  new Vector3f(0.9375F,0.9375F,0.9375F), head(0, 0.9375F, 0), ItemDisplay.ItemDisplayTransform.FIXED),
    H_HEAD(new Vector3f(0, 0F, 0), new Vector3f(0.9375F,0.9375F,0.9375F),  new Vector3f(0.9375F,0.9375F,0.9375F), head(0, 0.9375F, 0), ItemDisplay.ItemDisplayTransform.FIXED),

    RIGHT_ARM(new Vector3f(0, -512F, 0), new Vector3f(0.46875F,0.703125F,0.46875F), new Vector3f(0.3515625F,0.703125F,0.46875F), head(0F, -0.703125F, 0), ItemDisplay.ItemDisplayTransform.FIXED),
    RIGHT_FOREARM(new Vector3f(0, -1024F, 0), new Vector3f(0.46875F,0.703125F,0.46875F), new Vector3f(0.3515625F,0.703125F,0.46875F), head(0F, -0.703125F, 0F), ItemDisplay.ItemDisplayTransform.FIXED),
    LEFT_ARM(new Vector3f(0, -1536F, 0), new Vector3f(0.46875F,0.703125F,0.46875F), new Vector3f(0.3515625F,0.703125F,0.46875F), head(0F, -0.703125F, 0F), ItemDisplay.ItemDisplayTransform.FIXED),
    LEFT_FOREARM(new Vector3f(0, -2048F, 0), new Vector3f(0.46875F,0.703125F,0.46875F), new Vector3f(0.3515625F,0.703125F,0.46875F), head(0F, -0.703125F, 0F), ItemDisplay.ItemDisplayTransform.FIXED),

    HIP(new Vector3f(0, -2560F, 0), new Vector3f(0.9375F,0.43945F,0.46875F), new Vector3f(0.9375F,0.52734F,0.46875F), head(0, 0.52734F, 0), ItemDisplay.ItemDisplayTransform.FIXED),
    WAIST(new Vector3f(0, -3072F, 0), new Vector3f(0.9375F,0.52734F,0.46875F), new Vector3f(0.9375F,0.52734F,0.46875F), head(0, 0.52734F, 0), ItemDisplay.ItemDisplayTransform.FIXED),
    CHEST(new Vector3f(0, -3584F, 0), new Vector3f(0.9375F,0.43945F,0.46875F), new Vector3f(0.9375F,0.52734F,0.46875F), head(0, 0.52734F, 0), ItemDisplay.ItemDisplayTransform.FIXED),

    RIGHT_LEG(new Vector3f(0, -4096F, 0), new Vector3f(0.46875F,0.703125F,0.46875F), new Vector3f(0.46875F,0.703125F,0.46875F), head(0, -0.703125F, 0), ItemDisplay.ItemDisplayTransform.FIXED),
    RIGHT_FORELEG(new Vector3f(0, -4608F, 0), new Vector3f(0.46875F,0.703125F,0.46875F), new Vector3f(0.46875F,0.703125F,0.46875F), head(0, -0.703125F, 0), ItemDisplay.ItemDisplayTransform.FIXED),
    LEFT_LEG(new Vector3f(0, -5120F, 0), new Vector3f(0.46875F,0.703125F,0.46875F), new Vector3f(0.46875F,0.703125F,0.46875F), head(0, -0.703125F, 0), ItemDisplay.ItemDisplayTransform.FIXED),
    LEFT_FORELEG(new Vector3f(0, -5632F, 0), new Vector3f(0.46875F,0.703125F,0.46875F), new Vector3f(0.46875F,0.703125F,0.46875F), head(0, -0.703125F, 0), ItemDisplay.ItemDisplayTransform.FIXED),

    LEFT_ITEM(new Vector3f(), new Vector3f(1), new Vector3f(1), new Vector3f(), ItemDisplay.ItemDisplayTransform.THIRDPERSON_LEFTHAND) {
        @NotNull
        @Override
        public TransformedItemStack createItem(@NotNull Player player) {
            return TransformedItemStack.of(player.getInventory().getItemInOffHand());
        }
    },
    RIGHT_ITEM(new Vector3f(), new Vector3f(1), new Vector3f(1), new Vector3f(), ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND) {
        @NotNull
        @Override
        public TransformedItemStack createItem(@NotNull Player player) {
            return TransformedItemStack.of(player.getInventory().getItemInMainHand());
        }
    },
    ;

    public static Vector3f head(float x, float y, float z) {
        return new Vector3f(x, y, z).mul(0.25F);
    }

    private final @NotNull Vector3f position;
    private final @NotNull Vector3f scale;
    private final @NotNull Vector3f slimScale;
    private final @NotNull Vector3f offset;
    private final @NotNull ItemDisplay.ItemDisplayTransform transform;

    public @NotNull TransformedItemStack createItem(@NotNull Player player) {
        var channel = BetterModel.inst().playerManager().player(player.getUniqueId());
        var item = new ItemStack(Material.PLAYER_HEAD);
        var meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        item.setItemMeta(meta);
        return TransformedItemStack.of(offset, channel == null || channel.isSlim() ? slimScale : scale, item);
    }
}