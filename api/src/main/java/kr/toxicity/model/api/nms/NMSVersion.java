package kr.toxicity.model.api.nms;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * Represents server's version.
 */
@RequiredArgsConstructor
@Getter
public enum NMSVersion {
    /**
     * 1.20.3-1.20.4
     */
    V1_20_R3(20,3, 22),
    /**
     * 1.20.5-1.20.6
     */
    V1_20_R4(20,4, 32),
    /**
     * 1.21-1.21.1
     */
    V1_21_R1(21,1, 34),
    /**
     * 1.21.2-1.21.3
     */
    V1_21_R2(21,2, 42),
    /**
     * 1.21.4
     */
    V1_21_R3(21,3, 46),
    /**
     * 1.21.5
     */
    V1_21_R4(21,4, 55)
    ;
    /**
     * Main version.
     */
    private final int version;
    /**
     * Sub version.
     */
    private final int subVersion;
    /**
     * That client version's resource pack mcmeta version.
     */
    private final int metaVersion;

    public static @NotNull NMSVersion first() {
        return values()[0];
    }

    public static @NotNull NMSVersion latest() {
        var entries = values();
        return entries[entries.length - 1];
    }
}