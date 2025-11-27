package org.PhantomCamera.Camera;

import org.PhantomCamera.Camera.Filters.FilterMode;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class CameraView {

    private final Camera cameraInstance;
    private JFrame windowFrame;
    private JLabel imageDisplayLabel;
    private boolean cameraViewIsRunning = false;

    private volatile FilterMode currentFilterMode = FilterMode.ORIGINAL_WITH_CANNY_EDGES;

    private long lastFrameTimestampNanoseconds = System.nanoTime();
    private int renderedFrameCountSinceLastFpsUpdate = 0;
    private double currentFramesPerSecondValue = 0.0;

    public CameraView(Camera cameraInstance) {
        this.cameraInstance = cameraInstance;
        createApplicationWindowFrame();
    }

    private void createApplicationWindowFrame() {
        windowFrame = new JFrame("Camera View - OpenCV");
        windowFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        imageDisplayLabel = new JLabel();
        windowFrame.getContentPane().add(imageDisplayLabel, BorderLayout.CENTER);

        windowFrame.setSize(800, 600);
        windowFrame.setVisible(true);

        windowFrame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent keyEvent) {
                char pressedCharacter = keyEvent.getKeyChar();
                switch (pressedCharacter) {
                    case '1':
                        currentFilterMode = FilterMode.ORIGINAL;
                        System.out.println("Filter mode: ORIGINAL (1)");
                        break;
                    case '2':
                        currentFilterMode = FilterMode.GRAYSCALE;
                        System.out.println("Filter mode: GRAYSCALE (2)");
                        break;
                    case '3':
                        currentFilterMode = FilterMode.CANNY_EDGES;
                        System.out.println("Filter mode: CANNY_EDGES (3)");
                        break;
                    case '4':
                        currentFilterMode = FilterMode.ORIGINAL_WITH_CANNY_EDGES;
                        System.out.println("Filter mode: ORIGINAL_WITH_CANNY_EDGES (4)");
                        break;
                    default:
                        break;
                }
            }
        });
    }

    public void start() {
        if (!cameraInstance.isOpened()) {
            System.out.println("The camera is not ready.");
            return;
        }

        cameraViewIsRunning = true;

        Thread cameraViewThread = new Thread(() -> {
            while (cameraViewIsRunning && windowFrame.isVisible()) {
                Mat cameraFrame = cameraInstance.readFrame();

                if (cameraFrame == null) {
                    System.out.println("Could not read frame from camera.");
                    break;
                }

                updateFramesPerSecondMeasurement();

                Mat displayFrame = Filters.applyFilterByMode(cameraFrame, currentFilterMode);

                drawFramesPerSecondInformation(displayFrame);

                Image imageToDisplay = convertOpenCvMatToBufferedImage(displayFrame);
                if (imageToDisplay != null) {
                    ImageIcon imageIconToDisplay = new ImageIcon(imageToDisplay);
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

    private void updateFramesPerSecondMeasurement() {
        renderedFrameCountSinceLastFpsUpdate++;
        long currentTimeNanoseconds = System.nanoTime();
        long elapsedNanosecondsSinceLastFpsUpdate =
                currentTimeNanoseconds - lastFrameTimestampNanoseconds;

        if (elapsedNanosecondsSinceLastFpsUpdate >= 1_000_000_000L) {
            currentFramesPerSecondValue =
                    renderedFrameCountSinceLastFpsUpdate * 1e9 / elapsedNanosecondsSinceLastFpsUpdate;

            renderedFrameCountSinceLastFpsUpdate = 0;
            lastFrameTimestampNanoseconds = currentTimeNanoseconds;
        }
    }

    private void drawFramesPerSecondInformation(Mat frameToDrawOn) {
        String framesPerSecondInformationText =
                String.format("FPS: %.1f  |  Mode: %s",
                        currentFramesPerSecondValue,
                        currentFilterMode.name());

        Point textOriginPosition = new Point(10, 30);

        Scalar textColorScalar = new Scalar(0, 255, 0);

        Imgproc.putText(
                frameToDrawOn,
                framesPerSecondInformationText,
                textOriginPosition,
                Imgproc.FONT_HERSHEY_SIMPLEX,
                0.8,
                textColorScalar,
                2
        );
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

    public void stop() {
        cameraViewIsRunning = false;
    }
}
