/**
 * TGAReader.java
 *
 * Copyright (c) 2014 Kenji Sasaki
 * Released under the MIT license.
 * https://github.com/npedotnet/TGAReader/blob/master/LICENSE
 *
 * English document
 * https://github.com/npedotnet/TGAReader/blob/master/README.md
 *
 * Japanese document
 * http://3dtech.jp/wiki/index.php?TGAReader
 *
 */

package cleargl;

import java.io.IOException;

public final class TGAReader {

	public static final Order ARGB = new Order(16, 8, 0, 24);
	public static final Order ABGR = new Order(0, 8, 16, 24);

	public static int getWidth(final byte[] buffer) {
		return (buffer[12] & 0xFF) | (buffer[13] & 0xFF) << 8;
	}

	public static int getHeight(final byte[] buffer) {
		return (buffer[14] & 0xFF) | (buffer[15] & 0xFF) << 8;
	}

	public static int[] read(final byte[] buffer, final Order order) throws IOException {

		// header
		// int idFieldLength = buffer[0] & 0xFF;
		// int colormapType = buffer[1] & 0xFF;
		final int type = buffer[2] & 0xFF;
		final int colormapOrigin = (buffer[3] & 0xFF) | (buffer[4] & 0xFF) << 8;
		final int colormapLength = (buffer[5] & 0xFF) | (buffer[6] & 0xFF) << 8;
		final int colormapDepth = buffer[7] & 0xFF;
		// int originX = (buffer[8] & 0xFF) | (buffer[9] & 0xFF) << 8; //
		// unsupported
		// int originY = (buffer[10] & 0xFF) | (buffer[11] & 0xFF) << 8; //
		// unsupported
		final int width = getWidth(buffer);
		final int height = getHeight(buffer);
		final int depth = buffer[16] & 0xFF;
		final int descriptor = buffer[17] & 0xFF;

		int[] pixels = null;

		// data
		switch (type) {
			case COLORMAP: {
				final int imageDataOffset = 18 + (colormapDepth / 8) * colormapLength;
				pixels = createPixelsFromColormap(width, height, colormapDepth, buffer, imageDataOffset, buffer,
						colormapOrigin, descriptor, order);
			}
				break;
			case RGB:
				pixels = createPixelsFromRGB(width, height, depth, buffer, 18, descriptor, order);
				break;
			case GRAYSCALE:
				pixels = createPixelsFromGrayscale(width, height, depth, buffer, 18, descriptor, order);
				break;
			case COLORMAP_RLE: {
				final int imageDataOffset = 18 + (colormapDepth / 8) * colormapLength;
				final byte[] decodeBuffer = decodeRLE(width, height, depth, buffer, imageDataOffset);
				pixels = createPixelsFromColormap(width, height, colormapDepth, decodeBuffer, 0, buffer, colormapOrigin,
						descriptor, order);
			}
				break;
			case RGB_RLE: {
				final byte[] decodeBuffer = decodeRLE(width, height, depth, buffer, 18);
				pixels = createPixelsFromRGB(width, height, depth, decodeBuffer, 0, descriptor, order);
			}
				break;
			case GRAYSCALE_RLE: {
				final byte[] decodeBuffer = decodeRLE(width, height, depth, buffer, 18);
				pixels = createPixelsFromGrayscale(width, height, depth, decodeBuffer, 0, descriptor, order);
			}
				break;
			default:
				throw new IOException("Unsupported image type: " + type);
		}

		return pixels;

	}

	private static final int COLORMAP = 1;
	private static final int RGB = 2;
	private static final int GRAYSCALE = 3;
	private static final int COLORMAP_RLE = 9;
	private static final int RGB_RLE = 10;
	private static final int GRAYSCALE_RLE = 11;

	private static final int RIGHT_ORIGIN = 0x10;
	private static final int UPPER_ORIGIN = 0x20;

	private static byte[] decodeRLE(final int width, final int height, final int depth, final byte[] buffer,
			int offset) {
		final int elementCount = depth / 8;
		final byte[] elements = new byte[elementCount];
		final int decodeBufferLength = elementCount * width * height;
		final byte[] decodeBuffer = new byte[decodeBufferLength];
		int decoded = 0;
		while (decoded < decodeBufferLength) {
			final int packet = buffer[offset++] & 0xFF;
			if ((packet & 0x80) != 0) { // RLE
				for (int i = 0; i < elementCount; i++) {
					elements[i] = buffer[offset++];
				}
				final int count = (packet & 0x7F) + 1;
				for (int i = 0; i < count; i++) {
					for (int j = 0; j < elementCount; j++) {
						decodeBuffer[decoded++] = elements[j];
					}
				}
			} else { // RAW
				final int count = (packet + 1) * elementCount;
				for (int i = 0; i < count; i++) {
					decodeBuffer[decoded++] = buffer[offset++];
				}
			}
		}
		return decodeBuffer;
	}

	private static int[] createPixelsFromColormap(final int width, final int height, final int depth,
			final byte[] bytes, final int offset, final byte[] palette, final int colormapOrigin, final int descriptor,
			final Order order) throws IOException {
		int[] pixels = null;
		final int rs = order.redShift;
		final int gs = order.greenShift;
		final int bs = order.blueShift;
		final int as = order.alphaShift;
		switch (depth) {
			case 24:
				pixels = new int[width * height];
				if ((descriptor & RIGHT_ORIGIN) != 0) {
					if ((descriptor & UPPER_ORIGIN) != 0) {
						// UpperRight
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int colormapIndex = bytes[offset + width * i + j] & 0xFF - colormapOrigin;
								int color = 0xFFFFFFFF;
								if (colormapIndex >= 0) {
									final int index = 3 * colormapIndex + 18;
									final int b = palette[index + 0] & 0xFF;
									final int g = palette[index + 1] & 0xFF;
									final int r = palette[index + 2] & 0xFF;
									final int a = 0xFF;
									color = (r << rs) | (g << gs) | (b << bs) | (a << as);
								}
								pixels[width * i + (width - j - 1)] = color;
							}
						}
					} else {
						// LowerRight
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int colormapIndex = bytes[offset + width * i + j] & 0xFF - colormapOrigin;
								int color = 0xFFFFFFFF;
								if (colormapIndex >= 0) {
									final int index = 3 * colormapIndex + 18;
									final int b = palette[index + 0] & 0xFF;
									final int g = palette[index + 1] & 0xFF;
									final int r = palette[index + 2] & 0xFF;
									final int a = 0xFF;
									color = (r << rs) | (g << gs) | (b << bs) | (a << as);
								}
								pixels[width * (height - i - 1) + (width - j - 1)] = color;
							}
						}
					}
				} else {
					if ((descriptor & UPPER_ORIGIN) != 0) {
						// UpperLeft
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int colormapIndex = bytes[offset + width * i + j] & 0xFF - colormapOrigin;
								int color = 0xFFFFFFFF;
								if (colormapIndex >= 0) {
									final int index = 3 * colormapIndex + 18;
									final int b = palette[index + 0] & 0xFF;
									final int g = palette[index + 1] & 0xFF;
									final int r = palette[index + 2] & 0xFF;
									final int a = 0xFF;
									color = (r << rs) | (g << gs) | (b << bs) | (a << as);
								}
								pixels[width * i + j] = color;
							}
						}
					} else {
						// LowerLeft
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int colormapIndex = bytes[offset + width * i + j] & 0xFF - colormapOrigin;
								int color = 0xFFFFFFFF;
								if (colormapIndex >= 0) {
									final int index = 3 * colormapIndex + 18;
									final int b = palette[index + 0] & 0xFF;
									final int g = palette[index + 1] & 0xFF;
									final int r = palette[index + 2] & 0xFF;
									final int a = 0xFF;
									color = (r << rs) | (g << gs) | (b << bs) | (a << as);
								}
								pixels[width * (height - i - 1) + j] = color;
							}
						}
					}
				}
				break;
			case 32:
				pixels = new int[width * height];
				if ((descriptor & RIGHT_ORIGIN) != 0) {
					if ((descriptor & UPPER_ORIGIN) != 0) {
						// UpperRight
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int colormapIndex = bytes[offset + width * i + j] & 0xFF - colormapOrigin;
								int color = 0xFFFFFFFF;
								if (colormapIndex >= 0) {
									final int index = 4 * colormapIndex + 18;
									final int b = palette[index + 0] & 0xFF;
									final int g = palette[index + 1] & 0xFF;
									final int r = palette[index + 2] & 0xFF;
									final int a = palette[index + 3] & 0xFF;
									color = (r << rs) | (g << gs) | (b << bs) | (a << as);
								}
								pixels[width * i + (width - j - 1)] = color;
							}
						}
					} else {
						// LowerRight
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int colormapIndex = bytes[offset + width * i + j] & 0xFF - colormapOrigin;
								int color = 0xFFFFFFFF;
								if (colormapIndex >= 0) {
									final int index = 4 * colormapIndex + 18;
									final int b = palette[index + 0] & 0xFF;
									final int g = palette[index + 1] & 0xFF;
									final int r = palette[index + 2] & 0xFF;
									final int a = palette[index + 3] & 0xFF;
									color = (r << rs) | (g << gs) | (b << bs) | (a << as);
								}
								pixels[width * (height - i - 1) + (width - j - 1)] = color;
							}
						}
					}
				} else {
					if ((descriptor & UPPER_ORIGIN) != 0) {
						// UpperLeft
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int colormapIndex = bytes[offset + width * i + j] & 0xFF - colormapOrigin;
								int color = 0xFFFFFFFF;
								if (colormapIndex >= 0) {
									final int index = 4 * colormapIndex + 18;
									final int b = palette[index + 0] & 0xFF;
									final int g = palette[index + 1] & 0xFF;
									final int r = palette[index + 2] & 0xFF;
									final int a = palette[index + 3] & 0xFF;
									color = (r << rs) | (g << gs) | (b << bs) | (a << as);
								}
								pixels[width * i + j] = color;
							}
						}
					} else {
						// LowerLeft
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int colormapIndex = bytes[offset + width * i + j] & 0xFF - colormapOrigin;
								int color = 0xFFFFFFFF;
								if (colormapIndex >= 0) {
									final int index = 4 * colormapIndex + 18;
									final int b = palette[index + 0] & 0xFF;
									final int g = palette[index + 1] & 0xFF;
									final int r = palette[index + 2] & 0xFF;
									final int a = palette[index + 3] & 0xFF;
									color = (r << rs) | (g << gs) | (b << bs) | (a << as);
								}
								pixels[width * (height - i - 1) + j] = color;
							}
						}
					}
				}
				break;
			default:
				throw new IOException("Unsupported depth:" + depth);
		}
		return pixels;
	}

	private static int[] createPixelsFromRGB(final int width, final int height, final int depth, final byte[] bytes,
			final int offset, final int descriptor, final Order order) throws IOException {
		int[] pixels = null;
		final int rs = order.redShift;
		final int gs = order.greenShift;
		final int bs = order.blueShift;
		final int as = order.alphaShift;
		switch (depth) {
			case 24:
				pixels = new int[width * height];
				if ((descriptor & RIGHT_ORIGIN) != 0) {
					if ((descriptor & UPPER_ORIGIN) != 0) {
						// UpperRight
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int index = offset + 3 * width * i + 3 * j;
								final int b = bytes[index + 0] & 0xFF;
								final int g = bytes[index + 1] & 0xFF;
								final int r = bytes[index + 2] & 0xFF;
								final int a = 0xFF;
								pixels[width * i + (width - j - 1)] = (r << rs) | (g << gs) | (b << bs) | (a << as);
							}
						}
					} else {
						// LowerRight
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int index = offset + 3 * width * i + 3 * j;
								final int b = bytes[index + 0] & 0xFF;
								final int g = bytes[index + 1] & 0xFF;
								final int r = bytes[index + 2] & 0xFF;
								final int a = 0xFF;
								pixels[width * (height - i - 1) + (width - j - 1)] = (r << rs) | (g << gs) | (b << bs)
										| (a << as);
							}
						}
					}
				} else {
					if ((descriptor & UPPER_ORIGIN) != 0) {
						// UpperLeft
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int index = offset + 3 * width * i + 3 * j;
								final int b = bytes[index + 0] & 0xFF;
								final int g = bytes[index + 1] & 0xFF;
								final int r = bytes[index + 2] & 0xFF;
								final int a = 0xFF;
								pixels[width * i + j] = (r << rs) | (g << gs) | (b << bs) | (a << as);
							}
						}
					} else {
						// LowerLeft
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int index = offset + 3 * width * i + 3 * j;
								final int b = bytes[index + 0] & 0xFF;
								final int g = bytes[index + 1] & 0xFF;
								final int r = bytes[index + 2] & 0xFF;
								final int a = 0xFF;
								pixels[width * (height - i - 1) + j] = (r << rs) | (g << gs) | (b << bs) | (a << as);
							}
						}
					}
				}
				break;
			case 32:
				pixels = new int[width * height];
				if ((descriptor & RIGHT_ORIGIN) != 0) {
					if ((descriptor & UPPER_ORIGIN) != 0) {
						// UpperRight
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int index = offset + 4 * width * i + 4 * j;
								final int b = bytes[index + 0] & 0xFF;
								final int g = bytes[index + 1] & 0xFF;
								final int r = bytes[index + 2] & 0xFF;
								final int a = bytes[index + 3] & 0xFF;
								pixels[width * i + (width - j - 1)] = (r << rs) | (g << gs) | (b << bs) | (a << as);
							}
						}
					} else {
						// LowerRight
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int index = offset + 4 * width * i + 4 * j;
								final int b = bytes[index + 0] & 0xFF;
								final int g = bytes[index + 1] & 0xFF;
								final int r = bytes[index + 2] & 0xFF;
								final int a = bytes[index + 3] & 0xFF;
								pixels[width * (height - i - 1) + (width - j - 1)] = (r << rs) | (g << gs) | (b << bs)
										| (a << as);
							}
						}
					}
				} else {
					if ((descriptor & UPPER_ORIGIN) != 0) {
						// UpperLeft
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int index = offset + 4 * width * i + 4 * j;
								final int b = bytes[index + 0] & 0xFF;
								final int g = bytes[index + 1] & 0xFF;
								final int r = bytes[index + 2] & 0xFF;
								final int a = bytes[index + 3] & 0xFF;
								pixels[width * i + j] = (r << rs) | (g << gs) | (b << bs) | (a << as);
							}
						}
					} else {
						// LowerLeft
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int index = offset + 4 * width * i + 4 * j;
								final int b = bytes[index + 0] & 0xFF;
								final int g = bytes[index + 1] & 0xFF;
								final int r = bytes[index + 2] & 0xFF;
								final int a = bytes[index + 3] & 0xFF;
								pixels[width * (height - i - 1) + j] = (r << rs) | (g << gs) | (b << bs) | (a << as);
							}
						}
					}
				}
				break;
			default:
				throw new IOException("Unsupported depth:" + depth);
		}
		return pixels;
	}

	private static int[] createPixelsFromGrayscale(final int width, final int height, final int depth,
			final byte[] bytes, final int offset, final int descriptor, final Order order) throws IOException {
		int[] pixels = null;
		final int rs = order.redShift;
		final int gs = order.greenShift;
		final int bs = order.blueShift;
		final int as = order.alphaShift;
		switch (depth) {
			case 8:
				pixels = new int[width * height];
				if ((descriptor & RIGHT_ORIGIN) != 0) {
					if ((descriptor & UPPER_ORIGIN) != 0) {
						// UpperRight
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int e = bytes[offset + width * i + j] & 0xFF;
								final int a = 0xFF;
								pixels[width * i + (width - j - 1)] = (e << rs) | (e << gs) | (e << bs) | (a << as);
							}
						}
					} else {
						// LowerRight
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int e = bytes[offset + width * i + j] & 0xFF;
								final int a = 0xFF;
								pixels[width * (height - i - 1) + (width - j - 1)] = (e << rs) | (e << gs) | (e << bs)
										| (a << as);
							}
						}
					}
				} else {
					if ((descriptor & UPPER_ORIGIN) != 0) {
						// UpperLeft
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int e = bytes[offset + width * i + j] & 0xFF;
								final int a = 0xFF;
								pixels[width * i + j] = (e << rs) | (e << gs) | (e << bs) | (a << as);
							}
						}
					} else {
						// LowerLeft
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int e = bytes[offset + width * i + j] & 0xFF;
								final int a = 0xFF;
								pixels[width * (height - i - 1) + j] = (e << rs) | (e << gs) | (e << bs) | (a << as);
							}
						}
					}
				}
				break;
			case 16:
				pixels = new int[width * height];
				if ((descriptor & RIGHT_ORIGIN) != 0) {
					if ((descriptor & UPPER_ORIGIN) != 0) {
						// UpperRight
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int e = bytes[offset + 2 * width * i + 2 * j + 0] & 0xFF;
								final int a = bytes[offset + 2 * width * i + 2 * j + 1] & 0xFF;
								pixels[width * i + (width - j - 1)] = (e << rs) | (e << gs) | (e << bs) | (a << as);
							}
						}
					} else {
						// LowerRight
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int e = bytes[offset + 2 * width * i + 2 * j + 0] & 0xFF;
								final int a = bytes[offset + 2 * width * i + 2 * j + 1] & 0xFF;
								pixels[width * (height - i - 1) + (width - j - 1)] = (e << rs) | (e << gs) | (e << bs)
										| (a << as);
							}
						}
					}
				} else {
					if ((descriptor & UPPER_ORIGIN) != 0) {
						// UpperLeft
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int e = bytes[offset + 2 * width * i + 2 * j + 0] & 0xFF;
								final int a = bytes[offset + 2 * width * i + 2 * j + 1] & 0xFF;
								pixels[width * i + j] = (e << rs) | (e << gs) | (e << bs) | (a << as);
							}
						}
					} else {
						// LowerLeft
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								final int e = bytes[offset + 2 * width * i + 2 * j + 0] & 0xFF;
								final int a = bytes[offset + 2 * width * i + 2 * j + 1] & 0xFF;
								pixels[width * (height - i - 1) + j] = (e << rs) | (e << gs) | (e << bs) | (a << as);
							}
						}
					}
				}
				break;
			default:
				throw new IOException("Unsupported depth:" + depth);
		}
		return pixels;
	}

	private TGAReader() {
	}

	public static final class Order {
		Order(final int redShift, final int greenShift, final int blueShift, final int alphaShift) {
			this.redShift = redShift;
			this.greenShift = greenShift;
			this.blueShift = blueShift;
			this.alphaShift = alphaShift;
		}

		public int redShift;
		public int greenShift;
		public int blueShift;
		public int alphaShift;
	}

}
