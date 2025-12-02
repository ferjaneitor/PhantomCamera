package AprilTagTest;

import org.PhantomCamera.AprilTags.*;
import org.PhantomCamera.Camera.Camera;
import org.PhantomCamera.Stadistics.GrayScaleStatistics;
import org.PhantomCamera.Stadistics.GrayScaleStatistics.GrayscaleFrameStatistics;
import org.PhantomCamera.Stadistics.EdgeStadistics;
import org.PhantomCamera.Stadistics.EdgeStadistics.EdgeFrameStatistics;
import org.PhantomCamera.Utils.DrawingBoxes;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.List;

public class CameraGrayscaleEdgeAndComponentsTest {

    // Heuristics for filtering edge connected components that are likely to be AprilTags
    private static final double MAXIMUM_BOUNDING_BOX_AREA_FRACTION_OF_FRAME = 0.50;
    private static final double MINIMUM_COMPONENT_FILL_RATIO_WITHIN_BOUNDING_BOX = 0.05;

    private static final int gradientMagnitudeThresholdValue = 80;

    private static EdgeStadistics edgeStadistics = new EdgeStadistics();

    private static final int maximumNumberOfFramesToProcess = 200;

    // Thresholds for bounding boxes
    private static final int minimumComponentPixelCountThreshold = 2000;     // adjust depending on scene
    private static final int minimumBoundingBoxSideLengthInPixels = 20;      // minimum size in pixels

    // Global maximum observed component pixel count across all frames
    private static int maximumComponentPixelCount = 0;

    private static Camera cameraInstance = new Camera();

    private static AprilTagQuadrilateralFitter aprilTagQuadrilateralFitter =
            new AprilTagQuadrilateralFitter();

    public static void main(String[] args) {
        try {
            runCameraGrayscaleEdgeAndComponentsTest();
        } catch (Throwable throwable) {
            System.out.println("Unexpected error in CameraGrayscaleEdgeAndComponentsTest:");
            throwable.printStackTrace();
        }
    }

    private static void runCameraGrayscaleEdgeAndComponentsTest() {

        if (!cameraInstance.isOpened()) {
            System.out.println("Camera could not be opened.");
            return;
        }

        Mat initialColorFrameMatrix = cameraInstance.readFrame();
        if (initialColorFrameMatrix == null || initialColorFrameMatrix.empty()) {
            System.out.println("Could not read initial frame from camera.");
            cameraInstance.release();
            return;
        }

        int frameWidthInPixels = initialColorFrameMatrix.cols();
        int frameHeightInPixels = initialColorFrameMatrix.rows();
        int frameAreaInPixels = frameWidthInPixels * frameHeightInPixels;

        AprilTagFramePreProcessing aprilTagFramePreProcessing =
                new AprilTagFramePreProcessing(frameWidthInPixels, frameHeightInPixels);

        AprilTagEdgeDetector aprilTagEdgeDetector =
                new AprilTagEdgeDetector(
                        frameWidthInPixels,
                        frameHeightInPixels,
                        gradientMagnitudeThresholdValue
                );

        AprilTagEdgeComponentExtractor aprilTagEdgeComponentExtractor =
                new AprilTagEdgeComponentExtractor(
                        frameWidthInPixels,
                        frameHeightInPixels
                );

        AprilTagConvexHullCalculator aprilTagConvexHullCalculator =
                new AprilTagConvexHullCalculator();

        // Grayscale window
        JFrame grayscaleWindowFrame = new JFrame("Grayscale View - Statistics");
        grayscaleWindowFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel grayscaleImageDisplayLabel = new JLabel();
        grayscaleWindowFrame.getContentPane().add(grayscaleImageDisplayLabel, BorderLayout.CENTER);
        grayscaleWindowFrame.setSize(frameWidthInPixels, frameHeightInPixels);
        grayscaleWindowFrame.setLocation(100, 100);
        grayscaleWindowFrame.setVisible(true);

        // Edge window
        JFrame edgeWindowFrame = new JFrame("Edge Map View - Edges And Components Statistics");
        edgeWindowFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel edgeImageDisplayLabel = new JLabel();
        edgeWindowFrame.getContentPane().add(edgeImageDisplayLabel, BorderLayout.CENTER);
        edgeWindowFrame.setSize(frameWidthInPixels, frameHeightInPixels);
        edgeWindowFrame.setLocation(150 + frameWidthInPixels, 100);
        edgeWindowFrame.setVisible(true);

        for (int currentFrameIndex = 0;
             currentFrameIndex < maximumNumberOfFramesToProcess
                     && grayscaleWindowFrame.isVisible()
                     && edgeWindowFrame.isVisible();
             currentFrameIndex++) {

            Mat currentColorFrameMatrix = cameraInstance.readFrame();

            if (currentColorFrameMatrix == null || currentColorFrameMatrix.empty()) {
                System.out.println("Frame read failed, skipping.");
                continue;
            }

            Mat grayscaleFrameMatrix =
                    aprilTagFramePreProcessing.convertColorBgrFrameToGrayscale(currentColorFrameMatrix);

            int totalPixelCount = frameWidthInPixels * frameHeightInPixels;
            byte[] grayscaleFrameByteArray = new byte[totalPixelCount];
            grayscaleFrameMatrix.get(0, 0, grayscaleFrameByteArray);

            GrayscaleFrameStatistics grayscaleFrameStatistics =
                    GrayScaleStatistics.calculateGrayscaleFrameStatistics(
                            frameWidthInPixels,
                            frameHeightInPixels,
                            grayscaleFrameByteArray
                    );

            Mat edgeBinaryFrameMatrix =
                    aprilTagEdgeDetector.calculateEdgeBinaryFrameFromGrayscaleFrame(grayscaleFrameMatrix);

            EdgeFrameStatistics edgeFrameStatistics =
                    edgeStadistics.calculateEdgeFrameStatistics(
                            frameWidthInPixels,
                            frameHeightInPixels,
                            aprilTagEdgeDetector.getEdgeBinaryByteArray(),
                            aprilTagEdgeDetector.getGradientMagnitudeIntegerArray()
                    );

            List<EdgeConnectedComponent> edgeConnectedComponentList =
                    aprilTagEdgeComponentExtractor.extractEdgeConnectedComponentList(edgeBinaryFrameMatrix);

            int totalComponentCount = edgeConnectedComponentList.size();

            // Filter candidate components based on heuristics
            List<EdgeConnectedComponent> filteredCandidateEdgeConnectedComponentList =
                    new java.util.ArrayList<>();

            int filteredMaximumComponentPixelCount = 0;

            for (EdgeConnectedComponent currentEdgeConnectedComponent : edgeConnectedComponentList) {

                // Update global maximum observed component pixel count
                if (currentEdgeConnectedComponent.componentPixelCount > maximumComponentPixelCount) {
                    maximumComponentPixelCount = currentEdgeConnectedComponent.componentPixelCount;
                }

                // Filter 0: minimum component pixel count threshold
                if (currentEdgeConnectedComponent.componentPixelCount < minimumComponentPixelCountThreshold) {
                    continue;
                }

                int componentBoundingBoxAreaInPixels =
                        currentEdgeConnectedComponent.getComponentBoundingBoxAreaInPixels();

                if (componentBoundingBoxAreaInPixels <= 0) {
                    continue;
                }

                double componentBoundingBoxAreaFractionOfFrame =
                        (double) componentBoundingBoxAreaInPixels / (double) frameAreaInPixels;

                double componentFillRatioWithinBoundingBox =
                        currentEdgeConnectedComponent.getComponentFillRatioWithinBoundingBox();

                // Heuristic 1: discard components whose bounding box is too large relative to the frame
                if (componentBoundingBoxAreaFractionOfFrame > MAXIMUM_BOUNDING_BOX_AREA_FRACTION_OF_FRAME) {
                    continue;
                }

                // Heuristic 2: discard components with low fill ratio within their bounding box
                if (componentFillRatioWithinBoundingBox < MINIMUM_COMPONENT_FILL_RATIO_WITHIN_BOUNDING_BOX) {
                    continue;
                }

                // At this point the component passes all filters and is considered a candidate
                filteredCandidateEdgeConnectedComponentList.add(currentEdgeConnectedComponent);

                if (currentEdgeConnectedComponent.componentPixelCount > filteredMaximumComponentPixelCount) {
                    filteredMaximumComponentPixelCount = currentEdgeConnectedComponent.componentPixelCount;
                }

                // Detailed logging for each candidate component
                System.out.printf(
                        "[CandidateFilter] label=%d pixelCount=%d bboxArea=%d bboxAreaFraction=%.4f fillRatio=%.4f%n",
                        currentEdgeConnectedComponent.componentLabelValue,
                        currentEdgeConnectedComponent.componentPixelCount,
                        componentBoundingBoxAreaInPixels,
                        componentBoundingBoxAreaFractionOfFrame,
                        componentFillRatioWithinBoundingBox
                );
            }

            // Frame-level logging including candidate information
            System.out.printf(
                    "Frame %d - grayscale: min=%d, max=%d, average=%.2f | " +
                            "edges: edgePixelCount=%d, maximumGradientScaleValue=%d | " +
                            "components: totalComponentCount=%d, maximumComponentPixelCount=%d, " +
                            "candidateComponentCount=%d, filteredMaximumComponentPixelCount=%d%n",
                    currentFrameIndex,
                    grayscaleFrameStatistics.minimumIntensityValue,
                    grayscaleFrameStatistics.maximumIntensityValue,
                    grayscaleFrameStatistics.averageIntensityValue,
                    edgeFrameStatistics.edgePixelCount,
                    edgeFrameStatistics.maximumGradientScaleValue,
                    totalComponentCount,
                    maximumComponentPixelCount,
                    filteredCandidateEdgeConnectedComponentList.size(),
                    filteredMaximumComponentPixelCount
            );

            // Copies for overlay drawing without modifying the original processing matrices
            Mat grayscaleFrameForDisplayMatrix = grayscaleFrameMatrix.clone();
            Mat edgeBinaryFrameForDisplayMatrix = edgeBinaryFrameMatrix.clone();

            // For grayscale window we keep a single channel image
            drawGrayscaleStatisticsOnFrame(
                    grayscaleFrameForDisplayMatrix,
                    grayscaleFrameStatistics,
                    currentFrameIndex
            );

            // For edge window we convert to BGR so that colored overlays are visible
            Mat edgeBinaryFrameForDisplayColorMatrix = new Mat();
            Imgproc.cvtColor(
                    edgeBinaryFrameForDisplayMatrix,
                    edgeBinaryFrameForDisplayColorMatrix,
                    Imgproc.COLOR_GRAY2BGR
            );

            // Draw statistics, bounding boxes and convex hull using the filtered candidates
            DrawingBoxes.drawEdgeAndComponentStatisticsOnFrame(
                    edgeBinaryFrameForDisplayColorMatrix,
                    edgeFrameStatistics,
                    totalComponentCount,
                    filteredMaximumComponentPixelCount,
                    filteredCandidateEdgeConnectedComponentList.size(),
                    currentFrameIndex
            );

            DrawingBoxes.drawBoundingBoxesForCandidateComponents(
                    edgeBinaryFrameForDisplayColorMatrix,
                    filteredCandidateEdgeConnectedComponentList
            );

            DrawingBoxes.drawConvexHullForLargestSignificantEdgeConnectedComponent(
                    edgeBinaryFrameForDisplayColorMatrix,
                    filteredCandidateEdgeConnectedComponentList,
                    minimumComponentPixelCountThreshold,
                    aprilTagConvexHullCalculator,
                    aprilTagQuadrilateralFitter
            );

            BufferedImage grayscaleDisplayBufferedImage =
                    convertOpenCvMatToBufferedImage(grayscaleFrameForDisplayMatrix);

            BufferedImage edgeDisplayBufferedImage =
                    convertOpenCvMatToBufferedImage(edgeBinaryFrameForDisplayColorMatrix);

            if (grayscaleDisplayBufferedImage != null) {
                ImageIcon grayscaleImageIconToDisplay = new ImageIcon(grayscaleDisplayBufferedImage);
                grayscaleImageDisplayLabel.setIcon(grayscaleImageIconToDisplay);
                grayscaleImageDisplayLabel.repaint();
            }

            if (edgeDisplayBufferedImage != null) {
                ImageIcon edgeImageIconToDisplay = new ImageIcon(edgeDisplayBufferedImage);
                edgeImageDisplayLabel.setIcon(edgeImageIconToDisplay);
                edgeImageDisplayLabel.repaint();
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
                break;
            }
        }

        cameraInstance.release();
        grayscaleWindowFrame.dispose();
        edgeWindowFrame.dispose();
        System.out.println("CameraGrayscaleEdgeAndComponentsTest finished correctly.");
    }

    private static void drawGrayscaleStatisticsOnFrame(
            Mat grayscaleFrameMatrix,
            GrayscaleFrameStatistics grayscaleFrameStatistics,
            int currentFrameIndex
    ) {
        String grayscaleInformationTextHeader = String.format(
                "Frame %d - Grayscale",
                currentFrameIndex
        );

        String grayscaleInformationTextValues = String.format(
                "Min=%d Max=%d Avg=%.2f",
                grayscaleFrameStatistics.minimumIntensityValue,
                grayscaleFrameStatistics.maximumIntensityValue,
                grayscaleFrameStatistics.averageIntensityValue
        );

        org.opencv.core.Point firstTextOriginPosition = new org.opencv.core.Point(10, 30);
        org.opencv.core.Point secondTextOriginPosition = new org.opencv.core.Point(10, 60);

        org.opencv.core.Scalar textColorScalar = new org.opencv.core.Scalar(255);

        Imgproc.putText(
                grayscaleFrameMatrix,
                grayscaleInformationTextHeader,
                firstTextOriginPosition,
                Imgproc.FONT_HERSHEY_SIMPLEX,
                0.8,
                textColorScalar,
                2
        );

        Imgproc.putText(
                grayscaleFrameMatrix,
                grayscaleInformationTextValues,
                secondTextOriginPosition,
                Imgproc.FONT_HERSHEY_SIMPLEX,
                0.8,
                textColorScalar,
                2
        );
    }

    private static BufferedImage convertOpenCvMatToBufferedImage(Mat inputMatFrame) {
        int bufferedImageType;

        if (inputMatFrame.channels() == 1) {
            bufferedImageType = BufferedImage.TYPE_BYTE_GRAY;
        } else if (inputMatFrame.channels() == 3) {
            bufferedImageType = BufferedImage.TYPE_3BYTE_BGR;
        } else {
            System.out.println("Unsupported number of channels: " + inputMatFrame.channels());
            return null;
        }

        BufferedImage outputBufferedImage =
                new BufferedImage(inputMatFrame.width(), inputMatFrame.height(), bufferedImageType);

        byte[] bufferedImageRawDataBytes =
                ((DataBufferByte) outputBufferedImage.getRaster().getDataBuffer()).getData();

        inputMatFrame.get(0, 0, bufferedImageRawDataBytes);

        return outputBufferedImage;
    }
}