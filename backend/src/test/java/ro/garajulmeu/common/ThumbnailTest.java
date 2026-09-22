package ro.garajulmeu.common;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The thumbnail the cards draw, checked on pixels rather than on byte counts: a
 * picture that comes out sideways, stretched or black still has a plausible
 * length.
 *
 * <p>The fixtures are painted in flat bands of primary colour, which survive JPEG
 * compression well away from their edges. Every assertion below therefore samples
 * the middle of a band and asks which colour dominates, never for an exact value.
 */
class ThumbnailTest {

	/** Orientation 6: the camera was turned a quarter clockwise. */
	private static final int TURNED = 6;

	@Test
	void makesASquareOfTheDeclaredSide() throws IOException {
		BufferedImage thumbnail = decode(Thumbnail.of(landscapeBytes("jpeg")).orElseThrow());

		assertThat(thumbnail.getWidth()).isEqualTo(Thumbnail.SIDE);
		assertThat(thumbnail.getHeight()).isEqualTo(Thumbnail.SIDE);
	}

	/**
	 * The middle of the picture, not the whole of it squeezed into a square: the
	 * fixture is red, green and blue in three vertical bands, and a centre crop of
	 * it is green from edge to edge.
	 */
	@Test
	void keepsTheMiddleOfAWidePhotograph() throws IOException {
		BufferedImage thumbnail = decode(Thumbnail.of(bands()).orElseThrow());

		assertThat(dominant(thumbnail, 20, 96)).isEqualTo('g');
		assertThat(dominant(thumbnail, 96, 96)).isEqualTo('g');
		assertThat(dominant(thumbnail, 170, 96)).isEqualTo('g');
	}

	/**
	 * A photograph taken with the telephone held sideways. The sensor stored it
	 * lying down and wrote a tag saying so; every viewer on earth turns it, ImageIO
	 * does not, and a thumbnail that ignored the tag would put the car on its side
	 * on the card.
	 *
	 * <p>The same bytes twice, differing only in that tag: left-and-right becomes
	 * top-and-bottom, which is what a quarter turn means and what no amount of
	 * scaling could produce by accident.
	 */
	@Test
	void turnsAPhotographTheWayTheCameraWasHeld() throws IOException {
		byte[] photograph = halves();

		BufferedImage asStored = decode(Thumbnail.of(photograph).orElseThrow());
		BufferedImage asSeen = decode(Thumbnail.of(withOrientation(photograph, TURNED))
				.orElseThrow());

		assertThat(dominant(asStored, 40, 96)).isEqualTo('r');
		assertThat(dominant(asStored, 150, 96)).isEqualTo('b');

		assertThat(dominant(asSeen, 96, 40)).isEqualTo('r');
		assertThat(dominant(asSeen, 96, 150)).isEqualTo('b');
	}

	/**
	 * A PNG is a photograph here as much as a JPEG is - the validator accepts both
	 * - and a transparent one must not come out with black corners, which is what
	 * JPEG does with an alpha channel it has nowhere to keep.
	 */
	@Test
	void flattensATransparentPngOntoWhite() throws IOException {
		BufferedImage thumbnail = decode(Thumbnail.of(transparent()).orElseThrow());

		assertThat(brightness(thumbnail, 10, 10)).isGreaterThan(200);
		assertThat(dominant(thumbnail, 96, 96)).isEqualTo('r');
	}

	/** Answers nothing rather than throwing: the callers both have a plan for that. */
	@Test
	void answersNothingForBytesThatAreNotAPicture() {
		assertThat(Thumbnail.of("MZ not a car".getBytes(StandardCharsets.UTF_8))).isEmpty();
		assertThat(Thumbnail.of(new byte[0])).isEmpty();
	}

	/**
	 * A truncated JPEG: recognised by its first bytes, then nothing there. Whether
	 * the reader salvages the part it got or gives up is its business - what this
	 * fixes is that neither answer reaches the caller as an exception, because the
	 * caller is an upload that must not fail over a thumbnail.
	 */
	@Test
	void doesNotThrowOnAPictureThatStopsPartWayThrough() {
		byte[] photograph = landscapeBytes("jpeg");
		byte[] cut = new byte[photograph.length / 3];
		System.arraycopy(photograph, 0, cut, 0, cut.length);

		assertThatCode(() -> Thumbnail.of(cut)).doesNotThrowAnyException();
	}

	/** The tag reader on its own, where a missing tag and a damaged one both land. */
	@Test
	void readsTheOrientationTagAndLeavesEverythingElseUpright() {
		byte[] photograph = landscapeBytes("jpeg");

		assertThat(JpegOrientation.of(photograph)).isEqualTo(JpegOrientation.UPRIGHT);
		assertThat(JpegOrientation.of(withOrientation(photograph, TURNED))).isEqualTo(TURNED);
		assertThat(JpegOrientation.of(withOrientation(photograph, 99)))
				.isEqualTo(JpegOrientation.UPRIGHT);
		assertThat(JpegOrientation.of(landscapeBytes("png"))).isEqualTo(JpegOrientation.UPRIGHT);
		assertThat(JpegOrientation.of(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }))
				.isEqualTo(JpegOrientation.UPRIGHT);
	}

	/** 400 by 200, left half red and right half blue. */
	private static byte[] halves() {
		return write(painted(400, 200, canvas -> {
			canvas.setColor(Color.RED);
			canvas.fillRect(0, 0, 200, 200);
			canvas.setColor(Color.BLUE);
			canvas.fillRect(200, 0, 200, 200);
		}), "jpeg");
	}

	/** 600 by 200 in three bands, so that a centre crop is entirely the middle one. */
	private static byte[] bands() {
		return write(painted(600, 200, canvas -> {
			canvas.setColor(Color.RED);
			canvas.fillRect(0, 0, 200, 200);
			canvas.setColor(Color.GREEN);
			canvas.fillRect(200, 0, 200, 200);
			canvas.setColor(Color.BLUE);
			canvas.fillRect(400, 0, 200, 200);
		}), "jpeg");
	}

	private static byte[] landscapeBytes(String format) {
		return write(painted(800, 400, canvas -> {
			canvas.setColor(Color.RED);
			canvas.fillRect(0, 0, 800, 400);
		}), format);
	}

	/** A red circle on nothing at all, the corners of which have no colour to keep. */
	private static byte[] transparent() {
		BufferedImage picture = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
		Graphics2D canvas = picture.createGraphics();

		try {
			canvas.setColor(Color.RED);
			canvas.fillOval(40, 40, 320, 320);
		} finally {
			canvas.dispose();
		}

		return write(picture, "png");
	}

	private static BufferedImage painted(int width, int height, Consumer<Graphics2D> paint) {
		BufferedImage picture = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D canvas = picture.createGraphics();

		try {
			paint.accept(canvas);
		} finally {
			canvas.dispose();
		}

		return picture;
	}

	private static byte[] write(BufferedImage picture, String format) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();

		try {
			ImageIO.write(picture, format, bytes);
		} catch (IOException exception) {
			throw new IllegalStateException("The fixture could not be written", exception);
		}

		return bytes.toByteArray();
	}

	/**
	 * An EXIF segment carrying one tag, spliced in behind the start-of-image
	 * marker. Written by hand because the point is to test the reader against the
	 * format rather than against another library's idea of it.
	 */
	private static byte[] withOrientation(byte[] photograph, int orientation) {
		byte[] segment = {
				(byte) 0xFF, (byte) 0xE1, 0x00, 0x22,
				'E', 'x', 'i', 'f', 0, 0,
				'M', 'M', 0, 42, 0, 0, 0, 8,
				0, 1,
				0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, (byte) orientation, 0, 0,
				0, 0, 0, 0,
		};

		byte[] tagged = new byte[photograph.length + segment.length];

		System.arraycopy(photograph, 0, tagged, 0, 2);
		System.arraycopy(segment, 0, tagged, 2, segment.length);
		System.arraycopy(photograph, 2, tagged, 2 + segment.length, photograph.length - 2);

		return tagged;
	}

	private static BufferedImage decode(byte[] bytes) throws IOException {
		BufferedImage picture = ImageIO.read(new ByteArrayInputStream(bytes));

		assertThat(picture).as("the thumbnail is not a readable image").isNotNull();
		return picture;
	}

	/** Which of the three channels wins at one point: 'r', 'g', 'b', or '?'. */
	private static char dominant(BufferedImage picture, int x, int y) {
		int pixel = picture.getRGB(x, y);
		int red = (pixel >> 16) & 0xFF;
		int green = (pixel >> 8) & 0xFF;
		int blue = pixel & 0xFF;

		if (red > green + 60 && red > blue + 60) {
			return 'r';
		}
		if (green > red + 60 && green > blue + 60) {
			return 'g';
		}
		if (blue > red + 60 && blue > green + 60) {
			return 'b';
		}
		return '?';
	}

	private static int brightness(BufferedImage picture, int x, int y) {
		int pixel = picture.getRGB(x, y);

		return Math.min(Math.min((pixel >> 16) & 0xFF, (pixel >> 8) & 0xFF), pixel & 0xFF);
	}
}
