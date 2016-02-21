package automenta.spacegraph;

import automenta.spacegraph.shape.Drawable;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.gl2.GLUT;

import java.util.ArrayList;
import java.util.List;

import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_LIGHTING;


public class Space2D implements Drawable {

     List<Drawable> drawables = new ArrayList<Drawable>();

    public Space2D() {
        super();
    }

    public Space2D(Drawable d) {
        this();
        drawables.add(d);
    }

    public List<Drawable> getDrawables() {
        return drawables;
    }

    @Override
    public void draw(GL2 gl) {
        int id = 0;

        gl.glEnable(GL.GL_BLEND);			// Turn Blending On
        gl.glDisable(GL.GL_DEPTH_TEST);	// Turn Depth Testing Off
        //gl.glColor4f(1.0f, 1.0f, 1.0f, 0.5f);					// Full Brightness.  50% Alpha (new )
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);					// Set The Blending Function For Translucency (new )

        synchronized (drawables) {
            for (Drawable d : drawables) {
                //gl.glPushName(id++);
                d.draw(gl);
                //gl.glPopName();
            }
        }
    }

    public void removeAll() {
        synchronized (drawables) {
            drawables.clear();
        }
    }

    public <D extends Drawable> D add(D d) {
        synchronized (drawables) {
            drawables.add(d);
        }
        return d;
    }

    public boolean remove(Drawable d) {
        synchronized (drawables) {
            return drawables.add(d);
        }
    }

    static final GLUT glut = new GLUT();

    public static void renderstring2d(GL2 gl, char string[], float r, float g, float b, float x, float y)
    {
        gl.glColor3f(r, g, b);


        gl.glRasterPos2f(x, y);
        for(int i = 0; i < string.length; i++)
            glut.glutBitmapCharacter(GLUT.BITMAP_9_BY_15, string[i]);
    }

    public static void renderstring3d(GL2 gl, char string[], float r, float g, float b, float x, float y, float z)
    {
        gl.glDisable(GL_LIGHTING);
        gl.glColor3f(r, g, b);

        gl.glRasterPos3f(x, y, z);
        for(int i = 0; i < string.length; i++)
            glut.glutBitmapCharacter(GLUT.BITMAP_9_BY_15, string[i]);
    }
}
