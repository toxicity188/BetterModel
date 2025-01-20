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
    /**
     * Gets length of bounding box
     * @return length
     */
    public double length() {
        return Math.sqrt(Math.pow(maxY - minX, 2) + Math.pow(maxY - minY, 2) + Math.pow(maxZ - minZ, 2));
    }
}
