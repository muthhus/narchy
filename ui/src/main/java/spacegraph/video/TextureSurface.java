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

    public boolean mipmap;

    //TODO use a PPM uncompressed format for transferring from CPU to GPU


    final AtomicBoolean textureUpdated = new AtomicBoolean(false);
    private GLProfile profile;
    private TextureData nextData;
    public int[] array;
    private IntBuffer buffer;
    private Object src;

    public TextureSurface() {

    }


    @Override
    public void paint(GL2 gl) {

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


        if (nextData == null || this.src!=iimage) {

            this.src = iimage;
            array = ((DataBufferInt)(iimage.getRaster().getDataBuffer())).getData();

            //buffer = IntBuffer.allocate(pixels);
            buffer = IntBuffer.wrap(array);
            nextData = new TextureData(profile, GL_RGB,
                    iimage.getWidth(), iimage.getHeight(),
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

}
