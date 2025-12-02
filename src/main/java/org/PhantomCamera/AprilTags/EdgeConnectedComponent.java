package org.PhantomCamera.AprilTags;

import java.util.ArrayList;
import java.util.List;

public class EdgeConnectedComponent {

    public final int componentLabelValue;

    public int componentPixelCount;

    public int minimumXCoordinate;
    public int maximumXCoordinate;
    public int minimumYCoordinate;
    public int maximumYCoordinate;

    /**
     * List of all pixel coordinates that belong to this connected component.
     * It is needed for operations such as convex hull computation.
     */
    public final List<PixelCoordinate> pixelCoordinateList;

    public EdgeConnectedComponent(int componentLabelValue) {
        this.componentLabelValue = componentLabelValue;
        this.componentPixelCount = 0;

        this.minimumXCoordinate = Integer.MAX_VALUE;
        this.maximumXCoordinate = Integer.MIN_VALUE;
        this.minimumYCoordinate = Integer.MAX_VALUE;
        this.maximumYCoordinate = Integer.MIN_VALUE;

        this.pixelCoordinateList = new ArrayList<>();
    }

    public void updateWithPixel(
            int pixelXCoordinate,
            int pixelYCoordinate
    ) {
        componentPixelCount++;

        if (pixelXCoordinate < minimumXCoordinate) {
            minimumXCoordinate = pixelXCoordinate;
        }
        if (pixelXCoordinate > maximumXCoordinate) {
            maximumXCoordinate = pixelXCoordinate;
        }
        if (pixelYCoordinate < minimumYCoordinate) {
            minimumYCoordinate = pixelYCoordinate;
        }
        if (pixelYCoordinate > maximumYCoordinate) {
            maximumYCoordinate = pixelYCoordinate;
        }

        pixelCoordinateList.add(
                new PixelCoordinate(
                        pixelXCoordinate,
                        pixelYCoordinate
                )
        );
    }

    public int getComponentWidthInPixels() {
        if (componentPixelCount == 0) {
            return 0;
        }
        return maximumXCoordinate - minimumXCoordinate + 1;
    }

    public int getComponentHeightInPixels() {
        if (componentPixelCount == 0) {
            return 0;
        }
        return maximumYCoordinate - minimumYCoordinate + 1;
    }

    public int getComponentBoundingBoxAreaInPixels() {
        return getComponentWidthInPixels() * getComponentHeightInPixels();
    }

    public double getComponentAspectRatioWidthDividedByHeight() {
        int componentWidthInPixels = getComponentWidthInPixels();
        int componentHeightInPixels = getComponentHeightInPixels();

        if (componentHeightInPixels == 0) {
            return 0.0;
        }

        return (double) componentWidthInPixels / (double) componentHeightInPixels;
    }

    public double getComponentFillRatioWithinBoundingBox() {
        int boundingBoxAreaInPixels = getComponentBoundingBoxAreaInPixels();
        if (boundingBoxAreaInPixels <= 0) {
            return 0.0;
        }
        return (double) componentPixelCount / (double) boundingBoxAreaInPixels;
    }

}
