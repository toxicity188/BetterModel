package kr.toxicity.model.test;

import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("unused")
public final class BetterModelTest extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("Plugin enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin disabled.");
    }
}
