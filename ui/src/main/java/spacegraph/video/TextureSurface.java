package spacegraph.video;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import spacegraph.Surface;
import spacegraph.render.Draw;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jogamp.opengl.GL.GL_BGRA;
import static com.jogamp.opengl.GL.GL_RGB;
import static com.jogamp.opengl.GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV;
import static java.lang.System.arraycopy;

public class TextureSurface extends Surface {

    Texture texture;

    public boolean mipmap = false;

    //TODO use a PPM uncompressed format for transferring from CPU to GPU


    final AtomicBoolean textureUpdated = new AtomicBoolean(false);
    private GLProfile profile;
    private TextureData nextData;
    private int[] array;
    private IntBuffer buffer;

    public TextureSurface() {

    }


    @Override
    protected void paint(GL2 gl) {

        if (profile == null)
            profile = gl.getGLProfile();

        if (textureUpdated.compareAndSet(true, false)) {


            if (texture == null) {
                texture = TextureIO.newTexture(gl, nextData);
            } else {
                //TODO compute 'd' outside of rendering paint in the update method
                texture.updateImage(gl, nextData);
            }


        }

        if (texture != null) {
            Draw.rectTex(gl, texture, 0, 0, 1, 1, 0);
        }

    }

    public void update(BufferedImage iimage) {

        if (profile == null)
            return;

        int pixels = iimage.getWidth() * iimage.getHeight();

        if (nextData == null) {

            buffer = IntBuffer.allocate(pixels);
            array = buffer.array();
            nextData = new TextureData(profile, GL_RGB,
                    iimage.getWidth(), iimage.getHeight(),
                    0 /* border */,
                    GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV,
                    mipmap,
                    false,
                    false,
                    buffer, (TextureData.Flusher)null
            );
        }


        if (array!=null) {
            //TODO eliminate need for BufferedImage and move to subclass

            int[] aa = ((DataBufferInt)(iimage.getRaster().getDataBuffer())).getData();
            arraycopy(aa, 0, array, 0, aa.length);

//            int i = 0;
//            for (int j = 0; j < pixels; ) {
//                byte r = aa[i++];
//                byte g = aa[i++];
//                byte b = aa[i++];
//                byte a = aa[i++];
//                array[j++] = Ints.fromBytes(r, g, b, a);
//            }
        }


//        bb.rewind();
//        OutputStream out = new ByteBufferBackedOutputStream(bb); //ByteArrayOutputStream(aa);
//
//        try {
//
//
//            ImageIO.write(iimage, internalFormat, out);
//            out.close();
//
//            InputStream in = new ByteArrayInputStream(aa);
//            nextData = TextureIO.newTextureData(profile, in, mipmap, internalFormat);
//
//
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        textureUpdated.set(true);
    }

}
