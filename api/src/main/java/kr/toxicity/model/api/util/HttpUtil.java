package kr.toxicity.model.api.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.vdurmont.semver4j.Semver;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.version.MinecraftVersion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Http util
 */
@ApiStatus.Internal
public final class HttpUtil {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.of(5, ChronoUnit.SECONDS))
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .build();
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(MinecraftVersion.class, (JsonDeserializer<MinecraftVersion>) (json, typeOfT, context) -> new MinecraftVersion(json.getAsString()))
            .registerTypeAdapter(Semver.class, (JsonDeserializer<Semver>) (json, typeOfT, context) -> new Semver(json.getAsString(), Semver.SemverType.LOOSE))
            .create();

    private HttpUtil() {
        throw new RuntimeException();
    }

    public static @NotNull LatestVersion versionList() {
        return versionList(BetterModel.plugin().version());
    }

    public static @NotNull LatestVersion versionList(@NotNull MinecraftVersion version) {
        return client(client -> {
            try (var stream = client.send(HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("https://api.modrinth.com/v2/project/bettermodel/version"))
                    .build(), HttpResponse.BodyHandlers.ofInputStream()).body();
                 var reader = new InputStreamReader(stream);
                 var jsonReader = new JsonReader(reader)
            ) {
                return latestOf(JsonParser.parseReader(jsonReader)
                        .getAsJsonArray()
                        .asList()
                        .stream()
                        .map(e -> GSON.fromJson(e, PluginVersion.class))
                        .filter(v -> v.versions.contains(version))
                        .sorted(Comparator.comparing((PluginVersion v) -> v.versionNumber))
                        .toList()
                        .reversed());
            }
        }).orElse(e -> {
            LogUtil.handleException("Unable to get BetterModel's version info.", e);
            return new LatestVersion(null, null);
        });
    }

    public static @NotNull LatestVersion latestOf(@NotNull List<PluginVersion> versions) {
        PluginVersion release = null, snapshot = null;
        for (PluginVersion version : versions) {
            if (version.versionType.equals("release")) {
                if (release == null) release = version;
            } else if (snapshot == null) snapshot = version;
        }
        return new LatestVersion(release, snapshot);
    }

    public record LatestVersion(@Nullable PluginVersion release, @Nullable PluginVersion snapshot) {
    }

    public record PluginVersion(
            @NotNull @SerializedName("version_number") Semver versionNumber,
            @NotNull @SerializedName("version_type") String versionType,
            @NotNull @SerializedName("game_versions") List<MinecraftVersion> versions
    ) {
        public @NotNull Component toURLComponent() {
            var url = "https://modrinth.com/plugin/bettermodel/version/" + versionNumber.getOriginalValue();
            return Component.text()
                    .content(versionNumber.getOriginalValue())
                    .color(NamedTextColor.AQUA)
                    .hoverEvent(
                            HoverEvent.showText(Component.text()
                                    .append(Component.text(url).color(NamedTextColor.DARK_AQUA))
                                    .appendNewline()
                                    .append(Component.text("Click to download link.")))
                    )
                    .clickEvent(ClickEvent.openUrl(url))
                    .build();
        }
    }

    public static <T> @NotNull Result<T> client(@NotNull HttpClientConsumer<T> consumer) {
        try {
            return new Result.Success<>(consumer.accept(CLIENT));
        } catch (Exception e) {
            return new Result.Failure<>(e);
        }
    }

    public sealed interface Result<T> {

        default @NotNull T orElse(@NotNull Function<Exception, T> function) {
            return switch (this) {
                case Failure<T> failure -> function.apply(failure.exception);
                case Success<T> success -> success.result;
            };
        }

        record Success<T>(@NotNull T result) implements Result<T> {}
        record Failure<T>(@NotNull Exception exception) implements Result<T> {}
    }

    @FunctionalInterface
    public interface HttpClientConsumer<T> {
        @NotNull T accept(@NotNull HttpClient client) throws Exception;
    }
}
