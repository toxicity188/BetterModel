package kr.toxicity.model.test;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

@SuppressWarnings("unused")
public final class BetterModelTest extends JavaPlugin {

    private final List<ModelTester> testers = List.of(
            new RollTester()
    );

    @Override
    public void onEnable() {
        for (ModelTester tester : testers) {
            tester.start(this);
        }
        getLogger().info("Plugin enabled.");
    }

    @Override
    public void onDisable() {
        for (ModelTester tester : testers) {
            tester.end(this);
        }
        getLogger().info("Plugin disabled.");
    }
}
