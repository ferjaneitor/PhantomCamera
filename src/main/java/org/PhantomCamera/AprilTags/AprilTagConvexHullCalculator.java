package org.PhantomCamera.AprilTags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AprilTagConvexHullCalculator {

    public List<PixelCoordinate> calculateConvexHullPixelPositionList(
            List<PixelCoordinate> inputPixelPositionList
    ) {
        if (inputPixelPositionList == null || inputPixelPositionList.size() <= 1) {
            return inputPixelPositionList;
        }

        // Copia para no modificar la lista original
        List<PixelCoordinate> sortedPixelPositionList =
                new ArrayList<>(inputPixelPositionList);

        // Ordenar por X y luego por Y
        Collections.sort(
                sortedPixelPositionList,
                new Comparator<PixelCoordinate>() {
                    @Override
                    public int compare(
                            PixelCoordinate firstPixelPosition,
                            PixelCoordinate secondPixelPosition
                    ) {
                        if (firstPixelPosition.xPixeldCoordinate != secondPixelPosition.xPixeldCoordinate) {
                            return Integer.compare(
                                    firstPixelPosition.xPixeldCoordinate,
                                    secondPixelPosition.xPixeldCoordinate
                            );
                        }
                        return Integer.compare(
                                firstPixelPosition.yPixeldCoordinate,
                                secondPixelPosition.yPixeldCoordinate
                        );
                    }
                }
        );

        List<PixelCoordinate> lowerHullPixelPositionList =
                new ArrayList<>();
        List<PixelCoordinate> upperHullPixelPositionList =
                new ArrayList<>();

        // Construir la parte inferior del hull
        for (PixelCoordinate currentPixelPosition : sortedPixelPositionList) {

            while (lowerHullPixelPositionList.size() >= 2) {
                int lastIndex = lowerHullPixelPositionList.size() - 1;

                PixelCoordinate previousPixelPosition =
                        lowerHullPixelPositionList.get(lastIndex - 1);
                PixelCoordinate lastPixelPosition =
                        lowerHullPixelPositionList.get(lastIndex);

                long crossProductValue = calculateCrossProductValue(
                        previousPixelPosition,
                        lastPixelPosition,
                        currentPixelPosition
                );

                if (crossProductValue <= 0) {
                    lowerHullPixelPositionList.remove(lastIndex);
                } else {
                    break;
                }
            }

            lowerHullPixelPositionList.add(currentPixelPosition);
        }

        // Construir la parte superior del hull
        for (int currentIndex = sortedPixelPositionList.size() - 1;
             currentIndex >= 0;
             currentIndex--) {

            PixelCoordinate currentPixelPosition =
                    sortedPixelPositionList.get(currentIndex);

            while (upperHullPixelPositionList.size() >= 2) {
                int lastIndex = upperHullPixelPositionList.size() - 1;

                PixelCoordinate previousPixelPosition =
                        upperHullPixelPositionList.get(lastIndex - 1);
                PixelCoordinate lastPixelPosition =
                        upperHullPixelPositionList.get(lastIndex);

                long crossProductValue = calculateCrossProductValue(
                        previousPixelPosition,
                        lastPixelPosition,
                        currentPixelPosition
                );

                if (crossProductValue <= 0) {
                    upperHullPixelPositionList.remove(lastIndex);
                } else {
                    break;
                }
            }

            upperHullPixelPositionList.add(currentPixelPosition);
        }

        // Unir inferior y superior (sin duplicar extremos)
        lowerHullPixelPositionList.remove(lowerHullPixelPositionList.size() - 1);
        upperHullPixelPositionList.remove(upperHullPixelPositionList.size() - 1);

        List<PixelCoordinate> convexHullPixelPositionList =
                new ArrayList<>(lowerHullPixelPositionList.size()
                        + upperHullPixelPositionList.size());

        convexHullPixelPositionList.addAll(lowerHullPixelPositionList);
        convexHullPixelPositionList.addAll(upperHullPixelPositionList);

        return convexHullPixelPositionList;
    }

    private long calculateCrossProductValue(
            PixelCoordinate originPixelPosition,
            PixelCoordinate firstPixelPosition,
            PixelCoordinate secondPixelPosition
    ) {
        long firstVectorXComponent =
                firstPixelPosition.xPixeldCoordinate - originPixelPosition.xPixeldCoordinate;
        long firstVectorYComponent =
                firstPixelPosition.yPixeldCoordinate - originPixelPosition.yPixeldCoordinate;

        long secondVectorXComponent =
                secondPixelPosition.xPixeldCoordinate - originPixelPosition.xPixeldCoordinate;
        long secondVectorYComponent =
                secondPixelPosition.yPixeldCoordinate - originPixelPosition.yPixeldCoordinate;

        return firstVectorXComponent * secondVectorYComponent
                - firstVectorYComponent * secondVectorXComponent;
    }
}
