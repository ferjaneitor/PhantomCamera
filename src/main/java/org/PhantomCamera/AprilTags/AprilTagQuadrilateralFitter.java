package org.PhantomCamera.AprilTags;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class AprilTagQuadrilateralFitter {

    public PixelCoordinate[] calculateQuadrilateralCornerArrayFromConvexHullPixelCoordinateList(
            List<PixelCoordinate> convexHullPixelCoordinateList
    ) {
        if (convexHullPixelCoordinateList == null
                || convexHullPixelCoordinateList.size() < 4) {
            return null;
        }

        // 1) Convertir la lista de PixelCoordinate a un MatOfPoint2f de OpenCV
        MatOfPoint2f convexHullPoint2fMat = convertPixelCoordinateListToMatOfPoint2f(
                convexHullPixelCoordinateList
        );

        double convexHullPerimeterLength =
                Imgproc.arcLength(convexHullPoint2fMat, true);

        // 2) Aproximar el hull a un polígono con menos vértices
        //    El factor 0.02 es un punto de partida; después podemos ajustarlo.
        double approximationEpsilonDistance =
                0.02 * convexHullPerimeterLength;

        MatOfPoint2f approximatedPolygonPoint2fMat = new MatOfPoint2f();

        Imgproc.approxPolyDP(
                convexHullPoint2fMat,
                approximatedPolygonPoint2fMat,
                approximationEpsilonDistance,
                true
        );

        Point[] approximatedPolygonPointArray =
                approximatedPolygonPoint2fMat.toArray();

        if (approximatedPolygonPointArray.length != 4) {
            // Todavía no tenemos algo cuadrilátero, regresamos null.
            return null;
        }

        PixelCoordinate[] unorderedQuadrilateralCornerArray =
                convertPointArrayToPixelCoordinateArray(
                        approximatedPolygonPointArray
                );

        // 3) Ordenar esquinas (top-left, top-right, bottom-right, bottom-left)
        PixelCoordinate[] orderedQuadrilateralCornerArray =
                orderQuadrilateralCornersClockwise(
                        unorderedQuadrilateralCornerArray
                );

        return orderedQuadrilateralCornerArray;
    }

    private MatOfPoint2f convertPixelCoordinateListToMatOfPoint2f(
            List<PixelCoordinate> pixelCoordinateList
    ) {
        Point[] pointArray = new Point[pixelCoordinateList.size()];

        for (int currentIndex = 0;
             currentIndex < pixelCoordinateList.size();
             currentIndex++) {

            PixelCoordinate pixelCoordinate = pixelCoordinateList.get(currentIndex);

            pointArray[currentIndex] = new Point(
                    pixelCoordinate.xPixeldCoordinate,
                    pixelCoordinate.yPixeldCoordinate
            );
        }

        MatOfPoint2f matOfPoint2f = new MatOfPoint2f();
        matOfPoint2f.fromArray(pointArray);
        return matOfPoint2f;
    }

    private PixelCoordinate[] convertPointArrayToPixelCoordinateArray(
            Point[] pointArray
    ) {
        PixelCoordinate[] pixelCoordinateArray =
                new PixelCoordinate[pointArray.length];

        for (int currentIndex = 0;
             currentIndex < pointArray.length;
             currentIndex++) {

            Point currentPoint = pointArray[currentIndex];

            int xPixelCoordinate = (int) Math.round(currentPoint.x);
            int yPixelCoordinate = (int) Math.round(currentPoint.y);

            pixelCoordinateArray[currentIndex] =
                    new PixelCoordinate(
                            xPixelCoordinate,
                            yPixelCoordinate
                    );
        }

        return pixelCoordinateArray;
    }

    private PixelCoordinate[] orderQuadrilateralCornersClockwise(
            PixelCoordinate[] unorderedCornerArray
    ) {
        if (unorderedCornerArray == null
                || unorderedCornerArray.length != 4) {
            return unorderedCornerArray;
        }

        // 1) Centroide del cuadrilátero
        double sumXPixelCoordinate = 0.0;
        double sumYPixelCoordinate = 0.0;

        for (PixelCoordinate pixelCoordinate : unorderedCornerArray) {
            sumXPixelCoordinate += pixelCoordinate.xPixeldCoordinate;
            sumYPixelCoordinate += pixelCoordinate.yPixeldCoordinate;
        }

        double centerXPixelCoordinate = sumXPixelCoordinate / 4.0;
        double centerYPixelCoordinate = sumYPixelCoordinate / 4.0;

        PixelCoordinate topLeftCorner = null;
        PixelCoordinate topRightCorner = null;
        PixelCoordinate bottomRightCorner = null;
        PixelCoordinate bottomLeftCorner = null;

        // 2) Clasificar corners con respecto al centro
        for (PixelCoordinate pixelCoordinate : unorderedCornerArray) {

            boolean isLeft =
                    pixelCoordinate.xPixeldCoordinate < centerXPixelCoordinate;
            boolean isTop =
                    pixelCoordinate.yPixeldCoordinate < centerYPixelCoordinate;

            if (isLeft && isTop) {
                topLeftCorner = pixelCoordinate;
            } else if (!isLeft && isTop) {
                topRightCorner = pixelCoordinate;
            } else if (!isLeft && !isTop) {
                bottomRightCorner = pixelCoordinate;
            } else {
                bottomLeftCorner = pixelCoordinate;
            }
        }

        // 3) Construir array final en orden horario
        PixelCoordinate[] orderedCornerArray = new PixelCoordinate[4];
        orderedCornerArray[0] = topLeftCorner;
        orderedCornerArray[1] = topRightCorner;
        orderedCornerArray[2] = bottomRightCorner;
        orderedCornerArray[3] = bottomLeftCorner;

        return orderedCornerArray;
    }
}
