///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.fazecast:jSerialComm:2.10.2
//SOURCES helper/ImageHelper.java
//SOURCES helper/PixelBlazeOutputExpanderHelper.java

import helper.PixelBlazeOutputExpanderHelper;

import java.io.File;
import java.io.IOException;

import static helper.ImageHelper.getImageData;
import static helper.ImageHelper.imageToMatrix;

/**
 * Example code to use a Pixelblaze Output Expander to send an 8*8 image to a LED matrix. This example is based on
 * PixelblazeOutputExpander.java, so please check its documentation first!
 * <p>
 * This example can be executed without sudo:
 * jbang PixelblazeOutputExpanderImageMatrix8x8.java
 */
public class PixelblazeOutputExpanderImageMatrix8x8 {

	private static final int CHANNEL_MATRIX = 1;
	private static final int BYTES_PER_PIXEL = 3;
	private static final int MATRIX_WIDTH = 8;
	private static final int MATRIX_HEIGHT = 8;
	private static final int NUMBER_OF_LEDS = MATRIX_HEIGHT * MATRIX_WIDTH;

	public static void main(String[] args) throws Exception {

		PixelBlazeOutputExpanderHelper helper = new PixelBlazeOutputExpanderHelper("/dev/ttyS0");
		helper.sendAllOff(CHANNEL_MATRIX, NUMBER_OF_LEDS);

		String imagePath;
		if (args.length != 0) {
			imagePath = args[0];
			if (!new File(imagePath).exists()) {
				System.err.println("Image does not exist at " + imagePath);
				System.exit(1);
			}
			showImage(helper, imagePath, 5000);
		} else {
			for (TestImage testImage : TestImage.values()) {
				showImage(helper, "data/" + testImage.fileName, testImage.duration);
			}
		}

		helper.sendAllOff(CHANNEL_MATRIX, NUMBER_OF_LEDS);
		helper.closePort();
	}

	private static void showImage(PixelBlazeOutputExpanderHelper helper, String imagePath, int duration)
			throws IOException, InterruptedException {

		// Get the bytes from the given image
		byte[] pixelsRgb = imageToMatrix(getImageData(imagePath, BYTES_PER_PIXEL, MATRIX_WIDTH, MATRIX_HEIGHT, 90, 10),
				BYTES_PER_PIXEL, MATRIX_WIDTH, MATRIX_HEIGHT);

		helper.sendColors(CHANNEL_MATRIX, pixelsRgb, false);
		Thread.sleep(duration);

	}

	private enum TestImage {
		HEART("heart_8_8.png", 2000),
		TUX("tux_8_8.png", 2000);

		private final String fileName;
		private final int duration;

		TestImage(String fileName, int duration) {
			this.fileName = fileName;
			this.duration = duration;
		}
	}
}
