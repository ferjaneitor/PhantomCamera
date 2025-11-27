package org.PhantomCamera.Camera.StadisticsCamera;

import org.PhantomCamera.AprilTags.AprilTagFramePreProcessing;
import org.PhantomCamera.Camera.Camera;
import org.PhantomCamera.Stadistics.GrayScaleStatistics;
import org.PhantomCamera.Stadistics.GrayScaleStatistics.GrayscaleFrameStatistics;
import org.opencv.core.Mat;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class CameraGrayscaleStatisticsWindow {

    private final Camera cameraInstance;
    private final AprilTagFramePreProcessing aprilTagFramePreProcessing;

    private final JFrame windowFrame;
    private final JLabel imageDisplayLabel;

    private volatile boolean cameraViewIsRunning = false;

    public CameraGrayscaleStatisticsWindow() {
        cameraInstance = new Camera();
        if (!cameraInstance.isOpened()) {
            System.out.println("Camera could not be opened.");
            throw new IllegalStateException("Camera could not be opened.");
        }

        Mat initialColorFrameMatrix = cameraInstance.readFrame();
        if (initialColorFrameMatrix == null || initialColorFrameMatrix.empty()) {
            System.out.println("Could not read initial frame from camera.");
            cameraInstance.release();
            throw new IllegalStateException("Initial frame read failed.");
        }

        int frameWidthInPixels = initialColorFrameMatrix.cols();
        int frameHeightInPixels = initialColorFrameMatrix.rows();

        aprilTagFramePreProcessing =
                new AprilTagFramePreProcessing(frameWidthInPixels, frameHeightInPixels);

        windowFrame = new JFrame("Grayscale Camera View - Statistics");
        windowFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        imageDisplayLabel = new JLabel();
        windowFrame.getContentPane().add(imageDisplayLabel, BorderLayout.CENTER);

        windowFrame.setSize(frameWidthInPixels, frameHeightInPixels);
        windowFrame.setVisible(true);
    }

    public void start() {
        if (!cameraInstance.isOpened()) {
            System.out.println("The camera is not ready.");
            return;
        }

        cameraViewIsRunning = true;

        Thread cameraViewThread = new Thread(() -> {
            while (cameraViewIsRunning && windowFrame.isVisible()) {
                Mat currentColorFrameMatrix = cameraInstance.readFrame();

                if (currentColorFrameMatrix == null || currentColorFrameMatrix.empty()) {
                    System.out.println("Frame read failed, stopping.");
                    break;
                }

                Mat grayscaleFrameMatrix =
                        aprilTagFramePreProcessing.convertColorBgrFrameToGrayscale(currentColorFrameMatrix);

                int frameWidthInPixels = grayscaleFrameMatrix.cols();
                int frameHeightInPixels = grayscaleFrameMatrix.rows();
                int totalPixelCount = frameWidthInPixels * frameHeightInPixels;

                byte[] grayscaleFrameByteArray = new byte[totalPixelCount];
                grayscaleFrameMatrix.get(0, 0, grayscaleFrameByteArray);

                GrayscaleFrameStatistics grayscaleFrameStatistics =
                        GrayScaleStatistics.calculateGrayscaleFrameStatistics(
                                frameWidthInPixels,
                                frameHeightInPixels,
                                grayscaleFrameByteArray
                        );

                System.out.printf(
                        "Grayscale statistics - min=%d, max=%d, average=%.2f%n",
                        grayscaleFrameStatistics.minimumIntensityValue,
                        grayscaleFrameStatistics.maximumIntensityValue,
                        grayscaleFrameStatistics.averageIntensityValue
                );

                BufferedImage grayscaleBufferedImage =
                        convertOpenCvMatToBufferedImage(grayscaleFrameMatrix);

                if (grayscaleBufferedImage != null) {
                    ImageIcon imageIconToDisplay = new ImageIcon(grayscaleBufferedImage);
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
        });

        cameraViewThread.start();
    }

    public void stop() {
        cameraViewIsRunning = false;
    }

    private BufferedImage convertOpenCvMatToBufferedImage(Mat inputMatFrame) {
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

    public static void main(String[] args) {
        try {
            CameraGrayscaleStatisticsWindow cameraGrayscaleStatisticsWindow =
                    new CameraGrayscaleStatisticsWindow();
            cameraGrayscaleStatisticsWindow.start();
        } catch (Throwable throwable) {
            System.out.println("Unexpected error in CameraGrayscaleStatisticsWindow:");
            throwable.printStackTrace();
        }
    }
}
