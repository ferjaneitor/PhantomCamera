package org.PhantomCamera.Camera;

public class OpenCVLoader {
    private static boolean loaded = false;

    public static void load() {
        if (loaded) return;

        System.load("C:\\Visual Studio\\Java\\PhantomCamera\\lib\\opencv_java4120.dll");
        System.out.println("OpenCV cargado OK (OpenCVLoader)");
        loaded = true;
    }
}
