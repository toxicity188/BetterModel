/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelConfig;
import kr.toxicity.model.api.BetterModelEventBus;
import kr.toxicity.model.api.BetterModelEvaluator;
import kr.toxicity.model.api.BetterModelLogger;
import kr.toxicity.model.api.BetterModelPlatform;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.event.ModelEvent;
import kr.toxicity.model.api.event.ModelEventApplication;
import kr.toxicity.model.api.event.ModelEventListener;
import kr.toxicity.model.api.manager.Manager;
import kr.toxicity.model.api.manager.ReloadInfo;
import kr.toxicity.model.api.nms.ModAnimationBundler;
import kr.toxicity.model.api.nms.NMS;
import kr.toxicity.model.api.nms.PacketBundler;
import kr.toxicity.model.api.pack.PackZipper;
import kr.toxicity.model.api.platform.PlatformAdapter;
import kr.toxicity.model.api.platform.PlatformLocation;
import kr.toxicity.model.api.platform.PlatformWorld;
import kr.toxicity.model.api.config.DebugConfig;
import kr.toxicity.model.api.config.IndicatorConfig;
import kr.toxicity.model.api.config.ModuleConfig;
import kr.toxicity.model.api.config.PackConfig;
import kr.toxicity.model.api.mount.MountController;
import kr.toxicity.model.api.platform.PlatformItemStack;
import kr.toxicity.model.api.scheduler.ModelScheduler;
import kr.toxicity.model.api.scheduler.ModelTask;
import kr.toxicity.model.api.version.MinecraftVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.semver4j.Semver;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the {@link Tracker#closeAndDrain()} completion contract.
 *
 * @since 3.4.2
 */
class TrackerCloseAndDrainTest {

    private static final AtomicBoolean PLATFORM_REGISTERED = new AtomicBoolean();
    private static ModelRenderer renderer;

    @BeforeAll
    static void registerFakePlatform() {
        if (PLATFORM_REGISTERED.compareAndSet(false, true)) {
            BetterModel.register(new FakePlatform());
        }
        renderer = new ModelRenderer("drain-test", ModelRenderer.Type.GENERAL, new LinkedHashMap<>(), Map.of());
    }

    @Test
    void drainCompletesImmediatelyWhenIdle() {
        var tracker = renderer.create(location(), TrackerModifier.DEFAULT);
        assertFalse(tracker.isScheduled());
        var future = tracker.closeAndDrain();
        assertCompletion(future);
        assertTrue(tracker.isClosed());
    }

    @Test
    void queuedTaskIsInvalidatedAfterDrain() {
        var tracker = renderer.create(location(), TrackerModifier.DEFAULT);
        var future = tracker.closeAndDrain();
        assertCompletion(future);
        var ran = new AtomicBoolean();
        tracker.task(() -> ran.set(true));
        assertFalse(ran.get(), "A task queued after the drain completed must never run.");
    }

    @Test
    void queuedTaskRunsBeforeCompletion() {
        var tracker = renderer.create(location(), TrackerModifier.DEFAULT);
        var queuedRan = new AtomicBoolean();
        tracker.task(() -> queuedRan.set(true));
        var future = tracker.closeAndDrain();
        assertCompletion(future);
        assertTrue(queuedRan.get(), "Work queued before the drain must complete before the receipt.");
    }

    @Test
    void runningUpdaterDefersCompletionAndSourceAccessOrdersBeforeIt() throws Exception {
        var tracker = renderer.create(location(), TrackerModifier.DEFAULT);
        tracker.forRemoval(true);
        var ticks = new AtomicInteger();
        var updaterStarted = new CountDownLatch(1);
        var updaterMayReturn = new CountDownLatch(1);
        var completed = new AtomicBoolean();
        var accessSawCompleted = new AtomicBoolean();

        tracker.tick((_, _) -> {
            // The first tick is the synchronous one inside start(); only the scheduled
            // updater must block so the drain races a genuinely running updater.
            if (ticks.getAndIncrement() == 0) return;
            updaterStarted.countDown();
            try {
                updaterMayReturn.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            tracker.location();
            accessSawCompleted.set(completed.get());
        });
        startScheduler(tracker);
        assertTrue(updaterStarted.await(5, TimeUnit.SECONDS), "The updater must start before the drain.");

        var future = tracker.closeAndDrain();
        assertFalse(future.toCompletableFuture().isDone(), "The receipt must stay incomplete while an updater is blocked.");
        future.thenRun(() -> completed.set(true));

        var concurrent = new CompletableFuture<CompletionStage<Void>>();
        Thread.ofVirtual().start(() -> concurrent.complete(tracker.closeAndDrain()));
        assertSame(future, concurrent.get(5, TimeUnit.SECONDS));
        assertFalse(future.toCompletableFuture().isDone());

        updaterMayReturn.countDown();
        assertCompletion(future);
        assertTrue(tracker.isClosed());
        assertFalse(accessSawCompleted.get(), "The receipt must not complete before a running updater returned.");

        var ran = new AtomicBoolean();
        tracker.task(() -> ran.set(true));
        assertFalse(ran.get(), "No tracker work may run after the receipt completes.");
    }

    @Test
    void failingQueuedTaskCompletesTheReceiptExceptionally() throws Exception {
        var tracker = renderer.create(location(), TrackerModifier.DEFAULT);
        tracker.task(() -> {
            throw new IllegalStateException("boom");
        });
        var future = tracker.closeAndDrain();
        var error = assertThrows(ExecutionException.class, () -> future.toCompletableFuture().get(5, TimeUnit.SECONDS));
        assertInstanceOf(IllegalStateException.class, error.getCause());
        assertTrue(tracker.isClosed());
    }

    @Test
    void failingCloseHandlerCompletesTheReceiptExceptionally() throws Exception {
        var tracker = renderer.create(location(), TrackerModifier.DEFAULT);
        tracker.handleCloseEvent((_, _) -> {
            throw new IllegalStateException("boom");
        });
        var future = tracker.closeAndDrain();
        var error = assertThrows(ExecutionException.class, () -> future.toCompletableFuture().get(5, TimeUnit.SECONDS));
        assertInstanceOf(IllegalStateException.class, error.getCause());
        assertTrue(tracker.isClosed());
    }

    @Test
    void repeatedAndReentrantClosesAreIdempotent() {
        var tracker = renderer.create(location(), TrackerModifier.DEFAULT);
        var closeCalls = new AtomicInteger();
        tracker.handleCloseEvent((_, _) -> {
            closeCalls.incrementAndGet();
            tracker.close();
        });
        var first = tracker.closeAndDrain();
        var second = tracker.closeAndDrain();
        assertCompletion(first);
        assertSame(first, second, "Repeated drains must return the same completion stage.");
        assertEquals(1, closeCalls.get(), "Close handlers must run exactly once.");
    }

    private static void startScheduler(@NotNull Tracker tracker) {
        try {
            var spawn = Tracker.class.getDeclaredMethod("start");
            spawn.setAccessible(true);
            spawn.invoke(tracker);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void assertCompletion(@NotNull CompletionStage<Void> stage) {
        assertDoesNotThrow(() -> stage.toCompletableFuture().get(5, TimeUnit.SECONDS));
    }

    private static @NotNull PlatformLocation location() {
        return new FakeLocation();
    }

    private static @Nullable Object proxyDefault(@NotNull Method method) {
        var type = method.getReturnType();
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type.isInterface()) {
            var loader = type.getClassLoader();
            return java.lang.reflect.Proxy.newProxyInstance(
                loader,
                new Class<?>[]{type},
                (_, m, _) -> proxyDefault(m)
            );
        }
        return null;
    }

    private static final class FakeConfig implements BetterModelConfig {
        @Override
        public @NotNull DebugConfig debug() {
            return new DebugConfig(Set.of());
        }

        @Override
        public @NotNull IndicatorConfig indicator() {
            return new IndicatorConfig(Set.of());
        }

        @Override
        public @NotNull ModuleConfig module() {
            return new ModuleConfig(false, false);
        }

        @Override
        public @NotNull PackConfig pack() {
            return new PackConfig(false);
        }

        @Override
        public boolean metrics() { return false; }

        @Override
        public boolean sightTrace() { return false; }

        @Override
        public boolean mergeWithExternalResources() { return false; }

        @Override
        public @NotNull Supplier<PlatformItemStack> item() {
            return () -> null;
        }

        @Override
        public double maxSight() { return 0; }

        @Override
        public double minSight() { return 0; }

        @Override
        public @NotNull String namespace() { return "test"; }

        @Override
        public @NotNull PackType packType() { return PackType.NONE; }

        @Override
        public @NotNull String buildFolderLocation() { return "build"; }

        @Override
        public boolean followMobInvisibility() { return false; }

        @Override
        public boolean usePurpurAfk() { return false; }

        @Override
        public boolean versionCheck() { return false; }

        @Override
        @SuppressWarnings("unchecked")
        public @NotNull MountController defaultMountController() {
            return (MountController) java.lang.reflect.Proxy.newProxyInstance(
                MountController.class.getClassLoader(),
                new Class<?>[]{MountController.class},
                (_, _, _) -> null
            );
        }

        @Override
        public int lerpFrameTime() { return 0; }

        @Override
        public boolean cancelPlayerModelInventory() { return false; }

        @Override
        public long playerHideDelay() { return 0; }

        @Override
        public int packetBundlingSize() { return 0; }

        @Override
        public boolean enableStrictLoading() { return false; }
    }

    private static final class FakeLocation implements PlatformLocation {
        @Override
        public @NotNull PlatformWorld world() {
            throw new UnsupportedOperationException();
        }

        @Override
        public double x() {
            return 0;
        }

        @Override
        public double y() {
            return 0;
        }

        @Override
        public double z() {
            return 0;
        }

        @Override
        public float pitch() {
            return 0;
        }

        @Override
        public float yaw() {
            return 0;
        }

        @Override
        public @NotNull PlatformLocation add(double x, double y, double z) {
            return this;
        }

        @Override
        public @Nullable ModelTask task(@NotNull Runnable runnable) {
            return null;
        }

        @Override
        public @Nullable ModelTask taskLater(long delay, @NotNull Runnable runnable) {
            return null;
        }

        @Override
        public @NotNull String toString() {
            return "FakeLocation(0, 0, 0)";
        }
    }

    private static final class FakePlatform implements BetterModelPlatform {

        private static BetterModelLogger proxyLogger() {
            return (BetterModelLogger) java.lang.reflect.Proxy.newProxyInstance(
                BetterModelLogger.class.getClassLoader(),
                new Class<?>[]{BetterModelLogger.class},
                (_, _, _) -> null
            );
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public @NotNull File dataFolder() {
            return new File("build/tmp/drain-test");
        }

        @Override
        public @NotNull JarType jarType() {
            return JarType.PAPER;
        }

        @Override
        public @NotNull ReloadResult reload(@NotNull ReloadInfo info) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSnapshot() {
            return false;
        }

        @Override
        public @NotNull BetterModelConfig config() {
            return new FakeConfig();
        }

        @Override
        public @NotNull MinecraftVersion version() {
            return MinecraftVersion.V26_2;
        }

        @Override
        public @NotNull Semver semver() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull NMS nms() {
            var loader = NMS.class.getClassLoader();
            var noopBundler = (PacketBundler) java.lang.reflect.Proxy.newProxyInstance(
                PacketBundler.class.getClassLoader(),
                new Class<?>[]{PacketBundler.class},
                (_, method, _) -> {
                    var name = method.getName();
                    if ("isEmpty".equals(name)) return true;
                    if ("isNotEmpty".equals(name)) return false;
                    return null;
                }
            );
            var noopModBundler = (ModAnimationBundler) java.lang.reflect.Proxy.newProxyInstance(
                ModAnimationBundler.class.getClassLoader(),
                new Class<?>[]{ModAnimationBundler.class},
                (_, _, _) -> null
            );
            return (NMS) java.lang.reflect.Proxy.newProxyInstance(
                loader,
                new Class<?>[]{NMS.class},
                (_, method, _) -> {
                    var name = method.getName();
                    if ("createBundler".equals(name) || "createParallelBundler".equals(name)) return noopBundler;
                    if ("createModAnimationBuilder".equals(name)) return noopModBundler;
                    return proxyDefault(method);
                }
            );
        }

        @Override
        @SuppressWarnings("unchecked")
        public @NotNull <T extends Manager> T manager(@NotNull Class<T> managerClass) {
            return (T) java.lang.reflect.Proxy.newProxyInstance(
                managerClass.getClassLoader(),
                new Class<?>[]{managerClass},
                (_, method, _) -> proxyDefault(method)
            );
        }

        @Override
        public @NotNull ModelScheduler scheduler() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull PlatformAdapter adapter() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addReloadStartHandler(@NotNull Consumer<PackZipper> consumer) {
        }

        @Override
        public void addReloadEndHandler(@NotNull Consumer<ReloadResult> consumer) {
        }

        @Override
        public @NotNull BetterModelLogger logger() {
            return proxyLogger();
        }

        @Override
        public @NotNull BetterModelEvaluator evaluator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull BetterModelEventBus eventBus() {
            return new BetterModelEventBus() {
                @Override
                public @NotNull <T extends ModelEvent> ModelEventListener subscribe(
                    @NotNull ModelEventApplication application,
                    @NotNull Class<T> eventClass,
                    @NotNull Consumer<T> consumer
                ) {
                    return ModelEventListener.NONE;
                }

                @Override
                public <T extends ModelEvent> @NotNull Result call(
                    @NotNull Class<? extends T> eventClass,
                    @NotNull Supplier<T> eventSupplier
                ) {
                    return Result.NO_EVENT_HANDLER;
                }
            };
        }

        @Override
        public @Nullable InputStream getResource(@NotNull String path) {
            return null;
        }
    }
}
