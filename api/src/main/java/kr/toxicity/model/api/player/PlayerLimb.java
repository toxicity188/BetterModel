package kr.toxicity.model.api.player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

@RequiredArgsConstructor
@Getter
public enum PlayerLimb {
    HEAD(new Vector3f(0, 0F, 0), new Vector3f(0.9375F,0.9375F,0.9375F),  new Vector3f(0.9375F,0.9375F,0.9375F), new Vector3f(0, 0.15F, 0)),
    H_HEAD(new Vector3f(0, 0F, 0), new Vector3f(0.9375F,0.9375F,0.9375F),  new Vector3f(0.9375F,0.9375F,0.9375F), new Vector3f(0, 0.15F, 0)),

    RIGHT_ARM(new Vector3f(0, -512F - 0.1F, 0), new Vector3f(0.46875F,0.703125F,0.46875F), new Vector3f(0.3515625F,0.703125F,0.46875F), new Vector3f(0F, -0.1F, 0)),
    RIGHT_FOREARM(new Vector3f(0, -1024F - 0.1F, 0), new Vector3f(0.46875F,0.703125F,0.46875F), new Vector3f(0.3515625F,0.703125F,0.46875F), new Vector3f(0F, -0.1F, 0)),
    LEFT_ARM(new Vector3f(0, -1536F - 0.1F, 0), new Vector3f(0.46875F,0.703125F,0.46875F), new Vector3f(0.3515625F,0.703125F,0.46875F), new Vector3f(0F, -0.1F, 0F)),
    LEFT_FOREARM(new Vector3f(0, -2048F - 0.1F, 0), new Vector3f(0.46875F,0.703125F,0.46875F), new Vector3f(0.3515625F,0.703125F,0.46875F), new Vector3f(0F, -0.1F, 0)),

    HIP(new Vector3f(0, -2560F, 0), new Vector3f(0.9375F,0.43945F,0.46875F), new Vector3f(0.9375F,0.52734F,0.46875F), new Vector3f(0, 0.15F, 0)),
    WAIST(new Vector3f(0, -3072F, 0), new Vector3f(0.9375F,0.52734F,0.46875F), new Vector3f(0.9375F,0.52734F,0.46875F), new Vector3f()),
    CHEST(new Vector3f(0, -3584F, 0), new Vector3f(0.9375F,0.43945F,0.46875F), new Vector3f(0.9375F,0.52734F,0.46875F), new Vector3f()),

    RIGHT_LEG(new Vector3f(0, -4096F, 0), new Vector3f(0.46875F,0.703125F,0.46875F), new Vector3f(0.46875F,0.703125F,0.46875F), new Vector3f(0, -0.15F, 0)),
    RIGHT_FORELEG(new Vector3f(0, -4608F, 0), new Vector3f(0.46875F,0.703125F,0.46875F), new Vector3f(0.46875F,0.703125F,0.46875F), new Vector3f(0, -0.15F, 0)),
    LEFT_LEG(new Vector3f(0, -5120F, 0), new Vector3f(0.46875F,0.703125F,0.46875F), new Vector3f(0.46875F,0.703125F,0.46875F), new Vector3f(0, -0.15F, 0)),
    LEFT_FORELEG(new Vector3f(0, -5632F, 0), new Vector3f(0.46875F,0.703125F,0.46875F), new Vector3f(0.46875F,0.703125F,0.46875F), new Vector3f(0, -0.15F, 0)),

    LEFT_ITEM(new Vector3f(), new Vector3f(1), new Vector3f(1), new Vector3f()) {
        @NotNull
        @Override
        public ItemStack createItem(@NotNull Player player) {
            return player.getInventory().getItemInOffHand();
        }
    },
    RIGHT_ITEM(new Vector3f(), new Vector3f(1), new Vector3f(1), new Vector3f()) {
        @NotNull
        @Override
        public ItemStack createItem(@NotNull Player player) {
            return player.getInventory().getItemInMainHand();
        }
    },
    ;
    private final @NotNull Vector3f position;
    private final @NotNull Vector3f scale;
    private final @NotNull Vector3f slimScale;
    private final @NotNull Vector3f offset;

    public @NotNull ItemStack createItem(@NotNull Player player) {
        var item = new ItemStack(Material.PLAYER_HEAD);
        var meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        item.setItemMeta(meta);
        return item;
    }
}