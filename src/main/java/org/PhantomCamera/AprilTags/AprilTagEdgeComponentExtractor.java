package org.PhantomCamera.AprilTags;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Extracts connected components from a binary edge frame.
 * Each component groups together edge pixels that are connected
 * under 8-connectivity in the image grid.
 */
public class AprilTagEdgeComponentExtractor {

    private final int frameWidthInPixels;
    private final int frameHeightInPixels;

    /**
     * Reusable arrays to avoid creating new memory on every frame.
     */
    private final byte[] edgeBinaryByteArray;
    private final boolean[] visitedPixelBooleanArray;
    private final int[] breadthFirstSearchQueuePixelIndexArray;

    /**
     * Constructor.
     *
     * @param frameWidthInPixels  frame width in pixels
     * @param frameHeightInPixels frame height in pixels
     */
    public AprilTagEdgeComponentExtractor(
            int frameWidthInPixels,
            int frameHeightInPixels
    ) {
        this.frameWidthInPixels = frameWidthInPixels;
        this.frameHeightInPixels = frameHeightInPixels;

        int totalPixelCount = frameWidthInPixels * frameHeightInPixels;

        this.edgeBinaryByteArray = new byte[totalPixelCount];
        this.visitedPixelBooleanArray = new boolean[totalPixelCount];
        this.breadthFirstSearchQueuePixelIndexArray = new int[totalPixelCount];
    }

    /**
     * Extracts a list of connected components from the given binary edge frame.
     * The input frame must be CV_8UC1 and contain 0 or 255 values.
     *
     * @param edgeBinaryFrameMatrix binary edge frame (one channel, 8-bit)
     * @return list of connected components found in the frame
     */
    public List<EdgeConnectedComponent> extractEdgeConnectedComponentList(Mat edgeBinaryFrameMatrix) {

        if (edgeBinaryFrameMatrix.empty()) {
            return new ArrayList<>();
        }

        if (edgeBinaryFrameMatrix.cols() != frameWidthInPixels
                || edgeBinaryFrameMatrix.rows() != frameHeightInPixels) {
            throw new IllegalArgumentException(
                    "Input edge frame size does not match edge component extractor configuration. " +
                            "Expected " + frameWidthInPixels + "x" + frameHeightInPixels +
                            " but received " + edgeBinaryFrameMatrix.cols() +
                            "x" + edgeBinaryFrameMatrix.rows()
            );
        }

        int totalPixelCount = frameWidthInPixels * frameHeightInPixels;

        // Copy binary edge data into reusable array
        edgeBinaryFrameMatrix.get(0, 0, edgeBinaryByteArray);

        // Reset visited pixels
        Arrays.fill(visitedPixelBooleanArray, false);

        List<EdgeConnectedComponent> edgeConnectedComponentList = new ArrayList<>();

        int nextComponentLabelValue = 1;

        for (int pixelIndex = 0; pixelIndex < totalPixelCount; pixelIndex++) {

            if (visitedPixelBooleanArray[pixelIndex]) {
                continue;
            }

            int pixelIntensityValue = edgeBinaryByteArray[pixelIndex] & 0xFF;
            if (pixelIntensityValue == 0) {
                // background pixel
                continue;
            }

            EdgeConnectedComponent edgeConnectedComponent =
                    performBreadthFirstSearchFromSeedPixelIndex(
                            pixelIndex,
                            nextComponentLabelValue
                    );

            if (edgeConnectedComponent.componentPixelCount > 0) {
                edgeConnectedComponentList.add(edgeConnectedComponent);
                nextComponentLabelValue++;
            }
        }

        return edgeConnectedComponentList;
    }

    /**
     * Performs a breadth-first search starting from the given seed pixel index and
     * returns a connected component with all reachable edge pixels.
     * Connectivity is 8-connected.
     *
     * @param seedPixelIndex      starting pixel index
     * @param componentLabelValue label assigned to this component
     * @return connected component containing all pixels in this region
     */
    private EdgeConnectedComponent performBreadthFirstSearchFromSeedPixelIndex(
            int seedPixelIndex,
            int componentLabelValue
    ) {
        EdgeConnectedComponent edgeConnectedComponent =
                new EdgeConnectedComponent(componentLabelValue);

        int queueHeadIndex = 0;
        int queueTailIndex = 0;

        breadthFirstSearchQueuePixelIndexArray[queueTailIndex] = seedPixelIndex;
        queueTailIndex++;

        visitedPixelBooleanArray[seedPixelIndex] = true;

        while (queueHeadIndex < queueTailIndex) {
            int currentPixelIndex =
                    breadthFirstSearchQueuePixelIndexArray[queueHeadIndex];
            queueHeadIndex++;

            int currentPixelYPosition = currentPixelIndex / frameWidthInPixels;
            int currentPixelXPosition = currentPixelIndex % frameWidthInPixels;

            // Update component (count, bounding box and pixel list)
            edgeConnectedComponent.updateWithPixel(
                    currentPixelXPosition,
                    currentPixelYPosition
            );

            // Explore 8-connected neighbors
            for (int neighborDeltaYPosition = -1;
                 neighborDeltaYPosition <= 1;
                 neighborDeltaYPosition++) {

                for (int neighborDeltaXPosition = -1;
                     neighborDeltaXPosition <= 1;
                     neighborDeltaXPosition++) {

                    if (neighborDeltaXPosition == 0 && neighborDeltaYPosition == 0) {
                        continue;
                    }

                    int neighborPixelXPosition =
                            currentPixelXPosition + neighborDeltaXPosition;
                    int neighborPixelYPosition =
                            currentPixelYPosition + neighborDeltaYPosition;

                    if (neighborPixelXPosition < 0
                            || neighborPixelXPosition >= frameWidthInPixels
                            || neighborPixelYPosition < 0
                            || neighborPixelYPosition >= frameHeightInPixels) {
                        continue;
                    }

                    int neighborPixelIndex =
                            neighborPixelYPosition * frameWidthInPixels + neighborPixelXPosition;

                    if (visitedPixelBooleanArray[neighborPixelIndex]) {
                        continue;
                    }

                    int neighborPixelIntensityValue =
                            edgeBinaryByteArray[neighborPixelIndex] & 0xFF;

                    if (neighborPixelIntensityValue == 0) {
                        // background neighbor, skip
                        continue;
                    }

                    // This neighbor is an unvisited edge pixel, add to queue
                    visitedPixelBooleanArray[neighborPixelIndex] = true;
                    breadthFirstSearchQueuePixelIndexArray[queueTailIndex] = neighborPixelIndex;
                    queueTailIndex++;
                }
            }
        }

        return edgeConnectedComponent;
    }

}
