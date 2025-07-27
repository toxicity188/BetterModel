package kr.toxicity.model.test;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public final class BetterModelTest extends JavaPlugin {

    private final List<ModelTester> testers = List.of(
            new RollTester(),
            new FightTester()
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

    public @NotNull Supplier<byte[]> asByte(@NotNull String path) {
        try (
                var get = Objects.requireNonNull(getResource(path))
        ) {
            var bytes = get.readAllBytes();
            return () -> bytes;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
