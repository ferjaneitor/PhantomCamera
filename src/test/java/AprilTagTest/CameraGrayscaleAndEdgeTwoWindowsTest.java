package AprilTagTest;

import org.PhantomCamera.AprilTags.AprilTagEdgeDetector;
import org.PhantomCamera.AprilTags.AprilTagFramePreProcessing;
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

        int gradientMagnitudeThresholdValue = 60; // puedes ajustar este valor para ver más o menos bordes

        AprilTagEdgeDetector aprilTagEdgeDetector =
                new AprilTagEdgeDetector(
                        frameWidthInPixels,
                        frameHeightInPixels,
                        gradientMagnitudeThresholdValue
                );

        EdgeStadistics edgeStadistics = new EdgeStadistics();

        // Ventana para escala de grises
        JFrame grayscaleWindowFrame = new JFrame("Grayscale View - Statistics");
        grayscaleWindowFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel grayscaleImageDisplayLabel = new JLabel();
        grayscaleWindowFrame.getContentPane().add(grayscaleImageDisplayLabel, BorderLayout.CENTER);
        grayscaleWindowFrame.setSize(frameWidthInPixels, frameHeightInPixels);
        grayscaleWindowFrame.setLocation(100, 100);
        grayscaleWindowFrame.setVisible(true);

        // Ventana para bordes
        JFrame edgeWindowFrame = new JFrame("Edge Map View - Statistics");
        edgeWindowFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel edgeImageDisplayLabel = new JLabel();
        edgeWindowFrame.getContentPane().add(edgeImageDisplayLabel, BorderLayout.CENTER);
        edgeWindowFrame.setSize(frameWidthInPixels, frameHeightInPixels);
        edgeWindowFrame.setLocation(150 + frameWidthInPixels, 100);
        edgeWindowFrame.setVisible(true);

        int maximumNumberOfFramesToProcess = 300;

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

            // Log a consola para referencia
            System.out.printf(
                    "Frame %d - grayscale: min=%d, max=%d, average=%.2f | " +
                            "edges: edgePixelCount=%d, maximumGradientScaleValue=%d%n",
                    currentFrameIndex,
                    grayscaleFrameStatistics.minimumIntensityValue,
                    grayscaleFrameStatistics.maximumIntensityValue,
                    grayscaleFrameStatistics.averageIntensityValue,
                    edgeFrameStatistics.edgePixelCount,
                    edgeFrameStatistics.maximumGradientScaleValue
            );

            // Crear copias para dibujar texto sin modificar las matrices originales
            Mat grayscaleFrameForDisplayMatrix = grayscaleFrameMatrix.clone();
            Mat edgeBinaryFrameForDisplayMatrix = edgeBinaryFrameMatrix.clone();

            drawGrayscaleStatisticsOnFrame(
                    grayscaleFrameForDisplayMatrix,
                    grayscaleFrameStatistics,
                    currentFrameIndex
            );

            drawEdgeStatisticsOnFrame(
                    edgeBinaryFrameForDisplayMatrix,
                    edgeFrameStatistics,
                    currentFrameIndex
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

        Point firstTextOriginPosition = new Point(10, 30);
        Point secondTextOriginPosition = new Point(10, 60);

        // En escala de grises, 255 es blanco
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

        Point firstTextOriginPosition = new Point(10, 30);
        Point secondTextOriginPosition = new Point(10, 60);

        // En mapa de bordes binario, 255 es blanco
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
}
