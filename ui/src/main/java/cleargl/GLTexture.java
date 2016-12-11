package cleargl;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Hashtable;

public class GLTexture implements GLInterface, GLCloseable {

	private final GL4 mGL;

	private final int[] mTextureId = new int[1];

	private final GLTypeEnum mType;

	private int mBytesPerChannel;

	private final int mTextureWidth;

	private final int mTextureHeight;

	private final int mTextureDepth;

	private final int mTextureOpenGLDataType;

	private final int mTextureOpenGLFormat;

	private int mTextureOpenGLInternalFormat;

	private final int mMipMapLevels;

	private final int mTextureTarget;

	private final int mNumberOfChannels;

	private static ColorModel glAlphaColorModel = new ComponentColorModel(
			ColorSpace.getInstance(ColorSpace.CS_sRGB),
			new int[]{8, 8, 8, 8},
			true,
			false,
			ComponentColorModel.TRANSLUCENT,
			DataBuffer.TYPE_BYTE);

	private static ColorModel glColorModel = new ComponentColorModel(
			ColorSpace.getInstance(ColorSpace.CS_sRGB),
			new int[]{8, 8, 8, 0},
			false,
			false,
			ComponentColorModel.OPAQUE,
			DataBuffer.TYPE_BYTE);

	public GLTexture(final GLInterface pGLInterface,
			final GLTypeEnum pType,
			final int pTextureWidth,
			final int pTextureHeight,
			final int pTextureDepth) {
		this(pGLInterface,
				pType,
				4,
				pTextureWidth,
				pTextureHeight,
				pTextureDepth,
				true,
				1);
	}

	public GLTexture(final GLInterface pGLInterface,
			final GLTypeEnum pType,
			final int pTextureWidth,
			final int pTextureHeight,
			final int pTextureDepth,
			final boolean pLinearInterpolation) {
		this(pGLInterface,
				pType,
				4,
				pTextureWidth,
				pTextureHeight,
				pTextureDepth,
				pLinearInterpolation,
				1);
	}

	public GLTexture(final GLInterface pGLInterface,
			final GLTypeEnum pType,
			final int pNumberOfChannels,
			final int pTextureWidth,
			final int pTextureHeight,
			final int pTextureDepth,
			final boolean pLinearInterpolation,
			final int pMipMapLevels) {
		this(pGLInterface.getGL().getGL4(),
				pType,
				pNumberOfChannels,
				pTextureWidth,
				pTextureHeight,
				pTextureDepth,
				pLinearInterpolation,
				pMipMapLevels);
	}

	public GLTexture(final GL4 pGL,
			final GLTypeEnum pType,
			final int pNumberOfChannels,
			final int pTextureWidth,
			final int pTextureHeight,
			final int pTextureDepth,
			final boolean pLinearInterpolation,
			final int pMipMapLevels,
			final int precision)

	{
		super();
		mGL = pGL;
		mType = pType;
		mNumberOfChannels = pNumberOfChannels;
		mTextureWidth = pTextureWidth;
		mTextureHeight = pTextureHeight;
		mTextureDepth = pTextureDepth;
		mMipMapLevels = pMipMapLevels;

		mTextureTarget = mTextureDepth == 1 ? GL.GL_TEXTURE_2D
				: GL4.GL_TEXTURE_3D;
		mTextureOpenGLFormat = mNumberOfChannels == 4 ? GL.GL_RGBA// GL_BGRA
				: GL4.GL_RED;

		mTextureOpenGLDataType = mType.glType();
		if (mType == GLTypeEnum.Byte) {
			mTextureOpenGLInternalFormat = mNumberOfChannels == 4 ? GL.GL_RGBA8
					: GL.GL_R8;
			switch (mNumberOfChannels) {
				case 1:
					mTextureOpenGLInternalFormat = GL.GL_R8;
					break;
				case 3:
					mTextureOpenGLInternalFormat = GL.GL_RGB8;
					break;
				case 4:
					mTextureOpenGLInternalFormat = GL.GL_RGBA8;
					break;
				default:
					mTextureOpenGLInternalFormat = GL.GL_RGBA8;
			}
			mBytesPerChannel = 1;
		} else if (mType == GLTypeEnum.UnsignedByte) {
			switch (mNumberOfChannels) {
				case 1:
					mTextureOpenGLInternalFormat = GL.GL_R8;
					break;
				case 3:
					mTextureOpenGLInternalFormat = GL.GL_RGB8;
					break;
				case 4:
					mTextureOpenGLInternalFormat = GL.GL_RGBA8;
					break;
				default:
					mTextureOpenGLInternalFormat = GL.GL_RGBA8;
			}
			mBytesPerChannel = 1;
		} else if (mType == GLTypeEnum.Short) {
			mTextureOpenGLInternalFormat = mNumberOfChannels == 4 ? GL.GL_RGBA16F
					: GL.GL_R16F;
			mBytesPerChannel = 2;
		} else if (mType == GLTypeEnum.UnsignedShort) {
			mTextureOpenGLInternalFormat = mNumberOfChannels == 4 ? GL.GL_RGBA16F
					: GL.GL_R16F;
			mBytesPerChannel = 2;
		} else if (mType == GLTypeEnum.Int) {
			mTextureOpenGLInternalFormat = mNumberOfChannels == 4 ? GL.GL_RGBA32F
					: GL.GL_R32F;
			mBytesPerChannel = 4;
		} else if (mType == GLTypeEnum.UnsignedInt) {
			mTextureOpenGLInternalFormat = mNumberOfChannels == 4 ? GL.GL_RGBA32F
					: GL.GL_R32F;
			mBytesPerChannel = 4;
		} else if (mType == GLTypeEnum.Float) {
			switch (mNumberOfChannels) {
				case 1:
					mTextureOpenGLInternalFormat = GL.GL_R32F;
					mBytesPerChannel = 4;
					break;
				case 3:
					if (precision == 16) {
						mTextureOpenGLInternalFormat = GL.GL_RGB16F;
						mBytesPerChannel = 2;
					} else if (precision == 32) {
						mTextureOpenGLInternalFormat = GL.GL_RGB32F;
						mBytesPerChannel = 4;
					}
					break;
				case 4:
					if (precision == 16) {
						mTextureOpenGLInternalFormat = GL.GL_RGBA16F;
						mBytesPerChannel = 2;
					} else if (precision == 32) {
						mTextureOpenGLInternalFormat = GL.GL_RGBA32F;
						mBytesPerChannel = 4;
					}
					break;
				case -1:
					if (precision == 24) {
						mTextureOpenGLInternalFormat = GL.GL_DEPTH_COMPONENT24;
						mBytesPerChannel = 3;
					} else {
						mTextureOpenGLInternalFormat = GL.GL_DEPTH_COMPONENT32;
						mBytesPerChannel = 4;
					}
					break;
			}
		} else
			throw new IllegalArgumentException("Data type not supported for texture !");

		mGL.glGenTextures(1, mTextureId, 0);
		bind();
		mGL.glTexParameterf(mTextureTarget,
				GL.GL_TEXTURE_MAG_FILTER,
				pLinearInterpolation ? GL.GL_LINEAR
						: GL.GL_NEAREST);
		mGL.glTexParameterf(mTextureTarget,
				GL.GL_TEXTURE_MIN_FILTER,
				mMipMapLevels > 1 ? (pLinearInterpolation ? GL.GL_LINEAR_MIPMAP_LINEAR
						: GL.GL_NEAREST_MIPMAP_NEAREST)
						: (pLinearInterpolation ? GL.GL_LINEAR
								: GL.GL_NEAREST));
		mGL.glTexParameterf(mTextureTarget,
				GL.GL_TEXTURE_WRAP_S,
				GL.GL_REPEAT);
		mGL.glTexParameterf(mTextureTarget,
				GL.GL_TEXTURE_WRAP_T,
				GL.GL_REPEAT);

		mGL.glTexStorage2D(mTextureTarget,
				mMipMapLevels,
				mTextureOpenGLInternalFormat,
				mTextureWidth,
				mTextureHeight);
	}

	public GLTexture(final GL4 pGL,
			final GLTypeEnum pType,
			final int pNumberOfChannels,
			final int pTextureWidth,
			final int pTextureHeight,
			final int pTextureDepth,
			final boolean pLinearInterpolation,
			final int pMipMapLevels) {
		this(
				pGL,
				pType,
				pNumberOfChannels,
				pTextureWidth,
				pTextureHeight,
				pTextureDepth,
				pLinearInterpolation,
				pMipMapLevels,
				0);
	}

	public void unbind() {
		mGL.glBindTexture(mTextureTarget, 0);
	}

	@SafeVarargs
	public static <T> void bindTextures(final GLProgram pGLProgram,
			final GLTexture... pTexturesToBind) {
		pGLProgram.bind();
		int lTextureUnit = 0;
		for (final GLTexture lTexture : pTexturesToBind)
			lTexture.bind(lTextureUnit++);
	}

	public void bind(final GLProgram pGLProgram) {
		pGLProgram.bind();
		bind();
	}

	public void bind() {
		mGL.glActiveTexture(GL.GL_TEXTURE0);
		mGL.glBindTexture(mTextureTarget, getId());
	}

	public void bind(final int pTextureUnit) {
		mGL.glActiveTexture(GL.GL_TEXTURE0 + pTextureUnit);
		mGL.glBindTexture(mTextureTarget, getId());
	}

	public void setClamp(final boolean clampS, final boolean clampT) {
		mGL.glTexParameterf(mTextureTarget,
				GL.GL_TEXTURE_WRAP_S,
				clampS ? GL.GL_CLAMP_TO_EDGE : GL.GL_REPEAT);
		mGL.glTexParameterf(mTextureTarget,
				GL.GL_TEXTURE_WRAP_T,
				clampT ? GL.GL_CLAMP_TO_EDGE : GL.GL_REPEAT);
	}

	public void clear() {
		bind();

		final int lNeededSize = mTextureWidth * mTextureHeight
				* mBytesPerChannel
				* mNumberOfChannels;

		// empty buffer
		final Buffer lEmptyBuffer = ByteBuffer.allocateDirect(lNeededSize)
				.order(ByteOrder.nativeOrder());

		mGL.glTexSubImage2D(mTextureTarget,
				0,
				0,
				0,
				mTextureWidth,
				mTextureHeight,
				mTextureOpenGLFormat,
				mTextureOpenGLDataType,
				lEmptyBuffer);
		if (mMipMapLevels > 1)
			updateMipMaps();

	}

	public void updateMipMaps() {
		mGL.glGenerateMipmap(mTextureTarget);
	}

	public void copyFrom(final GLPixelBufferObject pPixelBufferObject) {
		bind();
		pPixelBufferObject.bind();
		mGL.glTexSubImage2D(mTextureTarget,
				0,
				0,
				0,
				mTextureWidth,
				mTextureHeight,
				mTextureOpenGLFormat,
				mTextureOpenGLDataType,
				0);
		if (mMipMapLevels > 1)
			updateMipMaps();

		pPixelBufferObject.unbind();
	}

	public void copyFrom(final Buffer pBuffer,
			final int pLODLevel,
			final boolean pAutoGenerateMipMaps) {
		bind();
		pBuffer.rewind();

		mGL.glTexSubImage2D(mTextureTarget,
				pLODLevel,
				0,
				0,
				mTextureWidth >> pLODLevel,
				mTextureHeight >> pLODLevel,
				mTextureOpenGLFormat,
				mTextureOpenGLDataType,
				pBuffer);
		if (pAutoGenerateMipMaps && mMipMapLevels > 1)
			updateMipMaps();
	}

	public void copyFrom(final Buffer pBuffer) {
		copyFrom(pBuffer, 0, true);
	}

	@Override
	public void close() throws GLException {
		mGL.glDeleteTextures(1, mTextureId, 0);
	}

	public int getWidth() {
		return mTextureWidth;
	}

	public int getHeight() {
		return mTextureHeight;
	}

	public int getDepth()
	{
		return mTextureDepth;
	}

	public int getType() {
		return mTextureOpenGLDataType;
	}

	public GLTypeEnum getNativeType() {
		return mType;
	}

	public int getTextureTarget()
	{
		return mTextureTarget;
	}

	public int getChannels() {
		return mNumberOfChannels;
	}

	public int getInternalFormat() {
		return mTextureOpenGLInternalFormat;
	}

	public int getBitsPerChannel() {
		return mBytesPerChannel * 8;
	}

	@Override
	public GL getGL() {
		return mGL.getGL();
	}

	@Override
	public int getId() {
		return mTextureId[0];
	}

	@Override
	public String toString() {
		return "GLTexture [mGLInterface=" + mGL
				+ ", mTextureId="
				+ Arrays.toString(mTextureId)
				+ ", mTextureWidth="
				+ mTextureWidth
				+ ", mTextureHeight="
				+ mTextureHeight
				+ ", mTextureOpenGLInternalFormat="
				+ mTextureOpenGLInternalFormat
				+ "]";
	}

	public void dumpToFile(final ByteBuffer buf) {
		try {
			final File file = new File("/Users/ulrik/" + this.getId() + ".dump");
			final FileChannel channel = new FileOutputStream(file, false).getChannel();
			buf.rewind();
			channel.write(buf);
			channel.close();
		} catch (final Exception e) {
			System.err.println("Unable to dump " + this.getId());
			e.printStackTrace();
		}
	}

	public static GLTexture loadFromFile(final GL4 gl, final String filename, final boolean linearInterpolation,
			final int mipmapLevels) {
		BufferedImage bi;
		BufferedImage flippedImage;
		final ByteBuffer imageData;
		FileInputStream fis = null;
		FileChannel channel = null;
		int[] pixels = null;
		GLTexture tex;

		if (filename.substring(filename.lastIndexOf('.')).toLowerCase().endsWith("tga")) {
			byte[] buffer = null;

			try {
				fis = new FileInputStream(filename);
				channel = fis.getChannel();
				final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				channel.transferTo(0, channel.size(), Channels.newChannel(byteArrayOutputStream));
				buffer = byteArrayOutputStream.toByteArray();

				channel.close();
				fis.close();

				pixels = TGAReader.read(buffer, TGAReader.ARGB);
				final int width = TGAReader.getWidth(buffer);
				final int height = TGAReader.getHeight(buffer);
				bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				bi.setRGB(0, 0, width, height, pixels, 0, width);
			} catch (final Exception e) {
				System.err.println("GLTexture: could not read image from TGA" + filename + ".");
				return null;
			}
		} else {
			try {
				fis = new FileInputStream(filename);
				channel = fis.getChannel();
				final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				channel.transferTo(0, channel.size(), Channels.newChannel(byteArrayOutputStream));
				bi = ImageIO.read(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));

				channel.close();
			} catch (final Exception e) {
				System.err.println("GLTexture: could not read image from " + filename + ".");
				return null;
			}
		}

		// convert to OpenGL UV space
		flippedImage = createFlipped(bi);
		imageData = bufferedImageToRGBABuffer(flippedImage);

		int texWidth = 2;
		int texHeight = 2;

		while (texWidth < bi.getWidth()) {
			texWidth *= 2;
		}
		while (texHeight < bi.getHeight()) {
			texHeight *= 2;
		}

		return getGlTexture(gl, linearInterpolation, mipmapLevels, bi, imageData, texWidth, texHeight);
	}

	public static GLTexture getGlTexture(GL4 gl, boolean linearInterpolation, int mipmapLevels, BufferedImage bi, ByteBuffer imageData, int texWidth, int texHeight) {
		GLTexture tex;
		tex = new GLTexture(gl,
				nativeTypeEnumFromBufferedImage(bi),
				bi.getColorModel().getNumComponents(),
				texWidth, texHeight, 1,
				linearInterpolation,
				mipmapLevels);

		tex.clear();
		tex.copyFrom(imageData);
		tex.updateMipMaps();

		return tex;
	}

	private static GLTypeEnum nativeTypeEnumFromBufferedImage(final BufferedImage bi) {
		switch (bi.getData().getDataBuffer().getDataType()) {
			case DataBuffer.TYPE_BYTE: {
				return GLTypeEnum.UnsignedByte;
			}
			case DataBuffer.TYPE_DOUBLE: {
				return GLTypeEnum.Double;
			}
			case DataBuffer.TYPE_INT: {
				return GLTypeEnum.UnsignedByte;
			}
			case DataBuffer.TYPE_SHORT: {
				return GLTypeEnum.Short;
			}
			default:
				return null;
		}
	}

	private static ByteBuffer bufferedImageToRGBABuffer(final BufferedImage bufferedImage) {
		ByteBuffer imageBuffer;
		WritableRaster raster;
		BufferedImage texImage;

		int texWidth = 2;
		int texHeight = 2;

		while (texWidth < bufferedImage.getWidth()) {
			texWidth *= 2;
		}
		while (texHeight < bufferedImage.getHeight()) {
			texHeight *= 2;
		}

		if (bufferedImage.getColorModel().hasAlpha()) {
			raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, texWidth, texHeight, 4, null);
			texImage = new BufferedImage(GLTexture.glAlphaColorModel, raster, false, new Hashtable<>());
		} else {
			raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, texWidth, texHeight, 3, null);
			texImage = new BufferedImage(GLTexture.glColorModel, raster, false, new Hashtable<>());
		}

		final Graphics g = texImage.getGraphics();
		g.setColor(new Color(0.0f, 0.0f, 0.0f, 1.0f));
		g.fillRect(0, 0, texWidth, texHeight);
		g.drawImage(bufferedImage, 0, 0, null);
		g.dispose();

		final byte[] data = ((DataBufferByte) texImage.getRaster().getDataBuffer()).getData();

		imageBuffer = ByteBuffer.allocateDirect(data.length);
		imageBuffer.order(ByteOrder.nativeOrder());
		imageBuffer.put(data, 0, data.length);
		imageBuffer.rewind();

		return imageBuffer;
	}

	// the following three routines are from
	// http://stackoverflow.com/a/23458883/2129040,
	// authored by MarcoG
	private static BufferedImage createFlipped(final BufferedImage image) {
		final AffineTransform at = new AffineTransform();
		at.concatenate(AffineTransform.getScaleInstance(1, -1));
		at.concatenate(AffineTransform.getTranslateInstance(0, -image.getHeight()));
		return createTransformed(image, at);
	}

	private static BufferedImage createRotated(final BufferedImage image) {
		final AffineTransform at = AffineTransform.getRotateInstance(
				Math.PI, image.getWidth() / 2, image.getHeight() / 2.0);
		return createTransformed(image, at);
	}

	private static BufferedImage createTransformed(
			final BufferedImage image, final AffineTransform at) {
		final BufferedImage newImage = new BufferedImage(
				image.getWidth(), image.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = newImage.createGraphics();
		g.transform(at);
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return newImage;
	}

}
