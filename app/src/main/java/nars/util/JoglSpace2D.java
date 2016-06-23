package nars.util;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

/**
 * 2D window
 */
public abstract class JoglSpace2D extends JoglSpace {

    public JoglSpace2D() {
        super();
    }

    @Deprecated protected static void line(GL2 gl, double x1, double y1, double x2, double y2) {
        line(gl, (float)x1, (float)y1, (float)x2, (float)y2 );
    }


    protected static void line(GL2 gl, float x1, float y1, float x2, float y2) {
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex2f(x1, y1);
        gl.glVertex2f(x2, y2);
        gl.glEnd();
    }

    protected static void strokeRect(GL2 gl, float x1, float y1, float w, float h) {
        line(gl, x1, y1, x1 + w, y1);
        line(gl, x1, y1, x1, y1 + h);
        line(gl, x1, y1+h, x1+w, y1 + h);
        line(gl, x1+w, y1, x1+w, y1 + h);
    }

    protected static void rect(GL2 gl, float x1, float y1, float w, float h) {
        gl.glBegin(GL2.GL_QUADS);
        gl.glVertex2f(x1, y1);
        gl.glVertex2f(x1+w, y1);
        gl.glVertex2f(x1+w, y1+h);
        gl.glVertex2f(x1, y1+h);
        gl.glEnd();
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
