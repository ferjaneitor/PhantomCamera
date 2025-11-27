package AprilTagTest;

import org.PhantomCamera.AprilTags.AprilTagEdgeComponentExtractor;
import org.PhantomCamera.AprilTags.AprilTagEdgeDetector;
import org.PhantomCamera.AprilTags.AprilTagFramePreProcessing;
import org.PhantomCamera.AprilTags.EdgeConnectedComponent;
import org.PhantomCamera.Camera.Camera;
import org.PhantomCamera.Stadistics.GrayScaleStatistics;
import org.PhantomCamera.Stadistics.GrayScaleStatistics.GrayscaleFrameStatistics;
import org.PhantomCamera.Stadistics.EdgeStadistics;
import org.PhantomCamera.Stadistics.EdgeStadistics.EdgeFrameStatistics;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.List;

public class CameraGrayscaleEdgeAndComponentsTest {

    public static void main(String[] args) {
        try {
            runCameraGrayscaleEdgeAndComponentsTest();
        } catch (Throwable throwable) {
            System.out.println("Unexpected error in CameraGrayscaleEdgeAndComponentsTest:");
            throwable.printStackTrace();
        }
    }

    private static void runCameraGrayscaleEdgeAndComponentsTest() {

        Camera cameraInstance = new Camera();
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

        AprilTagFramePreProcessing aprilTagFramePreProcessing =
                new AprilTagFramePreProcessing(frameWidthInPixels, frameHeightInPixels);

        int gradientMagnitudeThresholdValue = 80;

        AprilTagEdgeDetector aprilTagEdgeDetector =
                new AprilTagEdgeDetector(
                        frameWidthInPixels,
                        frameHeightInPixels,
                        gradientMagnitudeThresholdValue
                );

        EdgeStadistics edgeStadistics = new EdgeStadistics();

        AprilTagEdgeComponentExtractor aprilTagEdgeComponentExtractor =
                new AprilTagEdgeComponentExtractor(
                        frameWidthInPixels,
                        frameHeightInPixels
                );

        // Ventana para escala de grises
        JFrame grayscaleWindowFrame = new JFrame("Grayscale View - Statistics");
        grayscaleWindowFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel grayscaleImageDisplayLabel = new JLabel();
        grayscaleWindowFrame.getContentPane().add(grayscaleImageDisplayLabel, BorderLayout.CENTER);
        grayscaleWindowFrame.setSize(frameWidthInPixels, frameHeightInPixels);
        grayscaleWindowFrame.setLocation(100, 100);
        grayscaleWindowFrame.setVisible(true);

        // Ventana para bordes
        JFrame edgeWindowFrame = new JFrame("Edge Map View - Edges And Components Statistics");
        edgeWindowFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel edgeImageDisplayLabel = new JLabel();
        edgeWindowFrame.getContentPane().add(edgeImageDisplayLabel, BorderLayout.CENTER);
        edgeWindowFrame.setSize(frameWidthInPixels, frameHeightInPixels);
        edgeWindowFrame.setLocation(150 + frameWidthInPixels, 100);
        edgeWindowFrame.setVisible(true);

        int maximumNumberOfFramesToProcess = 200;

        // Umbrales para los bounding boxes
        int minimumComponentPixelCountThreshold = 2000;     // ajusta según la escena
        int minimumBoundingBoxSideLengthInPixels = 20;     // tamaño mínimo en píxeles

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
            int maximumComponentPixelCount = 0;

            for (EdgeConnectedComponent edgeConnectedComponent
                    : edgeConnectedComponentList) {

                if (edgeConnectedComponent.componentPixelCount > maximumComponentPixelCount) {
                    maximumComponentPixelCount = edgeConnectedComponent.componentPixelCount;
                }
            }

            System.out.printf(
                    "Frame %d - grayscale: min=%d, max=%d, average=%.2f | " +
                            "edges: edgePixelCount=%d, maximumGradientScaleValue=%d | " +
                            "components: totalComponentCount=%d, maximumComponentPixelCount=%d%n",
                    currentFrameIndex,
                    grayscaleFrameStatistics.minimumIntensityValue,
                    grayscaleFrameStatistics.maximumIntensityValue,
                    grayscaleFrameStatistics.averageIntensityValue,
                    edgeFrameStatistics.edgePixelCount,
                    edgeFrameStatistics.maximumGradientScaleValue,
                    totalComponentCount,
                    maximumComponentPixelCount
            );

            // Copias para dibujar texto y cajas sin modificar las matrices originales de procesamiento
            Mat grayscaleFrameForDisplayMatrix = grayscaleFrameMatrix.clone();
            Mat edgeBinaryFrameForDisplayMatrix = edgeBinaryFrameMatrix.clone();

            drawGrayscaleStatisticsOnFrame(
                    grayscaleFrameForDisplayMatrix,
                    grayscaleFrameStatistics,
                    currentFrameIndex
            );

            drawEdgeAndComponentStatisticsOnFrame(
                    edgeBinaryFrameForDisplayMatrix,
                    edgeFrameStatistics,
                    totalComponentCount,
                    maximumComponentPixelCount,
                    currentFrameIndex
            );

            // Dibujar bounding boxes de componentes significativos
            drawBoundingBoxesForSignificantEdgeConnectedComponents(
                    edgeBinaryFrameForDisplayMatrix,
                    edgeConnectedComponentList,
                    minimumComponentPixelCountThreshold,
                    minimumBoundingBoxSideLengthInPixels
            );

            BufferedImage grayscaleDisplayBufferedImage =
                    convertOpenCvMatToBufferedImage(grayscaleFrameForDisplayMatrix);

            BufferedImage edgeDisplayBufferedImage =
                    convertOpenCvMatToBufferedImage(edgeBinaryFrameForDisplayMatrix);

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

        Point firstTextOriginPosition = new Point(10, 30);
        Point secondTextOriginPosition = new Point(10, 60);

        Scalar textColorScalar = new Scalar(255);

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

    private static void drawEdgeAndComponentStatisticsOnFrame(
            Mat edgeBinaryFrameMatrix,
            EdgeFrameStatistics edgeFrameStatistics,
            int totalComponentCount,
            int maximumComponentPixelCount,
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
                "Components=%d MaxComponentPixels=%d",
                totalComponentCount,
                maximumComponentPixelCount
        );

        Point firstTextOriginPosition = new Point(10, 30);
        Point secondTextOriginPosition = new Point(10, 60);
        Point thirdTextOriginPosition = new Point(10, 90);

        Scalar textColorScalar = new Scalar(255);

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

    private static void drawBoundingBoxesForSignificantEdgeConnectedComponents(
            Mat edgeBinaryFrameMatrix,
            List<EdgeConnectedComponent> edgeConnectedComponentList,
            int minimumComponentPixelCountThreshold,
            int minimumBoundingBoxSideLengthInPixels
    ) {
        Scalar rectangleColorScalar = new Scalar(255);
        int rectangleThicknessInPixels = 2;

        for (EdgeConnectedComponent edgeConnectedComponent : edgeConnectedComponentList) {

            int componentPixelCount = edgeConnectedComponent.componentPixelCount;

            if (componentPixelCount < minimumComponentPixelCountThreshold) {
                continue;
            }

            int minimumPixelXPosition = edgeConnectedComponent.minimumPixelXPosition;
            int maximumPixelXPosition = edgeConnectedComponent.maximumPixelXPosition;
            int minimumPixelYPosition = edgeConnectedComponent.minimumPixelYPosition;
            int maximumPixelYPosition = edgeConnectedComponent.maximumPixelYPosition;

            int boundingBoxWidthInPixels =
                    maximumPixelXPosition - minimumPixelXPosition + 1;
            int boundingBoxHeightInPixels =
                    maximumPixelYPosition - minimumPixelYPosition + 1;

            int minimumBoundingBoxSideLengthInPixelsCurrentComponent =
                    Math.min(boundingBoxWidthInPixels, boundingBoxHeightInPixels);

            if (minimumBoundingBoxSideLengthInPixelsCurrentComponent
                    < minimumBoundingBoxSideLengthInPixels) {
                continue;
            }

            Point topLeftCornerPoint =
                    new Point(minimumPixelXPosition, minimumPixelYPosition);

            Point bottomRightCornerPoint =
                    new Point(maximumPixelXPosition, maximumPixelYPosition);

            Imgproc.rectangle(
                    edgeBinaryFrameMatrix,
                    topLeftCornerPoint,
                    bottomRightCornerPoint,
                    rectangleColorScalar,
                    rectangleThicknessInPixels
            );
        }
    }
}
