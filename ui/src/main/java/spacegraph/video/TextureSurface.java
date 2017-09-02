package spacegraph.video;

import com.fasterxml.jackson.databind.util.ByteBufferBackedOutputStream;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import spacegraph.Surface;
import spacegraph.render.Draw;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class TextureSurface extends Surface {

    Texture texture;

    //TODO dynamically adapt this
    final byte[] aa = new byte[2 * 1024 * 1024];
    private final ByteBuffer bb;

    public boolean mipmap = true;

    //TODO use a PPM uncompressed format for transferring from CPU to GPU
    String internalFormat =
            //"tif";
            "png";
    //"png";
    //"jpg";


    final AtomicBoolean textureUpdated = new AtomicBoolean(false);
    private GLProfile profile;
    private TextureData nextData;

    public TextureSurface() {
        bb = ByteBuffer.wrap(aa);
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

        bb.rewind();
        OutputStream out = new ByteBufferBackedOutputStream(bb); //ByteArrayOutputStream(aa);

        try {

            ImageIO.write(iimage, internalFormat, out);
            out.close();

            InputStream in = new ByteArrayInputStream(aa);
            nextData = TextureIO.newTextureData(profile, in, mipmap, internalFormat);

        } catch (IOException e) {
            e.printStackTrace();
        }

        textureUpdated.set(true);
    }

}
