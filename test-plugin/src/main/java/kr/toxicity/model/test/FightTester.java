package kr.toxicity.model.test;

import com.google.gson.JsonObject;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.event.PluginStartReloadEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public final class FightTester implements ModelTester, Listener {

    private BetterModelTest test;

    @Override
    public void start(@NotNull BetterModelTest test) {
        this.test = test;
        Bukkit.getPluginManager().registerEvents(this, test);
        var command = test.getCommand("knightsword");
        if (command != null) command.setExecutor((sender, command1, label, args) -> {
            if (sender instanceof Player player) giveKnightSword(player);
            return true;
        });
    }

    @Override
    public void end(@NotNull BetterModelTest test) {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void start(@NotNull PluginStartReloadEvent event) {
        var path = event.getPackZipper()
                .modern()
                .bettermodel();
        path.models().resolve("class_item").add("knight_sword.json", test.asByte("knight_sword.json"));
        path.textures().add("knight_sword.png", test.asByte("knight_sword.png"));

        var model = new JsonObject();
        model.addProperty("type", "minecraft:model");
        model.addProperty("model", "bettermodel:class_item/knight_sword");
        var json = new JsonObject();
        json.add("model", model);

        path.items().add("knight_sword.json", () -> json.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void giveKnightSword(@NotNull Player player) {
        var sword = new ItemStack(Material.NETHERITE_SWORD);
        sword.editMeta(meta -> {
            meta.displayName(MiniMessage.miniMessage().deserialize("<gradient:#FF6A00:#FFD800><b>Knight Sword"));
            meta.setUnbreakable(true);
            meta.setItemModel(new NamespacedKey(
                    (Plugin) BetterModel.plugin(),
                    "knight_sword"
            ));
            meta.addItemFlags(ItemFlag.values());
        });
        player.getInventory().addItem(sword);
    }
}
