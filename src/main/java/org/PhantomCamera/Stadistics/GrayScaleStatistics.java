package org.PhantomCamera.Stadistics;

public class GrayScaleStatistics {

    public static final class GrayscaleFrameStatistics {

        public final int minimumIntensityValue;
        public final int maximumIntensityValue;
        public final double averageIntensityValue;

        public GrayscaleFrameStatistics(
                int minimumIntensityValue,
                int maximumIntensityValue,
                double averageIntensityValue
        ) {
            this.minimumIntensityValue = minimumIntensityValue;
            this.maximumIntensityValue = maximumIntensityValue;
            this.averageIntensityValue = averageIntensityValue;
        }
    }

    public static GrayscaleFrameStatistics calculateGrayscaleFrameStatistics(
            int frameWidth,
            int frameHeight,
            byte[] outputGrayscaleByteArray
    ) {
        int totalPixelCount = frameWidth * frameHeight;

        int minimumIntensityValue = 255;
        int maximumIntensityValue = 0;
        long sumIntensityValue = 0L;

        for (int pixelIndex = 0; pixelIndex < totalPixelCount; pixelIndex++) {
            int intensityValue = outputGrayscaleByteArray[pixelIndex] & 0xFF;

            if (intensityValue < minimumIntensityValue) {
                minimumIntensityValue = intensityValue;
            }
            if (intensityValue > maximumIntensityValue) {
                maximumIntensityValue = intensityValue;
            }
            sumIntensityValue += intensityValue;
        }

        double averageIntensityValue =
                (double) sumIntensityValue / (double) totalPixelCount;

        return new GrayscaleFrameStatistics(
                minimumIntensityValue,
                maximumIntensityValue,
                averageIntensityValue
        );
    }
}
