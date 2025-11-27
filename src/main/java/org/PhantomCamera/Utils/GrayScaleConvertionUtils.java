package org.PhantomCamera.Utils;

public class GrayScaleConvertionUtils {

    private GrayScaleConvertionUtils() {}

    public static void convertBgrByteArrayToGrayscaleByteArray(
            byte[] inputColorDataBlueGreenRed,
            int frameWith,
            int frameHieght,
            byte[] outputGrayScaleData
    ){

        int numbereOffPixels = frameWith * frameHieght;

        if (outputGrayScaleData.length < numbereOffPixels) {

            throw new IllegalArgumentException(
                    "Output Gray Scale buffer is too small. Expect at least  " +  numbereOffPixels + "bytes but found " + outputGrayScaleData.length
            );

        }

        if (inputColorDataBlueGreenRed.length < numbereOffPixels * 3) {
            throw new IllegalArgumentException(
                    "Input BGR is too small. Expect at least " + numbereOffPixels * 3 + "bytes but found " + inputColorDataBlueGreenRed.length
            );
        }

        for (int pixelIndex = 0; pixelIndex < numbereOffPixels; pixelIndex++) {

            int colorChannelBaseIndex = pixelIndex * 3;

            int colorChannelBlueValue = inputColorDataBlueGreenRed[colorChannelBaseIndex] & 0xFF;

            int colorChannelRedValue = inputColorDataBlueGreenRed[colorChannelBaseIndex + 1] & 0xFF;

            int colorChannelGreenValue = inputColorDataBlueGreenRed[colorChannelBaseIndex + 2] & 0xFF;

            int grayScaleValue = ( 77 * colorChannelRedValue + 150 * colorChannelGreenValue + 29 * colorChannelBlueValue ) >> 8 ;

            outputGrayScaleData[pixelIndex] = (byte) grayScaleValue;

        }

    }

}
