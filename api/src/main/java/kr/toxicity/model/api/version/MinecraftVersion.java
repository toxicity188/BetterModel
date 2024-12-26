package kr.toxicity.model.api.version;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public record MinecraftVersion(int first, int second, int third) implements Comparable<MinecraftVersion> {
    private static final Comparator<MinecraftVersion> COMPARATOR = Comparator.comparing(MinecraftVersion::first)
            .thenComparing(MinecraftVersion::second)
            .thenComparing(MinecraftVersion::third);
    @Override
    public int compareTo(@NotNull MinecraftVersion o) {
        return COMPARATOR.compare(this, o);
    }

    public static final MinecraftVersion V1_21_4 = new MinecraftVersion(1, 21, 4);
    public static final MinecraftVersion V1_21_3 = new MinecraftVersion(1, 21, 3);
    public static final MinecraftVersion V1_21_2 = new MinecraftVersion(1, 21, 2);
    public static final MinecraftVersion V1_21_1 = new MinecraftVersion(1, 21, 1);
    public static final MinecraftVersion V1_21 = new MinecraftVersion(1, 21, 0);
    public static final MinecraftVersion V1_20_6 = new MinecraftVersion(1, 20, 6);
    public static final MinecraftVersion V1_20_5 = new MinecraftVersion(1, 20, 5);
    public static final MinecraftVersion V1_20_4 = new MinecraftVersion(1, 20, 4);
    public static final MinecraftVersion V1_20_3 = new MinecraftVersion(1, 20, 3);

    public MinecraftVersion(@NotNull String version) {
        this(version.split("\\."));
    }
    public MinecraftVersion(@NotNull String[] version) {
        this(
                version.length > 0 ? Integer.parseInt(version[0]) : 0,
                version.length > 1 ? Integer.parseInt(version[1]) : 0,
                version.length > 2 ? Integer.parseInt(version[2]) : 0
        );
    }

    @Override
    public String toString() {
        return first + "." + second + "." + third;
    }
}