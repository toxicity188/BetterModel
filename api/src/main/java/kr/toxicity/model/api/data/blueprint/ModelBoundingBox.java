package kr.toxicity.model.api.data.blueprint;

public record ModelBoundingBox(
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ
) {
    public double length() {
        return Math.sqrt(Math.pow(maxY - minX, 2) + Math.pow(maxY - minY, 2) + Math.pow(maxZ - minZ, 2));
    }
}
