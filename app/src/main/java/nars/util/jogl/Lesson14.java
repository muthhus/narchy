package nars.util.jogl;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;
import nars.util.AbstractJoglPanel;

import java.text.NumberFormat;

public class Lesson14 extends AbstractJoglPanel {
    private float rotation; // Rotation
    private static final GLU glu = new GLU();
    private static final GLUT glut = new GLUT();
    private static final NumberFormat numberFormat = NumberFormat.getNumberInstance();
    ;

    public static void main(String[] args) {
        new Lesson14().show(500, 400);
    }

    public Lesson14() {
        super();
        //GLDisplay neheGLDisplay = GLDisplay.createGLDisplay("Lesson 14: Outline fonts");

        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);
    }

    void renderString(GL2 gl, int font, String string) {
// Center Our Text On The Screen
        float width =
                glut.glutStrokeLength(font, string);
                //glut.glutBitmapLength(font, string);

        gl.glTranslatef(-width / 2f, 0, 0);
// Render The Text
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            glut.glutStrokeCharacter(font, c);
            //glut.glutBitmapCharacter(font, c);
        }
    }

    public void init(GLAutoDrawable glDrawable) {
        GL2 gl2 = (GL2) glDrawable.getGL();
        gl2.glShadeModel(GL2.GL_SMOOTH); // Enable Smooth Shading
        gl2.glClearColor(0.0f, 0.0f, 0.0f, 0.1f); // Black Background
        gl2.glClearDepth(1.0f); // Depth Buffer Setup
        gl2.glEnable(GL2.GL_DEPTH_TEST); // Enables Depth Testing
        gl2.glDepthFunc(GL2.GL_LEQUAL); // The Type Of Depth Testing To Do

        gl2.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);
        gl2.glEnable(GL2.GL_BLEND); // Enable Default Light (Quick And Dirty)
        gl2.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl2.glEnable(GL2.GL_LIGHT0); // Enable Default Light (Quick And Dirty)
        gl2.glEnable(GL2.GL_LIGHTING); // Enable Lighting
        gl2.glEnable(GL2.GL_COLOR_MATERIAL); // Enable Coloring Of Material
    }

    public void display(GLAutoDrawable glDrawable) {
        GL2 gl2 = (GL2) glDrawable.getGL();

// Clear Screen And Depth Buffer
        gl2.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        gl2.glLoadIdentity(); // Reset The Current Modelview Matrix
        gl2.glTranslatef(0.0f, 0.0f, -15.0f); // Move One Unit Into The Screen
        gl2.glRotatef(rotation, 1.0f, 0.0f, 0.0f); // Rotate On The X Axis
        gl2.glRotatef(rotation * 1.5f, 0.0f, 1.0f, 0.0f); // Rotate On The Y Axis
        gl2.glRotatef(rotation * 1.4f, 0.0f, 0.0f, 1.0f); // Rotate On The Z Axis
        gl2.glScalef(0.005f, 0.005f, 0.0f);
// Pulsing Colors Based On The Rotation
        gl2.glColor3f((float) (Math.cos(rotation / 20.0f)),
                (float) (Math.sin(rotation / 25.0f)), 1.0f - 0.5f *
                        (float) (Math.cos(rotation / 17.0f)));
        gl2.glLineWidth(5f);

        renderString(gl2, GLUT.STROKE_MONO_ROMAN /*STROKE_MONO_ROMAN*/, "NeHe - " +
                numberFormat.format((rotation / 50))); // Print GL Text To The Screen

        rotation += 0.5f; // Increase The Rotation Variable
    }

    public void reshape(GLAutoDrawable glDrawable, int x, int y, int w, int h) {
        if (h == 0) h = 1;
        GL2 gl2 = (GL2) glDrawable.getGL();

// Reset The Current Viewport And Perspective Transformation
        gl2.glViewport(0, 0, w, h);

        gl2.glMatrixMode(GL2.GL_PROJECTION); // Select The Projection Matrix
        gl2.glLoadIdentity(); // Reset The Projection Matrix

// Calculate The Aspect Ratio Of The Window
        glu.gluPerspective(45.0f, (float) w / (float) h, 0.1f, 100.0f);

        gl2.glMatrixMode(GL2.GL_MODELVIEW); // Select The Modelview Matrix
        gl2.glLoadIdentity(); // Reset The ModalView Matrix
    }

    public void displayChanged(GLAutoDrawable glDrawable, boolean b, boolean b1) {
    }
}