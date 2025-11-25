package org.PhantomCamera.Camera;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

public class Camera {

    private VideoCapture camera;

    public Camera() {
        try {
            OpenCVLoader.load();  // carga única
        } catch (UnsatisfiedLinkError e) {
            System.out.println("Error al cargar OpenCV:");
            e.printStackTrace();
            return;
        }

        camera = new VideoCapture(0);
        if (!camera.isOpened()) {
            System.out.println("No se pudo abrir la cámara");
            camera = null;
        }
    }

    public boolean isOpened() {
        return camera != null && camera.isOpened();
    }

    public Mat readFrame() {
        if (!isOpened()) return null;
        Mat frame = new Mat();
        if (!camera.read(frame)) return null;
        return frame;
    }

    public void release() {
        if (camera != null) camera.release();
    }
}
