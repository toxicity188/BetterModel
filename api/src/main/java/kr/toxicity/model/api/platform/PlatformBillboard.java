package kr.toxicity.model.api.platform;

public enum PlatformBillboard {
    /**
     * No rotation (default).
     */
    FIXED,
    /**
     * Can pivot around vertical axis.
     */
    VERTICAL,
    /**
     * Can pivot around horizontal axis.
     */
    HORIZONTAL,
    /**
     * Can pivot around center point.
     */
    CENTER;
}
