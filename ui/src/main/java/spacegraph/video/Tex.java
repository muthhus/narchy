package spacegraph.video;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.Surface;
import spacegraph.render.Draw;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jogamp.opengl.GL.GL_BGRA;
import static com.jogamp.opengl.GL.GL_RGB;
import static com.jogamp.opengl.GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV;

public class Tex {

    public com.jogamp.opengl.util.texture.Texture texture;

    public boolean mipmap;

    //TODO use a PPM uncompressed format for transferring from CPU to GPU


    final AtomicBoolean textureUpdated = new AtomicBoolean(false);
    private GLProfile profile;
    private TextureData nextData;
    public int[] array;
    private IntBuffer buffer;
    private Object src;

    public final void paint(GL2 gl, RectFloat2D bounds) {
        paint(gl, bounds, -1);
    }

    public void paint(GL2 gl, RectFloat2D bounds, float repeatScale) {


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
            Draw.rectTex(gl, texture, bounds.min.x, bounds.min.y, bounds.w(), bounds.h(), 0, repeatScale);
        }

    }

    public void update(BufferedImage iimage) {

        if (profile == null)
            return;


        if (nextData == null || this.src!=iimage) {

            update(((DataBufferInt)(iimage.getRaster().getDataBuffer())).getData(), iimage.getWidth(), iimage.getHeight());

        }

        textureUpdated.set(true);
    }
    public void update(int[] iimage, int width, int height) {

        if (profile == null)
            return;


        if (nextData == null || this.src!=iimage) {

            this.src = iimage;
            array = iimage;

            //buffer = IntBuffer.allocate(pixels);
            buffer = IntBuffer.wrap(array);
            nextData = new TextureData(profile, GL_RGB,
                    width, height,
                    0 /* border */,
                    GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV,
                    mipmap,
                    false,
                    false,
                    buffer, null
            );
        }

        textureUpdated.set(true);
    }

    public Surface view() {
        return new TexSurface();
    }

    private class TexSurface extends Surface {

        @Override
        protected void paint(GL2 gl) {
            Tex.this.paint(gl, bounds);
        }
    }
}
