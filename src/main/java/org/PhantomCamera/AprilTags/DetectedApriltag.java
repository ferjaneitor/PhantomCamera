package org.PhantomCamera.AprilTags;

public class DetectedApriltag {

    private final int tagIdIdentifier;

    private final double[] cornerPixelPositionArray;

    private final int rotationIndex;

    private final int hammingDistance;

    public DetectedApriltag(
            int tagIdIdentifier,
            double[] cornerPixelPositionArray,
            int rotationIndex,
            int hammingDistance
    ){
        this.tagIdIdentifier = tagIdIdentifier;
        this.cornerPixelPositionArray = cornerPixelPositionArray;
        this.rotationIndex = rotationIndex;
        this.hammingDistance = hammingDistance;
    }

    public int getTagIdIdentifier() {
        return tagIdIdentifier;
    }

    public double[] getCornerPixelPositionArray() {
        return cornerPixelPositionArray;
    }

    public int getRotationIndex() {
        return rotationIndex;
    }

    public int getHammingDistance() {
        return hammingDistance;
    }

}
