package org.PhantomCamera.AprilTags;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.Arrays;

/**
 * Computes image gradients and a simple binary edge map from a grayscale frame.
 * This is a low-level building block for AprilTag detection.
 */
public class AprilTagEdgeDetector {

    private final int frameWidth;
    private final int frameHeight;

    /**
     * Threshold on gradient magnitude to decide whether a pixel is considered an edge.
     * This value can be tuned depending on the camera and scene.
     */
    private final int gradientMagnitudeThresholdValue;

    /**
     * Reusable arrays to avoid memory allocations every frame.
     */
    private final byte[] grayscaleByteArray;
    private final short[] gradientXShortArray;
    private final short[] gradientYShortArray;
    private final int[] gradientMagnitudeIntegerArray;
    private final byte[] edgeBinaryByteArray;

    /**
     * Output Mat containing the binary edge frame (single channel, 8-bit).
     */
    private final Mat edgeBinaryFrameMatrix;

    public AprilTagEdgeDetector(
            int frameWidth,
            int frameHeight,
            int gradientMagnitudeThresholdValue
    ) {
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.gradientMagnitudeThresholdValue = gradientMagnitudeThresholdValue;

        int totalPixelCount = frameWidth * frameHeight;

        this.grayscaleByteArray = new byte[totalPixelCount];
        this.gradientXShortArray = new short[totalPixelCount];
        this.gradientYShortArray = new short[totalPixelCount];
        this.gradientMagnitudeIntegerArray = new int[totalPixelCount];
        this.edgeBinaryByteArray = new byte[totalPixelCount];

        this.edgeBinaryFrameMatrix =
                new Mat(frameHeight, frameWidth, CvType.CV_8UC1);
    }

    /**
     * Calculates the binary edge map from a grayscale frame using Sobel filters and
     * a simple threshold on gradient magnitude.
     *
     * @param grayscaleFrameMatrix input grayscale frame (CV_8UC1)
     * @return reference to an internal Mat containing the binary edge frame
     */
    public Mat calculateEdgeBinaryFrameFromGrayscaleFrame(Mat grayscaleFrameMatrix) {
        if (grayscaleFrameMatrix.empty()) {
            return edgeBinaryFrameMatrix;
        }

        if (grayscaleFrameMatrix.cols() != frameWidth
                || grayscaleFrameMatrix.rows() != frameHeight) {
            throw new IllegalArgumentException(
                    "Input grayscale frame size does not match edge detector configuration. " +
                            "Expected " + frameWidth + "x" + frameHeight +
                            " but received " + grayscaleFrameMatrix.cols() +
                            "x" + grayscaleFrameMatrix.rows()
            );
        }

        int totalPixelCount = frameWidth * frameHeight;

        // Copy grayscale data from Mat into reusable array
        grayscaleFrameMatrix.get(0, 0, grayscaleByteArray);

        // Initialize arrays for the new frame
        Arrays.fill(gradientXShortArray, (short) 0);
        Arrays.fill(gradientYShortArray, (short) 0);
        Arrays.fill(gradientMagnitudeIntegerArray, 0);
        Arrays.fill(edgeBinaryByteArray, (byte) 0);

        // Apply Sobel operator (3x3) to compute gradients.
        for (int pixelYPosition = 1;
             pixelYPosition < frameHeight - 1;
             pixelYPosition++) {

            int topRowStartIndex =
                    (pixelYPosition - 1) * frameWidth;
            int middleRowStartIndex =
                    pixelYPosition * frameWidth;
            int bottomRowStartIndex =
                    (pixelYPosition + 1) * frameWidth;

            for (int pixelXPosition = 1;
                 pixelXPosition < frameWidth - 1;
                 pixelXPosition++) {

                int pixelXPositionMinusOne = pixelXPosition - 1;
                int pixelXPositionPlusOne = pixelXPosition + 1;

                int pixelIndex = middleRowStartIndex + pixelXPosition;

                int pixelTopLeftIntensityValue =
                        grayscaleByteArray[topRowStartIndex + pixelXPositionMinusOne] & 0xFF;
                int pixelTopCenterIntensityValue =
                        grayscaleByteArray[topRowStartIndex + pixelXPosition] & 0xFF;
                int pixelTopRightIntensityValue =
                        grayscaleByteArray[topRowStartIndex + pixelXPositionPlusOne] & 0xFF;

                int pixelMiddleLeftIntensityValue =
                        grayscaleByteArray[middleRowStartIndex + pixelXPositionMinusOne] & 0xFF;
                int pixelMiddleCenterIntensityValue =
                        grayscaleByteArray[middleRowStartIndex + pixelXPosition] & 0xFF;
                int pixelMiddleRightIntensityValue =
                        grayscaleByteArray[middleRowStartIndex + pixelXPositionPlusOne] & 0xFF;

                int pixelBottomLeftIntensityValue =
                        grayscaleByteArray[bottomRowStartIndex + pixelXPositionMinusOne] & 0xFF;
                int pixelBottomCenterIntensityValue =
                        grayscaleByteArray[bottomRowStartIndex + pixelXPosition] & 0xFF;
                int pixelBottomRightIntensityValue =
                        grayscaleByteArray[bottomRowStartIndex + pixelXPositionPlusOne] & 0xFF;

                // Sobel Gx
                int gradientXValue =
                        (-1 * pixelTopLeftIntensityValue) +
                                (1 * pixelTopRightIntensityValue) +
                                (-2 * pixelMiddleLeftIntensityValue) +
                                (2 * pixelMiddleRightIntensityValue) +
                                (-1 * pixelBottomLeftIntensityValue) +
                                (1 * pixelBottomRightIntensityValue);

                // Sobel Gy
                int gradientYValue =
                        (1 * pixelTopLeftIntensityValue) +
                                (2 * pixelTopCenterIntensityValue) +
                                (1 * pixelTopRightIntensityValue) +
                                (-1 * pixelBottomLeftIntensityValue) +
                                (-2 * pixelBottomCenterIntensityValue) +
                                (-1 * pixelBottomRightIntensityValue);

                gradientXShortArray[pixelIndex] = (short) gradientXValue;
                gradientYShortArray[pixelIndex] = (short) gradientYValue;

                int gradientMagnitudeValue =
                        Math.abs(gradientXValue) + Math.abs(gradientYValue);

                gradientMagnitudeIntegerArray[pixelIndex] = gradientMagnitudeValue;

                if (gradientMagnitudeValue >= gradientMagnitudeThresholdValue) {
                    edgeBinaryByteArray[pixelIndex] = (byte) 255;
                } else {
                    edgeBinaryByteArray[pixelIndex] = 0;
                }
            }
        }

        // Copy binary edge data into the Mat
        edgeBinaryFrameMatrix.put(0, 0, edgeBinaryByteArray);

        return edgeBinaryFrameMatrix;
    }

    public byte[] getEdgeBinaryByteArray(){
        return  edgeBinaryByteArray;
    }

    public int[] getGradientMagnitudeIntegerArray(){
        return  gradientMagnitudeIntegerArray;
    }
}
