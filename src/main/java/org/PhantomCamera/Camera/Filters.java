package org.PhantomCamera.Camera;

import org.PhantomCamera.Utils.GrayScaleConvertionUtils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;

public class Filters {

    public enum FilterMode {
        ORIGINAL,
        GRAYSCALE,
        CANNY_EDGES,
        ORIGINAL_WITH_CANNY_EDGES
    }

    public static Mat applyFilterByMode(Mat originalFrameMatrix, FilterMode filterMode) {
        switch (filterMode) {
            case ORIGINAL:
                return applyOriginalFilter(originalFrameMatrix);
            case GRAYSCALE:
                return applyGrayscaleFilter(originalFrameMatrix);
            case CANNY_EDGES:
                return applyCannyEdgesFilter(originalFrameMatrix);
            case ORIGINAL_WITH_CANNY_EDGES:
                return applyOriginalWithCannyEdgesFilter(originalFrameMatrix);
            default:
                return applyOriginalFilter(originalFrameMatrix);
        }
    }

    public static Mat applyOriginalFilter(Mat originalFrameMatrix) {
        Mat resultFrameMatrix = new Mat();
        originalFrameMatrix.copyTo(resultFrameMatrix);
        return resultFrameMatrix;
    }

    /**
     * Converts the frame to grayscale using direct byte access and an
     * integer luminance approximation for maximum speed. Returns a
     * three-channel BGR Mat so that it can be shown alongside color frames.
     */
    public static Mat applyGrayscaleFilter(Mat originalFrameMatrix) {
        int frameWidth = originalFrameMatrix.cols();
        int frameHeight = originalFrameMatrix.rows();
        int numberOfColorChannels = originalFrameMatrix.channels(); // expected 3 (blue, green, red)

        if (numberOfColorChannels != 3) {
            // Simple fallback in case the input has an unexpected number of channels
            Mat fallbackGrayscaleSingleChannelFrame = new Mat();
            Imgproc.cvtColor(originalFrameMatrix, fallbackGrayscaleSingleChannelFrame, Imgproc.COLOR_BGR2GRAY);

            Mat fallbackGrayscaleThreeChannelFrame = new Mat();
            Imgproc.cvtColor(
                    fallbackGrayscaleSingleChannelFrame,
                    fallbackGrayscaleThreeChannelFrame,
                    Imgproc.COLOR_GRAY2BGR
            );

            fallbackGrayscaleSingleChannelFrame.release();
            return fallbackGrayscaleThreeChannelFrame;
        }

        int inputColorDataLength = frameWidth * frameHeight * numberOfColorChannels;
        byte[] inputColorDataBlueGreenRed = new byte[inputColorDataLength];
        originalFrameMatrix.get(0, 0, inputColorDataBlueGreenRed);

        int grayscaleDataLength = frameWidth * frameHeight;
        byte[] outputGrayscaleData = new byte[grayscaleDataLength];

        GrayScaleConvertionUtils.convertBgrByteArrayToGrayscaleByteArray(
                inputColorDataBlueGreenRed,
                frameWidth,
                frameHeight,
                outputGrayscaleData
        );

        Mat grayscaleSingleChannelFrameMatrix = new Mat(frameHeight, frameWidth, CvType.CV_8UC1);
        grayscaleSingleChannelFrameMatrix.put(0, 0, outputGrayscaleData);

        Mat grayscaleThreeChannelFrameMatrix = new Mat();
        Imgproc.cvtColor(
                grayscaleSingleChannelFrameMatrix,
                grayscaleThreeChannelFrameMatrix,
                Imgproc.COLOR_GRAY2BGR
        );

        grayscaleSingleChannelFrameMatrix.release();

        return grayscaleThreeChannelFrameMatrix;
    }

    public static Mat applyCannyEdgesFilter(Mat originalFrameMatrix) {
        Mat grayscaleFrameMatrix = new Mat();
        Mat blurredFrameMatrix = new Mat();
        Mat edgesFrameMatrix = new Mat();
        Mat resultFrameMatrix = new Mat();

        Imgproc.cvtColor(originalFrameMatrix, grayscaleFrameMatrix, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(grayscaleFrameMatrix, blurredFrameMatrix, new Size(5, 5), 0);
        Imgproc.Canny(blurredFrameMatrix, edgesFrameMatrix, 50, 150);

        Imgproc.cvtColor(edgesFrameMatrix, resultFrameMatrix, Imgproc.COLOR_GRAY2BGR);

        grayscaleFrameMatrix.release();
        blurredFrameMatrix.release();
        edgesFrameMatrix.release();

        return resultFrameMatrix;
    }

    public static Mat applyOriginalWithCannyEdgesFilter(Mat originalFrameMatrix) {
        Mat grayscaleFrameMatrix = new Mat();
        Mat blurredFrameMatrix = new Mat();
        Mat edgesFrameMatrix = new Mat();
        Mat edgesColorFrameMatrix = new Mat();
        Mat resultFrameMatrix = new Mat();

        Imgproc.cvtColor(originalFrameMatrix, grayscaleFrameMatrix, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(grayscaleFrameMatrix, blurredFrameMatrix, new Size(5, 5), 0);
        Imgproc.Canny(blurredFrameMatrix, edgesFrameMatrix, 50, 150);
        Imgproc.cvtColor(edgesFrameMatrix, edgesColorFrameMatrix, Imgproc.COLOR_GRAY2BGR);

        Core.hconcat(Arrays.asList(originalFrameMatrix, edgesColorFrameMatrix), resultFrameMatrix);

        grayscaleFrameMatrix.release();
        blurredFrameMatrix.release();
        edgesFrameMatrix.release();
        edgesColorFrameMatrix.release();

        return resultFrameMatrix;
    }
}
