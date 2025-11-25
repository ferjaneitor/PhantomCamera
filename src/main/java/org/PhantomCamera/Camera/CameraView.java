package org.PhantomCamera.Camera;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Arrays;

public class CameraView {

    private final Camera camera;
    private JFrame frame;
    private JLabel imageLabel;
    private boolean running = false;

    // Modos de filtro
    private enum FilterMode {
        ORIGINAL,
        GRAY,
        CANNY,
        ORIGINAL_PLUS_CANNY
    }

    private volatile FilterMode mode = FilterMode.ORIGINAL_PLUS_CANNY;

    // Para calcular FPS
    private long lastTimeNs = System.nanoTime();
    private int framesCount = 0;
    private double currentFps = 0.0;

    public CameraView(Camera camera) {
        this.camera = camera;
        createWindow();
    }

    // Crear ventana Swing
    private void createWindow() {
        frame = new JFrame("Camera View - OpenCV");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        imageLabel = new JLabel();
        frame.getContentPane().add(imageLabel, BorderLayout.CENTER);

        frame.setSize(800, 600);
        frame.setVisible(true);

        // Escuchar teclas: 1, 2, 3, 4
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                char ch = e.getKeyChar();
                switch (ch) {
                    case '1':
                        mode = FilterMode.ORIGINAL;
                        System.out.println("Filtro: ORIGINAL (1)");
                        break;
                    case '2':
                        mode = FilterMode.GRAY;
                        System.out.println("Filtro: GRAY (2)");
                        break;
                    case '3':
                        mode = FilterMode.CANNY;
                        System.out.println("Filtro: CANNY (3)");
                        break;
                    case '4':
                        mode = FilterMode.ORIGINAL_PLUS_CANNY;
                        System.out.println("Filtro: ORIGINAL + CANNY (4)");
                        break;
                }
            }
        });
    }

    public void start() {
        if (!camera.isOpened()) {
            System.out.println("La cámara no está lista.");
            return;
        }

        running = true;

        Thread thread = new Thread(() -> {
            while (running && frame.isVisible()) {
                Mat matFrame = camera.readFrame();

                if (matFrame == null) {
                    System.out.println("No se pudo leer frame");
                    break;
                }

                // Actualizar FPS (cálculo)
                updateFps();

                // Aplicar filtros según el modo actual
                Mat displayMat = applyFilters(matFrame);

                // Dibujar FPS (y modo) sobre el frame
                drawFps(displayMat);

                // Convertir a imagen de Java y mostrar
                Image img = matToBufferedImage(displayMat);
                if (img != null) {
                    ImageIcon icon = new ImageIcon(img);
                    imageLabel.setIcon(icon);
                    imageLabel.repaint();
                }

                try {
                    Thread.sleep(10); // pequeño delay
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }

            camera.release();
            frame.dispose();
        });

        thread.start();
    }

    /**
     * Aplica filtros según el modo actual:
     *  - ORIGINAL
     *  - GRAY
     *  - CANNY
     *  - ORIGINAL_PLUS_CANNY
     */
    private Mat applyFilters(Mat original) {
        Mat result = new Mat();

        switch (mode) {
            case ORIGINAL:
                // Solo copiamos el original
                original.copyTo(result);
                break;

            case GRAY: {
                Mat gray = new Mat();
                Imgproc.cvtColor(original, gray, Imgproc.COLOR_BGR2GRAY);

                // Pasamos de 1 canal a 3 para mostrar bien
                Imgproc.cvtColor(gray, result, Imgproc.COLOR_GRAY2BGR);
                gray.release();
                break;
            }

            case CANNY: {
                Mat gray = new Mat();
                Mat blurred = new Mat();
                Mat edges = new Mat();

                Imgproc.cvtColor(original, gray, Imgproc.COLOR_BGR2GRAY);
                Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);
                Imgproc.Canny(blurred, edges, 50, 150);

                // edges (1 canal) -> 3 canales para visualizar
                Imgproc.cvtColor(edges, result, Imgproc.COLOR_GRAY2BGR);

                gray.release();
                blurred.release();
                edges.release();
                break;
            }

            case ORIGINAL_PLUS_CANNY: {
                Mat gray = new Mat();
                Mat blurred = new Mat();
                Mat edges = new Mat();
                Mat edgesColor = new Mat();

                Imgproc.cvtColor(original, gray, Imgproc.COLOR_BGR2GRAY);
                Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);
                Imgproc.Canny(blurred, edges, 50, 150);
                Imgproc.cvtColor(edges, edgesColor, Imgproc.COLOR_GRAY2BGR);

                Core.hconcat(Arrays.asList(original, edgesColor), result);

                gray.release();
                blurred.release();
                edges.release();
                edgesColor.release();
                break;
            }
        }

        return result;
    }

    // Calcula FPS (actualiza currentFps una vez por segundo)
    private void updateFps() {
        framesCount++;
        long now = System.nanoTime();
        long diff = now - lastTimeNs;

        if (diff >= 1_000_000_000L) { // 1 segundo
            currentFps = framesCount * 1e9 / diff;
            framesCount = 0;
            lastTimeNs = now;
        }
    }

    // Dibuja el texto de FPS y modo sobre el Mat
    private void drawFps(Mat mat) {
        String text = String.format("FPS: %.1f  |  Mode: %s", currentFps, mode.name());

        // Posición del texto
        Point org = new Point(10, 30); // x=10, y=30

        // Color verde en BGR, grosor 2
        Scalar color = new Scalar(0, 255, 0);

        Imgproc.putText(
                mat,
                text,
                org,
                Imgproc.FONT_HERSHEY_SIMPLEX,
                0.8,
                color,
                2
        );
    }

    // Conversión de Mat (OpenCV) → BufferedImage (Java)
    private BufferedImage matToBufferedImage(Mat mat) {
        int type;

        if (mat.channels() == 1) {
            type = BufferedImage.TYPE_BYTE_GRAY;
        } else if (mat.channels() == 3) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        } else {
            System.out.println("Número de canales no soportado: " + mat.channels());
            return null;
        }

        BufferedImage image = new BufferedImage(mat.width(), mat.height(), type);
        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        mat.get(0, 0, data);

        return image;
    }

    public void stop() {
        running = false;
    }
}
