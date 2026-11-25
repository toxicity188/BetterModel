/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelConfig;
import kr.toxicity.model.api.BetterModelPlatform;
import kr.toxicity.model.api.data.raw.ModelLoadResult;
import kr.toxicity.model.api.manager.ModelManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ModelAssetTest {

    private static final byte[] MODEL = """
        {"meta":{"format_version":"4.0"},"resolution":{"width":16,"height":16},
         "elements":[],"outliner":[],"textures":[]}
        """.getBytes(StandardCharsets.UTF_8);

    private final HashMap<String, ModelImporter> importers = new HashMap<>();

    @BeforeEach
    void registerPlatform() {
        var manager = proxy(ModelManager.class, (instance, method, args) ->
            method.getName().equals("importer") ? importers.get(args[0]) : null);
        var config = proxy(BetterModelConfig.class, (instance, method, args) ->
            method.getName().equals("enableStrictLoading") ? false : null);
        BetterModel.register(proxy(BetterModelPlatform.class, (instance, method, args) -> switch (method.getName()) {
            case "manager" -> manager;
            case "config" -> config;
            default -> null;
        }));
    }

    @Test
    void defaultsUnspecifiedExtensionsToBbmodel() {
        ModelAsset.StreamSupplier supplier = () -> new ByteArrayInputStream(MODEL);
        assertEquals("bbmodel", ModelAsset.of("model", MODEL).extension());
        assertEquals("bbmodel", ModelAsset.of("model", supplier).extension());
        assertEquals("bbmodel", ModelAsset.of("model", MODEL.length, supplier).extension());
        assertEquals("bbmodel", new ModelAsset("model", "model", MODEL.length, supplier).extension());
    }

    @Test
    void acceptsExplicitExtensionsForBytesAndStreams() {
        ModelAsset.StreamSupplier supplier = () -> new ByteArrayInputStream(MODEL);
        assertEquals("custom", ModelAsset.of("model", "CUSTOM", MODEL).extension());
        assertEquals("custom", ModelAsset.of("model", "CUSTOM", supplier).extension());
        assertEquals("custom", ModelAsset.of("model", "CUSTOM", MODEL.length, supplier).extension());
    }

    @Test
    void infersFileAndPathExtensions(@TempDir Path directory) throws IOException {
        var path = Files.write(directory.resolve("model.CUSTOM"), MODEL);
        assertEquals("custom", ModelAsset.of(path).extension());
        assertEquals("custom", ModelAsset.of(path.toFile()).extension());
        assertEquals("model", ModelAsset.of(path).name());
        var noExtension = Files.write(directory.resolve("model"), MODEL);
        assertEquals("bbmodel", ModelAsset.of(noExtension).extension());
        assertEquals("bbmodel", ModelAsset.of(noExtension.toFile()).extension());
    }

    @Test
    void dispatchesToRegisteredImporterWithoutParsingJson() {
        var asset = ModelAsset.of("model", "CUSTOM", new byte[]{1, 2, 3});
        var expected = ModelImporter.BBMODEL.load(ModelAsset.of("model", MODEL));
        importers.put("custom", source -> {
            assertSame(asset, source);
            return expected;
        });
        assertSame(expected, asset.toResult());
    }

    @Test
    void fallsBackToBuiltInBbmodelForUnknownExtension() {
        importers.put("bbmodel", _ -> fail("Unknown extensions must use the built-in importer"));
        ModelLoadResult result = ModelAsset.of("model", "unknown", MODEL).toResult();
        assertEquals("model", result.blueprint().name());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void closesDefaultImporterStreamAndPreservesParseFailure() {
        var closed = new AtomicBoolean();
        var asset = ModelAsset.of("model", () -> new ByteArrayInputStream(new byte[]{'{'}) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        });
        var exception = assertThrows(RuntimeException.class, asset::toResult);
        assertEquals("Unable to parse this json asset: model", exception.getMessage());
        assertTrue(closed.get());
    }

    @Test
    void doesNotFallbackAfterRegisteredImporterFails() {
        var failure = new IllegalStateException("Custom importer failed");
        importers.put("custom", _ -> { throw failure; });
        assertSame(failure, assertThrows(IllegalStateException.class,
            () -> ModelAsset.of("model", "custom", MODEL).toResult()));
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
