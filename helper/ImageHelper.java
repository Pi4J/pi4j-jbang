package helper;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageHelper {

	/**
	 * Loads the given image into a byte array with RGB colors.
	 */
	public static byte[] getImageData(String imagePath, int bytesPerPixel, int width, int height) throws IOException {
		return getImageData(imagePath, bytesPerPixel, width, height, 0, 0);
	}

	public static byte[] getImageData(String imagePath, int bytesPerPixel, int width, int height, int rotation,
			int roundsOfDarkness) throws IOException {
		byte[] imageData = new byte[width * height * bytesPerPixel];

		// Open image
		File imgPath = new File(imagePath);
		BufferedImage bufferedImage = ImageIO.read(imgPath);
		if (bufferedImage.getWidth() > width || bufferedImage.getHeight() > height)
			throw new IllegalStateException(
					"Image " + imgPath.getName() + " (" + bufferedImage.getWidth() + "x" + bufferedImage.getHeight() +
							")" + " is larger than " + width + "x" + height + "!");

		// rotate
		if (rotation != 0)
			bufferedImage = rotateImage(bufferedImage, rotation);

		// Read color values for each pixel
		int pixelCounter = 0;
		for (int y = 0; y < width; y++) {
			for (int x = 0; x < height; x++) {
				int rgb = bufferedImage.getRGB(x, y);
				Color color = new Color(rgb, false);

				// darken the color
				for (int i = 0; i < roundsOfDarkness; i++) {
					color = color.darker();
				}

				int blue = color.getBlue();
				int green = color.getGreen();
				int red = color.getRed();
				imageData[(pixelCounter * bytesPerPixel)] = (byte) red;
				imageData[(pixelCounter * bytesPerPixel) + 1] = (byte) green;
				imageData[(pixelCounter * bytesPerPixel) + 2] = (byte) blue;
				pixelCounter++;
			}
		}

		return imageData;
	}

	/**
	 * Image is read to byte array pixel per pixel for each row to get one continuous line of data. But the matrix is
	 * wired in columns, first column down, second column up, third column down,...
	 * <p>
	 * So we need to "mix up" the image byte array to one that matches the coordinates on the matrix.
	 */
	public static byte[] imageToMatrix(byte[] imageData, int bytesPerPixel, int width, int height) {
		byte[] matrixData = new byte[imageData.length];

		int indexInImage = 0;
		for (int row = 0; row < width; row++) {
			for (int column = 0; column < height; column++) {
				int indexInMatrix = (column * 8) + (column % 2 == 0 ? row : 7 - row);
				//System.out.println("Row : " + row + " / column: " + column + " / index image : " + indexInImage + " / index matrix: " + indexInMatrix);
				matrixData[indexInMatrix * bytesPerPixel] = imageData[indexInImage * bytesPerPixel];
				matrixData[(indexInMatrix * bytesPerPixel) + 1] = imageData[(indexInImage * bytesPerPixel) + 1];
				matrixData[(indexInMatrix * bytesPerPixel) + 2] = imageData[(indexInImage * bytesPerPixel) + 2];
				indexInImage++;
			}
		}

		return matrixData;
	}

	public static BufferedImage rotateImage(BufferedImage buffImage, double angle) {
		double radian = Math.toRadians(angle);
		double sin = Math.abs(Math.sin(radian));
		double cos = Math.abs(Math.cos(radian));

		int width = buffImage.getWidth();
		int height = buffImage.getHeight();

		int nWidth = (int) Math.floor((double) width * cos + (double) height * sin);
		int nHeight = (int) Math.floor((double) height * cos + (double) width * sin);

		BufferedImage rotatedImage = new BufferedImage(nWidth, nHeight, BufferedImage.TYPE_INT_ARGB);

		Graphics2D graphics = rotatedImage.createGraphics();

		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		graphics.translate((nWidth - width) / 2, (nHeight - height) / 2);
		// rotation around the center point
		graphics.rotate(radian, (double) (width / 2), (double) (height / 2));
		graphics.drawImage(buffImage, 0, 0, null);
		graphics.dispose();

		return rotatedImage;
	}
}
