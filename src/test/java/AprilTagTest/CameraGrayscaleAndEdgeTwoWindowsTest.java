package AprilTagTest;

import org.PhantomCamera.AprilTags.AprilTagConvexHullCalculator;
import org.PhantomCamera.AprilTags.AprilTagEdgeComponentExtractor;
import org.PhantomCamera.AprilTags.AprilTagEdgeDetector;
import org.PhantomCamera.AprilTags.AprilTagFramePreProcessing;
import org.PhantomCamera.AprilTags.EdgeConnectedComponent;
import org.PhantomCamera.AprilTags.PixelCoordinate;
import org.PhantomCamera.Camera.Camera;
import org.PhantomCamera.Stadistics.GrayScaleStatistics;
import org.PhantomCamera.Stadistics.GrayScaleStatistics.GrayscaleFrameStatistics;
import org.PhantomCamera.Stadistics.EdgeStadistics;
import org.PhantomCamera.Stadistics.EdgeStadistics.EdgeFrameStatistics;
import org.PhantomCamera.Utils.DrawingBoxes;
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
import java.util.ArrayList;

public class CameraGrayscaleAndEdgeTwoWindowsTest {

    public static void main(String[] args) {
        try {
            runCameraGrayscaleAndEdgeTest();
        } catch (Throwable throwable) {
            System.out.println("Unexpected error in CameraGrayscaleAndEdgeTwoWindowsTest:");
            throwable.printStackTrace();
        }
    }

    private static void runCameraGrayscaleAndEdgeTest() {

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

        int gradientMagnitudeThresholdValue = 60;

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

        AprilTagConvexHullCalculator aprilTagConvexHullCalculator =
                new AprilTagConvexHullCalculator();

        // Ventana para escala de grises
        JFrame grayscaleWindowFrame = new JFrame("Grayscale View - Statistics");
        grayscaleWindowFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel grayscaleImageDisplayLabel = new JLabel();
        grayscaleWindowFrame.getContentPane().add(grayscaleImageDisplayLabel, BorderLayout.CENTER);
        grayscaleWindowFrame.setSize(frameWidthInPixels, frameHeightInPixels);
        grayscaleWindowFrame.setLocation(100, 100);
        grayscaleWindowFrame.setVisible(true);

        // Ventana para bordes
        JFrame edgeWindowFrame = new JFrame("Edge Map View - Statistics And Convex Hull");
        edgeWindowFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel edgeImageDisplayLabel = new JLabel();
        edgeWindowFrame.getContentPane().add(edgeImageDisplayLabel, BorderLayout.CENTER);
        edgeWindowFrame.setSize(frameWidthInPixels, frameHeightInPixels);
        edgeWindowFrame.setLocation(150 + frameWidthInPixels, 100);
        edgeWindowFrame.setVisible(true);

        int maximumNumberOfFramesToProcess = 300;

        int minimumComponentPixelCountThreshold = 200; // ajusta según tu escena

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

            // Obtener componentes conectados de bordes
            List<EdgeConnectedComponent> edgeConnectedComponentList =
                    aprilTagEdgeComponentExtractor.extractEdgeConnectedComponentList(edgeBinaryFrameMatrix);

            System.out.printf(
                    "Frame %d - grayscale: min=%d, max=%d, average=%.2f | " +
                            "edges: edgePixelCount=%d, maximumGradientScaleValue=%d | " +
                            "components: totalComponentCount=%d%n",
                    currentFrameIndex,
                    grayscaleFrameStatistics.minimumIntensityValue,
                    grayscaleFrameStatistics.maximumIntensityValue,
                    grayscaleFrameStatistics.averageIntensityValue,
                    edgeFrameStatistics.edgePixelCount,
                    edgeFrameStatistics.maximumGradientScaleValue,
                    edgeConnectedComponentList.size()
            );

            // --- NEW: build candidate list and compute statistics ---

            // Lista de componentes candidatos (por ahora: los que tienen al menos N píxeles)
            List<EdgeConnectedComponent> candidateEdgeConnectedComponentList = new ArrayList<>();

            int maximumComponentPixelCount = 0;

            for (EdgeConnectedComponent currentEdgeConnectedComponent : edgeConnectedComponentList) {

                int currentComponentPixelCount = currentEdgeConnectedComponent.componentPixelCount;

                // Solo consideramos candidatos los componentes que tienen al menos este tamaño
                if (currentComponentPixelCount >= minimumComponentPixelCountThreshold) {
                    candidateEdgeConnectedComponentList.add(currentEdgeConnectedComponent);

                    if (currentComponentPixelCount > maximumComponentPixelCount) {
                        maximumComponentPixelCount = currentComponentPixelCount;
                    }
                }
            }

            // Densidades globales para debug
            double edgePixelDensity =
                    (double) edgeFrameStatistics.edgePixelCount / (double) totalPixelCount;

            double largestComponentPixelDensity =
                    (double) maximumComponentPixelCount / (double) totalPixelCount;

            System.out.printf(
                    "densities: edgePixelDensity=%.6f | largestComponentPixelDensity=%.6f%n",
                    edgePixelDensity,
                    largestComponentPixelDensity
            );

            // Log adicional: cuántos candidatos tenemos
            int candidateComponentCount = candidateEdgeConnectedComponentList.size();

            System.out.printf(
                    "component candidates above %d pixels: %d (maximumComponentPixelCount=%d)%n",
                    minimumComponentPixelCountThreshold,
                    candidateComponentCount,
                    maximumComponentPixelCount
            );

            Mat grayscaleFrameForDisplayMatrix = grayscaleFrameMatrix.clone();

            Mat edgeBinaryFrameForDisplayMatrix = edgeBinaryFrameMatrix.clone();

            Mat edgeFrameForDrawingMatrix = new Mat();
            Imgproc.cvtColor(
                    edgeBinaryFrameMatrix,
                    edgeFrameForDrawingMatrix,
                    Imgproc.COLOR_GRAY2BGR
            );

            // Dibujar overlay de estadísticas en la ventana de grises
            drawGrayscaleStatisticsOnFrame(
                    grayscaleFrameForDisplayMatrix,
                    grayscaleFrameStatistics,
                    currentFrameIndex
            );

            // Dibujar estadísticas de bordes + componentes en la ventana de bordes
            DrawingBoxes.drawEdgeAndComponentStatisticsOnFrame(
                    edgeFrameForDrawingMatrix,
                    edgeFrameStatistics,
                    edgeConnectedComponentList.size(),
                    maximumComponentPixelCount,
                    candidateComponentCount,
                    currentFrameIndex
            );

            // Dibujar bounding boxes verdes para todos los candidatos
            DrawingBoxes.drawBoundingBoxesForCandidateComponents(
                    edgeFrameForDrawingMatrix,
                    candidateEdgeConnectedComponentList
            );

            // Dibujar el convex hull del componente significativo más grande
            // Preferimos usar la lista de candidatos; si está vacía, usamos todos los componentes
            drawConvexHullForLargestSignificantEdgeConnectedComponent(
                    edgeFrameForDrawingMatrix,
                    candidateEdgeConnectedComponentList.isEmpty()
                            ? edgeConnectedComponentList
                            : candidateEdgeConnectedComponentList,
                    minimumComponentPixelCountThreshold,
                    aprilTagConvexHullCalculator
            );

            BufferedImage grayscaleDisplayBufferedImage =
                    convertOpenCvMatToBufferedImage(grayscaleFrameForDisplayMatrix);

            BufferedImage edgeDisplayBufferedImage =
                    convertOpenCvMatToBufferedImage(edgeFrameForDrawingMatrix);

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
        System.out.println("CameraGrayscaleAndEdgeTwoWindowsTest finished correctly.");
    }

    private static void drawGrayscaleStatisticsOnFrame(
            Mat grayscaleFrameMatrix,
            GrayscaleFrameStatistics grayscaleFrameStatistics,
            int currentFrameIndex
    ) {
        String grayscaleInformationTextLineOne = String.format(
                "Frame %d - Grayscale",
                currentFrameIndex
        );

        String grayscaleInformationTextLineTwo = String.format(
                "Min=%d Max=%d Avg=%.2f",
                grayscaleFrameStatistics.minimumIntensityValue,
                grayscaleFrameStatistics.maximumIntensityValue,
                grayscaleFrameStatistics.averageIntensityValue
        );

        // Log a consola lo mismo que se dibuja
        System.out.println("[GrayscaleOverlay] " + grayscaleInformationTextLineOne);
        System.out.println("[GrayscaleOverlay] " + grayscaleInformationTextLineTwo);

        Point firstTextOriginPosition = new Point(10, 30);
        Point secondTextOriginPosition = new Point(10, 60);

        Scalar textColorScalar = new Scalar(255);

        Imgproc.putText(
                grayscaleFrameMatrix,
                grayscaleInformationTextLineOne,
                firstTextOriginPosition,
                Imgproc.FONT_HERSHEY_SIMPLEX,
                0.8,
                textColorScalar,
                2
        );

        Imgproc.putText(
                grayscaleFrameMatrix,
                grayscaleInformationTextLineTwo,
                secondTextOriginPosition,
                Imgproc.FONT_HERSHEY_SIMPLEX,
                0.8,
                textColorScalar,
                2
        );
    }

    private static void drawEdgeStatisticsOnFrame(
            Mat edgeBinaryFrameMatrix,
            EdgeFrameStatistics edgeFrameStatistics,
            int currentFrameIndex
    ) {
        String edgeInformationTextLineOne = String.format(
                "Frame %d - Edge Map",
                currentFrameIndex
        );

        String edgeInformationTextLineTwo = String.format(
                "EdgePixels=%d MaxGradient=%d",
                edgeFrameStatistics.edgePixelCount,
                edgeFrameStatistics.maximumGradientScaleValue
        );

        // Log a consola lo mismo que se dibuja
        System.out.println("[EdgeOverlay] " + edgeInformationTextLineOne);
        System.out.println("[EdgeOverlay] " + edgeInformationTextLineTwo);

        Point firstTextOriginPosition = new Point(10, 30);
        Point secondTextOriginPosition = new Point(10, 60);

        Scalar textColorScalar = new Scalar(255);

        Imgproc.putText(
                edgeBinaryFrameMatrix,
                edgeInformationTextLineOne,
                firstTextOriginPosition,
                Imgproc.FONT_HERSHEY_SIMPLEX,
                0.8,
                textColorScalar,
                2
        );

        Imgproc.putText(
                edgeBinaryFrameMatrix,
                edgeInformationTextLineTwo,
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

    private static void drawConvexHullForLargestSignificantEdgeConnectedComponent(
            Mat edgeBinaryFrameMatrix,
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

        System.out.println("[ConvexHull] Convex hull point count: " + convexHullPixelPositionList.size());
        for (int currentIndex = 0; currentIndex < convexHullPixelPositionList.size(); currentIndex++) {
            PixelCoordinate pixelCoordinate = convexHullPixelPositionList.get(currentIndex);
            System.out.printf(
                    "[ConvexHull] Point %d: (%d, %d)%n",
                    currentIndex,
                    pixelCoordinate.xPixeldCoordinate,
                    pixelCoordinate.yPixeldCoordinate
            );
        }

        Scalar hullLineColorScalar = new Scalar(255);
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
                    edgeBinaryFrameMatrix,
                    firstPoint,
                    secondPoint,
                    hullLineColorScalar,
                    hullLineThicknessInPixels
            );
        }
    }
}
