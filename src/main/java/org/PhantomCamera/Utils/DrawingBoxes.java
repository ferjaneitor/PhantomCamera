package org.PhantomCamera.Utils;

import org.PhantomCamera.AprilTags.AprilTagConvexHullCalculator;
import org.PhantomCamera.AprilTags.AprilTagQuadrilateralFitter;
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

            Point topLeftPoint =
                    new Point(minimumXCoordinate, minimumYCoordinate);
            Point bottomRightPoint =
                    new Point(maximumXCoordinate, maximumYCoordinate);

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

        Point firstTextOriginPosition = new Point(10, 30);
        Point secondTextOriginPosition = new Point(10, 60);
        Point thirdTextOriginPosition = new Point(10, 90);

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

    public static void drawConvexHullForLargestSignificantEdgeConnectedComponent(
            Mat edgeFrameForDrawingMatrix,
            List<EdgeConnectedComponent> candidateEdgeConnectedComponentList,
            int minimumComponentPixelCountThreshold,
            AprilTagConvexHullCalculator aprilTagConvexHullCalculator,
            AprilTagQuadrilateralFitter aprilTagQuadrilateralFitter
    ) {
        EdgeConnectedComponent largestEdgeConnectedComponent = null;

        for (EdgeConnectedComponent currentEdgeConnectedComponent : candidateEdgeConnectedComponentList) {

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

        // ===== BLOQUE QUE QUERÍAS INSERTAR =====
        PixelCoordinate[] quadrilateralCornerArray =
                aprilTagQuadrilateralFitter
                        .calculateQuadrilateralCornerArrayFromConvexHullPixelCoordinateList(
                                convexHullPixelPositionList
                        );

        if (quadrilateralCornerArray == null) {
            System.out.println("[Quadrilateral] Could not approximate convex hull with four corner points.");
        } else {
            System.out.println("[Quadrilateral] Corner points in order (topLeft, topRight, bottomRight, bottomLeft):");
            for (int currentIndex = 0;
                 currentIndex < quadrilateralCornerArray.length;
                 currentIndex++) {

                PixelCoordinate pixelCoordinate = quadrilateralCornerArray[currentIndex];
                System.out.printf(
                        "[Quadrilateral] Corner %d: (%d, %d)%n",
                        currentIndex,
                        pixelCoordinate.xPixeldCoordinate,
                        pixelCoordinate.yPixeldCoordinate
                );
            }

            // Dibujar el cuadrilátero en la misma imagen de bordes
            DrawingBoxes.drawQuadrilateralOnEdgeFrame(
                    edgeFrameForDrawingMatrix,
                    quadrilateralCornerArray
            );
        }
        // ===== FIN DEL BLOQUE INSERTADO =====

        Scalar hullLineColorScalar = new Scalar(0, 0, 255); // red in BGR
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

    public static void drawQuadrilateralOnEdgeFrame(
            Mat edgeFrameMatrix,
            PixelCoordinate[] quadrilateralCornerArray
    ) {
        if (quadrilateralCornerArray == null
                || quadrilateralCornerArray.length != 4) {
            return;
        }

        org.opencv.core.Scalar quadrilateralColorScalar =
                new org.opencv.core.Scalar(0, 0, 255); // rojo BGR

        int quadrilateralLineThicknessInPixels = 2;

        for (int currentIndex = 0;
             currentIndex < quadrilateralCornerArray.length;
             currentIndex++) {

            PixelCoordinate firstCornerPixelCoordinate =
                    quadrilateralCornerArray[currentIndex];

            PixelCoordinate secondCornerPixelCoordinate =
                    quadrilateralCornerArray[
                            (currentIndex + 1) % quadrilateralCornerArray.length
                            ];

            org.opencv.core.Point firstPoint = new org.opencv.core.Point(
                    firstCornerPixelCoordinate.xPixeldCoordinate,
                    firstCornerPixelCoordinate.yPixeldCoordinate
            );

            org.opencv.core.Point secondPoint = new org.opencv.core.Point(
                    secondCornerPixelCoordinate.xPixeldCoordinate,
                    secondCornerPixelCoordinate.yPixeldCoordinate
            );

            Imgproc.line(
                    edgeFrameMatrix,
                    firstPoint,
                    secondPoint,
                    quadrilateralColorScalar,
                    quadrilateralLineThicknessInPixels
            );
        }
    }

}
