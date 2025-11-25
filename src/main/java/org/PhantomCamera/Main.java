package org.PhantomCamera;

import org.PhantomCamera.Camera.Camera;
import org.PhantomCamera.Camera.CameraView;
import org.PhantomCamera.Camera.OpenCVLoader;

public class Main {
    public static void main(String[] args) {

        OpenCVLoader.load();   // carga única de OpenCV

        Camera cam = new Camera();

        if (!cam.isOpened()) {
            System.out.println("No se pudo iniciar la cámara.");
            return;
        }

        CameraView view = new CameraView(cam);
        view.start();
    }
}
