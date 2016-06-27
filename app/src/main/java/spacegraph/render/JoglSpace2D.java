package spacegraph.render;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

/**
 * 2D window
 */
public abstract class JoglSpace2D extends JoglSpace {

    public JoglSpace2D() {
        super();
    }


    public void clear(float opacity) {

        if (opacity < 1f) {
            //TODO use gl.clear faster than rendering this quad
            gl.glColor4f(0, 0, 0, opacity);
            gl.glRectf(0, 0, getWidth(), getHeight());
        } else {
            gl.glClearColor(0f,0f,0f,1f);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        }
    }


    @Override
    public abstract void display(GLAutoDrawable glAutoDrawable);

    @Override
    public void reshape(GLAutoDrawable ad, int i, int i1, int i2, int i3) {

        GL2 gl = (GL2) ad.getGL();


        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

        gl.glClearColor(0, 0, 0, 0);

        gl.glViewport(0, 0, getWidth(), getHeight());


        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        gl.glOrtho(0, getWidth(), 0, getHeight(), 1, -1);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

    }

}
