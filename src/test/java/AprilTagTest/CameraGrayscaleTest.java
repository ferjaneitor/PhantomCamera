package AprilTagTest;

import org.PhantomCamera.AprilTags.AprilTagEdgeDetector;
import org.PhantomCamera.AprilTags.AprilTagFramePreProcessing;
import org.PhantomCamera.Camera.Camera;
import org.PhantomCamera.Stadistics.GrayScaleStatistics;
import org.PhantomCamera.Stadistics.GrayScaleStatistics.GrayscaleFrameStatistics;
import org.PhantomCamera.Stadistics.EdgeStadistics;
import org.PhantomCamera.Stadistics.EdgeStadistics.EdgeFrameStatistics;
import org.opencv.core.Mat;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class CameraGrayscaleTest {

    public static void main(String[] args) {
        try {
            runCameraGrayscaleTest();
        } catch (Throwable throwable) {
            System.out.println("Unexpected error in CameraGrayscaleTest:");
            throwable.printStackTrace();
        }
    }

    private static void runCameraGrayscaleTest() {

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

        // Crear ventana simple para mostrar la imagen
        JFrame windowFrame = new JFrame("Camera Grayscale And Edge Statistics");
        windowFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel imageDisplayLabel = new JLabel();
        windowFrame.getContentPane().add(imageDisplayLabel, BorderLayout.CENTER);

        windowFrame.setSize(frameWidthInPixels, frameHeightInPixels);
        windowFrame.setVisible(true);

        int maximumNumberOfFramesToProcess = 300;

        for (int currentFrameIndex = 0;
             currentFrameIndex < maximumNumberOfFramesToProcess && windowFrame.isVisible();
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

            // Aquí decides qué Mat quieres ver: grayscale o edges
            Mat frameToDisplayMatrix = grayscaleFrameMatrix;
            // Si quieres ver bordes, cambia a:
            // Mat frameToDisplayMatrix = edgeBinaryFrameMatrix;

            BufferedImage displayBufferedImage =
                    convertOpenCvMatToBufferedImage(frameToDisplayMatrix);

            if (displayBufferedImage != null) {
                ImageIcon imageIconToDisplay = new ImageIcon(displayBufferedImage);
                imageDisplayLabel.setIcon(imageIconToDisplay);
                imageDisplayLabel.repaint();
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
                break;
            }
        }

        cameraInstance.release();
        windowFrame.dispose();
        System.out.println("CameraGrayscaleTest finished correctly.");
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
