package kr.toxicity.model.api.data.blueprint;

/**
 * Model bounding box
 * @param minX min-x
 * @param minY min-y
 * @param minZ min-z
 * @param maxX max-x
 * @param maxY max-y
 * @param maxZ max-z
 */
public record ModelBoundingBox(
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ
) {
    public double x() {
        return maxX - minX;
    }
    public double y() {
        return maxY - minY;
    }
    public double z() {
        return maxZ - minZ;
    }
    /**
     * Gets length of bounding box
     * @return length
     */
    public double length() {
        return Math.sqrt(Math.pow(x(), 2) + Math.pow(y(), 2) + Math.pow(z(), 2));
    }
}
