package org.PhantomCamera.AprilTags;

import java.util.List;

public class EdgeConnectedComponent {

    public final int componentPixelCount;
    public final int minimumPixelXPosition;
    public final int maximumPixelXPosition;
    public final int minimumPixelYPosition;
    public final int maximumPixelYPosition;
    public final List<PixelCoordinate> pixelCoordinateList;

    public EdgeConnectedComponent(
            int componentPixelCount,
            int minimumPixelXPosition,
            int maximumPixelXPosition,
            int minimumPixelYPosition,
            int maximumPixelYPosition,
            List<PixelCoordinate> pixelCoordinateList
    ) {
        this.componentPixelCount = componentPixelCount;
        this.minimumPixelXPosition = minimumPixelXPosition;
        this.maximumPixelXPosition = maximumPixelXPosition;
        this.minimumPixelYPosition = minimumPixelYPosition;
        this.maximumPixelYPosition = maximumPixelYPosition;
        this.pixelCoordinateList = pixelCoordinateList;
    }

}
