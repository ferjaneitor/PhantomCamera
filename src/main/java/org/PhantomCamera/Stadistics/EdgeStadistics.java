package org.PhantomCamera.Stadistics;

public final class EdgeStadistics {

    public static  final  class EdgeFrameStatistics {

        public final int edgePixelCount;

        public final int maximumGradientScaleValue;

        public EdgeFrameStatistics(
                int edgePixelCount,
                int maximumGradientScaleValue
        ) {
            this.edgePixelCount = edgePixelCount;
            this.maximumGradientScaleValue = maximumGradientScaleValue;
        }

    }
    /**
     * Computes basic statistics on the current edge frame, useful for debugging and tuning.
     */
    public EdgeFrameStatistics calculateEdgeFrameStatistics(
            int frameWidthInPixels,
            int frameHeightInPixels,
            byte[] edgeBinaryByteArray,
            int[] gradientMagnitudeIntegerArray
    ) {
        int totalPixelCount = frameWidthInPixels * frameHeightInPixels;

        int edgePixelCount = 0;
        int maximumGradientMagnitudeValue = 0;

        for (int pixelIndex = 0; pixelIndex < totalPixelCount; pixelIndex++) {
            if ((edgeBinaryByteArray[pixelIndex] & 0xFF) != 0) {
                edgePixelCount++;
            }

            int gradientMagnitudeValue = gradientMagnitudeIntegerArray[pixelIndex];
            if (gradientMagnitudeValue > maximumGradientMagnitudeValue) {
                maximumGradientMagnitudeValue = gradientMagnitudeValue;
            }
        }

        return new EdgeFrameStatistics(
                edgePixelCount,
                maximumGradientMagnitudeValue
        );
    }


}
