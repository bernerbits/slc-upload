package net.bernerbits.avolve.slcupload.ui.util;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;

import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.log4j.Logger;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wb.swt.SWTResourceManager;

import sun.awt.shell.ShellFolder;

public class FileIcons {
	private static Logger logger = Logger.getLogger(FileIcons.class);

	public static ImageData convertToSWT(BufferedImage bufferedImage) {
		if (bufferedImage.getColorModel() instanceof DirectColorModel) {
			DirectColorModel colorModel = (DirectColorModel) bufferedImage.getColorModel();
			PaletteData palette = new PaletteData(colorModel.getRedMask(), colorModel.getGreenMask(),
					colorModel.getBlueMask());
			ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(),
					colorModel.getPixelSize(), palette);
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					int rgb = bufferedImage.getRGB(x, y);
					int pixel = palette.getPixel(new RGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
					data.setPixel(x, y, pixel);
					if (colorModel.hasAlpha()) {
						data.setAlpha(x, y, (rgb >> 24) & 0xFF);
					}
				}
			}
			return data;
		} else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
			IndexColorModel colorModel = (IndexColorModel) bufferedImage.getColorModel();
			int size = colorModel.getMapSize();
			byte[] reds = new byte[size];
			byte[] greens = new byte[size];
			byte[] blues = new byte[size];
			colorModel.getReds(reds);
			colorModel.getGreens(greens);
			colorModel.getBlues(blues);
			RGB[] rgbs = new RGB[size];
			for (int i = 0; i < rgbs.length; i++) {
				rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
			}
			PaletteData palette = new PaletteData(rgbs);
			ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(),
					colorModel.getPixelSize(), palette);
			data.transparentPixel = colorModel.getTransparentPixel();
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[1];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					raster.getPixel(x, y, pixelArray);
					data.setPixel(x, y, pixelArray[0]);
				}
			}
			return data;
		} else {
			throw new IllegalArgumentException(bufferedImage.getColorModel().getClass()
					+ " is a supported color model.");
		}
	}

	public static Image getFolderImage() throws IOException {
		File tempDir;
		try {
			tempDir = File.createTempFile("tmp", "file");
			tempDir.delete();
			ImageIcon systemIcon = (ImageIcon) FileSystemView.getFileSystemView()
					.getSystemIcon(tempDir.getParentFile());
			java.awt.Image image = systemIcon.getImage();
			return convertToSWT(image);
		} catch (Throwable e) {
			return SWTResourceManager.getImage(FileIcons.class, "/file_icon.png");
		}
	}

	public static Image getDefaultFileImage() {
		File tempFile;
		try {
			tempFile = File.createTempFile("tmp", "");
			try {
				return getFileImage(tempFile);
			} finally {
				tempFile.delete();
			}
		} catch (Throwable e) {
			return SWTResourceManager.getImage(FileIcons.class, "file_icon.png");
		}
	}

	public static Image getFileImage(File file) throws IOException {
		try {
			ShellFolder sf = ShellFolder.getShellFolder(file);

			ImageIcon systemIcon = new ImageIcon(sf.getIcon(true), sf.getFolderType());
			java.awt.Image image = systemIcon.getImage();
			return convertToSWT(image);
		} catch (Throwable e) {
			return SWTResourceManager.getImage(FileIcons.class, "file_icon.png");
		}
	}

	public static Image getBucketImage(int size) throws IOException {
		String bucketSvg = "<?xml version=\"1.0\" encoding=\"utf-16\"?>\n"
				+ "<!-- Generator: Adobe Illustrator 14.0.0, SVG Export Plug-In . SVG Version: 6.00 Build 43363)  -->\n"
				+ "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n"
				+ "<svg version=\"1.1\" id=\"Layer_1\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" x=\"0px\" y=\"0px\"\n"
				+ "	 width=\"70px\" height=\"70px\" viewBox=\"0 0 70 70\" enable-background=\"new 0 0 70 70\" xml:space=\"preserve\">\n"
				+ "<g>\n"
				+ "	<g>\n"
				+ "		<g>\n"
				+ "			<g>\n"
				+ "				<path fill-rule=\"evenodd\" clip-rule=\"evenodd\" fill=\"#146EB4\" d=\"M63.95,17.287c0,4.006-12.963,7.238-28.95,7.238\n"
				+ "					c-15.988,0-28.951-3.232-28.951-7.238l9.65,43.838c0,2.672,8.638,4.826,19.301,4.826s19.3-2.154,19.3-4.826l0,0L63.95,17.287z\"\n"
				+ "					/>\n"
				+ "			</g>\n"
				+ "			<g>\n"
				+ "				<path fill-rule=\"evenodd\" clip-rule=\"evenodd\" fill=\"#146EB4\" d=\"M63.95,14.287c0-4.004-12.963-7.238-28.95-7.238\n"
				+ "					c-15.988,0-28.951,3.234-28.951,7.238c0,4.006,12.963,7.238,28.951,7.238C50.987,21.525,63.95,18.293,63.95,14.287L63.95,14.287\n"
				+ "					z\"/>\n"
				+ "			</g>\n"
				+ "		</g>\n"
				+ "	</g>\n"
				+ "	<g>\n"
				+ "		<circle fill-rule=\"evenodd\" clip-rule=\"evenodd\" fill=\"#FFFFFF\" cx=\"24.743\" cy=\"53.136\" r=\"5.832\"/>\n"
				+ "	</g>\n"
				+ "	<rect x=\"28.454\" y=\"29.867\" fill-rule=\"evenodd\" clip-rule=\"evenodd\" fill=\"#FFFFFF\" width=\"11.829\" height=\"11.83\"/>\n"
				+ "	<polygon fill-rule=\"evenodd\" clip-rule=\"evenodd\" fill=\"#FFFFFF\" points=\"49.668,58.824 37.977,58.824 43.823,47.955 	\"/>\n"
				+ "</g>\n" + "</svg>";
		Transcoder t = new PNGTranscoder();

		// Set the transcoding hints.
		t.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, new Float(size));
		t.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, new Float(size));
		TranscoderInput input = new TranscoderInput(new StringReader(bucketSvg));

		ByteArrayOutputStream ostream = null;
		try {
			// Create the transcoder output.
			ostream = new ByteArrayOutputStream();
			TranscoderOutput output = new TranscoderOutput(ostream);

			// Save the image.
			t.transcode(input, output);

			// Flush and close the stream.
			ostream.flush();
			ostream.close();
		} catch (Exception ex) {
			logger.warn(ex.getMessage(), ex);
		}

		// Convert the byte stream into an image.
		byte[] imgData = ostream == null ? new byte[0] : ostream.toByteArray();

		return new Image(Display.getCurrent(), new ImageData(new ByteArrayInputStream(imgData)));
	}

	private static Image convertToSWT(java.awt.Image image) {
		if (image instanceof BufferedImage) {
			return new Image(Display.getCurrent(), convertToSWT((BufferedImage) image));
		}
		int width = image.getWidth(null);
		int height = image.getHeight(null);
		BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bufferedImage.createGraphics();
		g2d.drawImage(image, 0, 0, null);
		g2d.dispose();
		return new Image(Display.getCurrent(), convertToSWT(bufferedImage));
	}
}
