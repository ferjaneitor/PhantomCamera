package org.PhantomCamera.AprilTags;

import org.PhantomCamera.Utils.GrayScaleConvertionUtils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

/**
 * Preprocesses camera frames for AprilTag detection.
 * Step 1: manual conversion from BGR color to grayscale.
 */
public class AprilTagFramePreProcessing {

    private final int frameWidth;
    private final int frameHeight;

    /**
     * Reusable grayscale frame matrix (single channel, 8-bit).
     */
    private final Mat grayscaleFrameMatrix;

    /**
     * Reusable input color byte array (B, G, R per pixel → 3 bytes per pixel).
     */
    private final byte[] inputColorByteArray;

    /**
     * Reusable output grayscale byte array (one byte per pixel).
     */
    private final byte[] outputGrayscaleByteArray;

    public AprilTagFramePreProcessing(int frameWidth, int frameHeight) {
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;

        int totalPixelCount = frameWidth * frameHeight;
        int totalColorByteCount = totalPixelCount * 3;

        this.grayscaleFrameMatrix = new Mat(frameHeight, frameWidth, CvType.CV_8UC1);
        this.inputColorByteArray = new byte[totalColorByteCount];
        this.outputGrayscaleByteArray = new byte[totalPixelCount];
    }

    /**
     * Converts a BGR color frame into a grayscale frame using a manual
     * luminance formula through a shared utility. The result is stored inside
     * this.preallocated grayscaleFrameMatrix and returned.
     *
     * @param inputColorFrameMatrix input frame in BGR format (CV_8UC3)
     * @return reference to an internal grayscale Mat (CV_8UC1)
     */
    public Mat convertColorBgrFrameToGrayscale(Mat inputColorFrameMatrix) {
        if (inputColorFrameMatrix.empty()) {
            return grayscaleFrameMatrix;
        }

        if (inputColorFrameMatrix.cols() != frameWidth
                || inputColorFrameMatrix.rows() != frameHeight) {
            throw new IllegalArgumentException(
                    "Input frame size does not match preprocessor configuration. " +
                            "Expected " + frameWidth + "x" + frameHeight +
                            " but received " + inputColorFrameMatrix.cols() +
                            "x" + inputColorFrameMatrix.rows()
            );
        }

        int totalPixelCount = frameWidth * frameHeight;

        // Copy the BGR data from the Mat into our reusable byte array
        inputColorFrameMatrix.get(0, 0, inputColorByteArray);

        // Convert each pixel from BGR to grayscale using the shared utility
        GrayScaleConvertionUtils.convertBgrByteArrayToGrayscaleByteArray(
                inputColorByteArray,
                frameWidth,
                frameHeight,
                outputGrayscaleByteArray
        );

        // Write the grayscale data back into the Mat
        grayscaleFrameMatrix.put(0, 0, outputGrayscaleByteArray);

        return grayscaleFrameMatrix;
    }

}
