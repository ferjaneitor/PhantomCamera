package org.PhantomCamera.Utils;

import org.PhantomCamera.AprilTags.AprilTagConvexHullCalculator;
import org.PhantomCamera.AprilTags.EdgeConnectedComponent;
import org.PhantomCamera.AprilTags.PixelCoordinate;
import org.PhantomCamera.Stadistics.EdgeStadistics.EdgeFrameStatistics;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.List;

public class DrawingBoxes {

    public static void drawBoundingBoxesForCandidateComponents(
            Mat edgeFrameColorMatrix,
            List<EdgeConnectedComponent> candidateEdgeConnectedComponentList
    ) {
        Scalar rectangleColorScalar = new Scalar(0, 255, 0);
        int rectangleThicknessInPixels = 2;

        int currentComponentIndex = 0;

        for (EdgeConnectedComponent edgeConnectedComponent : candidateEdgeConnectedComponentList) {

            int minimumXCoordinate = edgeConnectedComponent.minimumXCoordinate;
            int maximumXCoordinate = edgeConnectedComponent.maximumXCoordinate;
            int minimumYCoordinate = edgeConnectedComponent.minimumYCoordinate;
            int maximumYCoordinate = edgeConnectedComponent.maximumYCoordinate;

            org.opencv.core.Point topLeftPoint =
                    new org.opencv.core.Point(minimumXCoordinate, minimumYCoordinate);
            org.opencv.core.Point bottomRightPoint =
                    new org.opencv.core.Point(maximumXCoordinate, maximumYCoordinate);

            Imgproc.rectangle(
                    edgeFrameColorMatrix,
                    topLeftPoint,
                    bottomRightPoint,
                    rectangleColorScalar,
                    rectangleThicknessInPixels
            );

            // Console logging for each candidate bounding box
            System.out.printf(
                    "[BoundingBoxCandidate] index=%d | componentPixelCount=%d | minX=%d minY=%d maxX=%d maxY=%d%n",
                    currentComponentIndex,
                    edgeConnectedComponent.componentPixelCount,
                    minimumXCoordinate,
                    minimumYCoordinate,
                    maximumXCoordinate,
                    maximumYCoordinate
            );

            currentComponentIndex++;
        }
    }

    public static void drawEdgeAndComponentStatisticsOnFrame(
            Mat edgeBinaryFrameMatrix,
            EdgeFrameStatistics edgeFrameStatistics,
            int totalComponentCount,
            int maximumComponentPixelCount,
            int candidateComponentCount,
            int currentFrameIndex
    ) {
        String edgeInformationTextHeader = String.format(
                "Frame %d - Edge Map And Components",
                currentFrameIndex
        );

        String edgeInformationTextEdges = String.format(
                "EdgePixels=%d MaxGradient=%d",
                edgeFrameStatistics.edgePixelCount,
                edgeFrameStatistics.maximumGradientScaleValue
        );

        String edgeInformationTextComponents = String.format(
                "Components=%d MaxComponentPixels=%d Candidates=%d",
                totalComponentCount,
                maximumComponentPixelCount,
                candidateComponentCount
        );

        org.opencv.core.Point firstTextOriginPosition = new org.opencv.core.Point(10, 30);
        org.opencv.core.Point secondTextOriginPosition = new org.opencv.core.Point(10, 60);
        org.opencv.core.Point thirdTextOriginPosition = new org.opencv.core.Point(10, 90);

        Scalar textColorScalar = new Scalar(255, 255, 255);

        Imgproc.putText(
                edgeBinaryFrameMatrix,
                edgeInformationTextHeader,
                firstTextOriginPosition,
                Imgproc.FONT_HERSHEY_SIMPLEX,
                0.8,
                textColorScalar,
                2
        );

        Imgproc.putText(
                edgeBinaryFrameMatrix,
                edgeInformationTextEdges,
                secondTextOriginPosition,
                Imgproc.FONT_HERSHEY_SIMPLEX,
                0.8,
                textColorScalar,
                2
        );

        Imgproc.putText(
                edgeBinaryFrameMatrix,
                edgeInformationTextComponents,
                thirdTextOriginPosition,
                Imgproc.FONT_HERSHEY_SIMPLEX,
                0.8,
                textColorScalar,
                2
        );

        // Console logging for the same information drawn on the frame
        System.out.println("[EdgeStatsOverlay] " + edgeInformationTextHeader);
        System.out.println("[EdgeStatsOverlay] " + edgeInformationTextEdges);
        System.out.println("[EdgeStatsOverlay] " + edgeInformationTextComponents);
    }

    private static void drawConvexHullForLargestSignificantEdgeConnectedComponent(
            Mat edgeFrameForDrawingMatrix,
            List<EdgeConnectedComponent> edgeConnectedComponentList,
            int minimumComponentPixelCountThreshold,
            AprilTagConvexHullCalculator aprilTagConvexHullCalculator
    ) {
        EdgeConnectedComponent largestEdgeConnectedComponent = null;

        for (EdgeConnectedComponent currentEdgeConnectedComponent : edgeConnectedComponentList) {

            if (currentEdgeConnectedComponent.componentPixelCount
                    < minimumComponentPixelCountThreshold) {
                continue;
            }

            if (currentEdgeConnectedComponent.pixelCoordinateList == null
                    || currentEdgeConnectedComponent.pixelCoordinateList.isEmpty()) {
                continue;
            }

            if (largestEdgeConnectedComponent == null
                    || currentEdgeConnectedComponent.componentPixelCount
                    > largestEdgeConnectedComponent.componentPixelCount) {

                largestEdgeConnectedComponent = currentEdgeConnectedComponent;
            }
        }

        if (largestEdgeConnectedComponent == null) {
            System.out.println("[ConvexHull] No significant component found (above threshold "
                    + minimumComponentPixelCountThreshold + ").");
            return;
        }

        System.out.println("[ConvexHull] Largest component pixel count: "
                + largestEdgeConnectedComponent.componentPixelCount);

        List<PixelCoordinate> convexHullPixelPositionList =
                aprilTagConvexHullCalculator.calculateConvexHullPixelPositionList(
                        largestEdgeConnectedComponent.pixelCoordinateList
                );

        if (convexHullPixelPositionList == null
                || convexHullPixelPositionList.size() < 2) {
            System.out.println("[ConvexHull] Convex hull has fewer than 2 points. Nothing to draw.");
            return;
        }

        System.out.println("[ConvexHull] Convex hull point count: "
                + convexHullPixelPositionList.size());

        // Console logging for each convex hull point
        for (int currentIndex = 0; currentIndex < convexHullPixelPositionList.size(); currentIndex++) {
            PixelCoordinate convexHullPixelCoordinate = convexHullPixelPositionList.get(currentIndex);
            System.out.printf(
                    "[ConvexHull] Point %d: (%d, %d)%n",
                    currentIndex,
                    convexHullPixelCoordinate.xPixeldCoordinate,
                    convexHullPixelCoordinate.yPixeldCoordinate
            );
        }

        Scalar hullLineColorScalar = new Scalar(0, 0, 255); // rojo en BGR
        int hullLineThicknessInPixels = 2;

        int convexHullPointCount = convexHullPixelPositionList.size();

        for (int currentIndex = 0;
             currentIndex < convexHullPointCount;
             currentIndex++) {

            PixelCoordinate firstPixelPosition =
                    convexHullPixelPositionList.get(currentIndex);

            PixelCoordinate secondPixelPosition =
                    convexHullPixelPositionList.get(
                            (currentIndex + 1) % convexHullPointCount
                    );

            Point firstPoint =
                    new Point(
                            firstPixelPosition.xPixeldCoordinate,
                            firstPixelPosition.yPixeldCoordinate
                    );

            Point secondPoint =
                    new Point(
                            secondPixelPosition.xPixeldCoordinate,
                            secondPixelPosition.yPixeldCoordinate
                    );

            Imgproc.line(
                    edgeFrameForDrawingMatrix,
                    firstPoint,
                    secondPoint,
                    hullLineColorScalar,
                    hullLineThicknessInPixels
            );

            // Console logging for each convex hull line segment
            System.out.printf(
                    "[ConvexHull] Line from (%d, %d) to (%d, %d)%n",
                    firstPixelPosition.xPixeldCoordinate,
                    firstPixelPosition.yPixeldCoordinate,
                    secondPixelPosition.xPixeldCoordinate,
                    secondPixelPosition.yPixeldCoordinate
            );
        }
    }

}
