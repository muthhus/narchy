package cleargl;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Hashtable;
import javax.swing.JLabel;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES3;

/**
 * Created by ulrik on 11/02/15.
 */

public class ClearTextRenderer {
	protected BufferedImage image;
	protected Graphics2D g2d;
	protected GLProgram mProg;
	protected GLMatrix ModelMatrix = new GLMatrix();
	protected GLMatrix ViewMatrix = new GLMatrix();
	protected GLMatrix ProjectionMatrix = new GLMatrix();

	protected GL mGL;
	protected final boolean mShouldCache;

	protected HashMap<String, ByteBuffer> textureCache = new HashMap<>();

	public ClearTextRenderer(final GL pGL, final boolean shouldCache) {
		init(pGL);
		mShouldCache = true;
	}

	public void init(final GL pGL) {
		mGL = pGL;
		try {
			mProg = GLProgram.buildProgram(pGL,
					ClearTextRenderer.class,
					new String[]{"shaders/TextRenderer.vs",
							"shaders/TextRenderer.fs"});
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public void drawTextAtPosition(final String pText,
			final int pWindowPositionX,
			final int pWindowPositionY,
			Font pFont,
			final Color pColor,
			final boolean pAntiAliased) {
		if (pFont == null) {
			System.err.println("Font invalid for text \"" + pText + "\"");
			pFont = new JLabel().getFont();
		}

		final int scaleFactor = ClearGLWindow.isRetina(mGL) ? 2 : 1;

		final int windowSizeX = mGL.getContext()
				.getGLDrawable()
				.getSurfaceWidth() / scaleFactor;
		final int windowSizeY = mGL.getContext()
				.getGLDrawable()
				.getSurfaceHeight() / scaleFactor;

		final int width = pText.length() * pFont.getSize();
		final int height = pFont.getSize();

		// don't store more then 50 textures
		if (textureCache.size() > 50) {
			textureCache.clear();
		}

		if (!mShouldCache || (mShouldCache && !textureCache.containsKey(pText))) {
			ByteBuffer imageBuffer;

			final ColorModel glAlphaColorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
					new int[]{8,
							8,
							8,
							8},
					true,
					false,
					Transparency.TRANSLUCENT,
					DataBuffer.TYPE_BYTE);
			WritableRaster raster;

			raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
					pFont.getSize() * pText.length(),
					18,
					4,
					null);

			image = new BufferedImage(glAlphaColorModel,
					raster,
					true,
					new Hashtable());

			g2d = image.createGraphics();
			g2d.setFont(pFont);
			g2d.setColor(pColor);

			if (pAntiAliased) {
				final RenderingHints rh = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
						RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);

				g2d.setRenderingHints(rh);
			}

			g2d.drawString(pText, 0, pFont.getSize());

			final byte[] data = ((DataBufferByte) image.getRaster()
					.getDataBuffer()).getData();

			imageBuffer = ByteBuffer.allocateDirect(data.length);
			imageBuffer.order(ByteOrder.nativeOrder());
			imageBuffer.put(data, 0, data.length);
			imageBuffer.flip();

			if (!mShouldCache) {
				textureCache.clear();
			}

			textureCache.put(pText, imageBuffer);
		}

		mGL.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT);

		mGL.glDisable(GL.GL_CULL_FACE);
		mGL.glDisable(GL.GL_DEPTH_TEST);

		mGL.glEnable(GL.GL_BLEND);
		mGL.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

		final int[] uiTexture = new int[1];
		final int[] ui_vbo = new int[3];
		final int[] ui_vao = new int[1];

		final float w = width;
		final float h = height;
		final float x = pWindowPositionX;
		final float y = pWindowPositionY;

		final FloatBuffer vertices = FloatBuffer
				.wrap(new float[]{x, y + h, 0.0f, x + w, y + h, 0.0f, x, y, 0.0f, x + w, y, 0.0f});

		final FloatBuffer normals = FloatBuffer.wrap(new float[]{0.0f,
				0.0f,
				-1.0f,
				0.0f,
				0.0f,
				-1.0f,
				0.0f,
				0.0f,
				-1.0f,
				0.0f,
				0.0f,
				-1.0f,});

		final FloatBuffer texCoords = FloatBuffer.wrap(new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f});

		mGL.getGL3().glUseProgram(mProg.getId());

		ModelMatrix.setIdentity();
		ViewMatrix.setIdentity();
		ProjectionMatrix.setOrthoProjectionMatrix(0.0f,
				windowSizeX,
				0.0f,
				windowSizeY,
				-1.0f,
				1.0f);

		mGL.getGL3().glGenVertexArrays(1, ui_vao, 0);
		mGL.getGL3().glBindVertexArray(ui_vao[0]);
		mGL.getGL3().glGenBuffers(3, ui_vbo, 0);

		mGL.glBindBuffer(GL.GL_ARRAY_BUFFER, ui_vbo[0]);
		mGL.glBufferData(GL.GL_ARRAY_BUFFER,
				vertices.limit() * (Float.SIZE / Byte.SIZE),
				vertices,
				GL.GL_STATIC_DRAW);
		mGL.getGL3().glEnableVertexAttribArray(0);
		mGL.getGL3()
				.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);

		mGL.glBindBuffer(GL.GL_ARRAY_BUFFER, ui_vbo[1]);
		mGL.glBufferData(GL.GL_ARRAY_BUFFER,
				normals.limit() * (Float.SIZE / Byte.SIZE),
				normals,
				GL.GL_STATIC_DRAW);
		mGL.getGL3().glEnableVertexAttribArray(1);
		mGL.getGL3()
				.glVertexAttribPointer(1, 3, GL.GL_FLOAT, false, 0, 0);

		mGL.glBindBuffer(GL.GL_ARRAY_BUFFER, ui_vbo[2]);
		mGL.glBufferData(GL.GL_ARRAY_BUFFER,
				texCoords.limit() * (Float.SIZE / Byte.SIZE),
				texCoords,
				GL.GL_STATIC_DRAW);
		mGL.getGL3().glEnableVertexAttribArray(2);
		mGL.getGL3()
				.glVertexAttribPointer(2, 2, GL.GL_FLOAT, false, 0, 0);

		mGL.glActiveTexture(GL.GL_TEXTURE1);
		mGL.glGenTextures(1, uiTexture, 0);
		mGL.glBindTexture(GL.GL_TEXTURE_2D, uiTexture[0]);

		mGL.glTexParameteri(GL.GL_TEXTURE_2D,
				GL.GL_TEXTURE_MIN_FILTER,
				GL.GL_NEAREST);
		mGL.glTexParameteri(GL.GL_TEXTURE_2D,
				GL.GL_TEXTURE_MAG_FILTER,
				GL.GL_NEAREST);

		mGL.glTexParameteri(GL.GL_TEXTURE_2D,
				GL2ES3.GL_TEXTURE_BASE_LEVEL,
				0);
		mGL.glTexParameteri(GL.GL_TEXTURE_2D,
				GL2ES3.GL_TEXTURE_MAX_LEVEL,
				0);

		mGL.glTexImage2D(GL.GL_TEXTURE_2D,
				0,
				GL.GL_RGBA8,
				width,
				height,
				0,
				GL.GL_RGBA,
				GL.GL_UNSIGNED_BYTE,
				textureCache.get(pText));

		mProg.getUniform("uitex").setInt(1);
		ModelMatrix.mult(ViewMatrix);
		mProg.getUniform("ModelViewMatrix")
				.setFloatMatrix(ModelMatrix.getFloatArray(), false);
		mProg.getUniform("ProjectionMatrix")
				.setFloatMatrix(ProjectionMatrix.getFloatArray(), false);
		mGL.getGL3().glUseProgram(mProg.getId());

		mGL.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);

		mGL.getGL3().glDisableVertexAttribArray(0);

		mGL.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
		mGL.glBindTexture(GL.GL_TEXTURE_2D, 0);

		mGL.glDeleteTextures(1, uiTexture, 0);
		mGL.glDeleteBuffers(3, ui_vbo, 0);
		mGL.getGL3().glDeleteVertexArrays(1, ui_vao, 0);
	}

}
