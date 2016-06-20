package nars.util.jogl;
 
/*--. .-"-.
/ o_O / O o \
\_ (__\ \_ v _/
// \\ // \\
(( )) (( ))
¤¤¤¤¤¤¤¤¤¤¤¤¤¤--""---""--¤¤¤¤--""---""--¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
¤ ||| ||| ¤
¤ | | ¤
¤ ¤
¤ Programmer:Abdul Bezrati ¤
¤ Program :Nehe's 12th lesson port to JOGL ¤
¤ Comments :None ¤
¤ _______ ¤
¤ /` _____ `\;, <span id="cloak31699"><a href="mailto:abezrati@hotmail.com">abezrati@hotmail.com</a></span><script type="text/javascript">
//<!--
document.getElementById('cloak31699').innerHTML = '';
var prefix = 'ma' + 'il' + 'to';
var path = 'hr' + 'ef' + '=';
var addy31699 = 'abezrati' + '@';
addy31699 = addy31699 + 'hotmail' + '.' + 'com';
document.getElementById('cloak31699').innerHTML += '<a ' + path + '\'' + prefix + ':' + addy31699 + '\'>' +addy31699+'<\/a>';
//-->
</script> ¤
¤ (__(^===^)__)';, ___ ¤
¤ / ::: \ ,; /^ ^\ ¤
¤ | ::: | ,;' ( Ö Ö ) ¤
¤¤¤'._______.'`¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤ --°oOo--(_)--oOo°--¤¤*/


import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.glu.GLU;
import nars.util.AbstractJoglPanel;

import javax.swing.*;

/**
 * @author Abdul Bezrati
 */
public class Lesson12 extends AbstractJoglPanel implements KeyListener {

    public static void main(String[] args) {
        new Lesson12().show(500, 500);
//        GLDisplay neheGLDisplay = GLDisplay.createGLDisplay("Lesson 12: Display lists");
//        Renderer renderer = new Renderer();
//        InputHandler inputHandler = new InputHandler(renderer, neheGLDisplay);
//        neheGLDisplay.addGLEventListener(renderer);
//        neheGLDisplay.addKeyListener(inputHandler);
//        neheGLDisplay.start();



    }

    public Lesson12() {
        super();

        registerKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "Decrease X-axis rotation");

        registerKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "Increase X-axis rotation");

        registerKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "Decrease Y-axis rotation");

        registerKey(
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "Increase Y-axis rotation");

        addKeyListener(this);
    }


    private void registerKey(KeyStroke keyStroke, String s) {
    }


    public void keyPressed(KeyEvent e) {
        processKeyEvent(e, true);
    }

    public void keyReleased(KeyEvent e) {
        processKeyEvent(e, false);
    }

    private void processKeyEvent(KeyEvent e, boolean pressed) {
        float speed = 5f;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                rotX(-speed);
                break;
            case KeyEvent.VK_DOWN:
                rotX(speed);
                break;
            case KeyEvent.VK_LEFT:
                rotY(-speed);
                break;
            case KeyEvent.VK_RIGHT:
                rotY(speed);
                break;
        }
    }

    private void rotX(float dr) {
        xrot += dr;
    }

    private void rotY(float dr) {
        yrot += dr;
    }


    private float[][] boxcol = {{1.0f, 0.0f, 0.0f},
            {1.0f, 0.5f, 0.0f},
            {1.0f, 1.0f, 0.0f},
            {0.0f, 1.0f, 0.0f},
            {0.0f, 1.0f, 1.0f}};
    private float[][] topcol = {{0.5f, 0.0f, 0.0f},
            {0.5f, .25f, 0.0f},
            {0.5f, 0.5f, 0.0f},
            {0.0f, 0.5f, 0.0f},
            {0.0f, 0.5f, 0.5f}};
    private float xrot; // Rotates Cube On The X Axis


    private float yrot; // Rotates Cube On The Y Axis


    //private int[] textures = new int[1]; // Storage For 1 Texture
    private int xloop; // Loop For X Axis
    private int yloop; // Loop For Y Axis
    private int box; // Storage For The Box Display List
    private int top; // Storage For The Top Display List

    private final static GLU glu = new GLU();

//    public void increaseXrot(boolean increase) {
//        increaseX = increase;
//    }
//
//    public void decreaseXrot(boolean decrease) {
//        decreaseX = decrease;
//    }
//
//    public void increaseYrot(boolean increase) {
//        increaseY = increase;
//    }
//
//    public void decreaseYrot(boolean decrease) {
//        decreaseY = decrease;
//    }

//    private void loadGLTexture(GL gl) {
//        TextureReader.Texture texture = null;
//        try {
//            texture = TextureReader.readTexture("demos/data/images/Cube.bmp");
//        } catch (IOException e) {
//            e.printStackTrace();
//            throw new RuntimeException(e);
//        }
//
//        gl.glGenTextures(1, textures, 0); // Create The Texture
//// Typical Texture Generation Using Data From The Bitmap
//        gl.glBindTexture(GL2.GL_TEXTURE_2D, textures[0]);
//        gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, 3, texture.getWidth(), texture.getHeight(),
//                0, GL2.GL_RGB, GL2.GL_UNSIGNED_BYTE, texture.getPixels());
//
//        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
//        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
//    }

    private void buildLists(GL2 gl) {
        box = gl.glGenLists(2); // Generate 2 Different Lists
        gl.glNewList(box, GL2.GL_COMPILE); // Start With The Box List

        gl.glBegin(GL2.GL_QUADS);
        gl.glNormal3f(0.0f, -1.0f, 0.0f);
        //gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3f(-1.0f, -1.0f, -1.0f); // Bottom Face
        //gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3f(1.0f, -1.0f, -1.0f);
        //gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f(1.0f, -1.0f, 1.0f);
        //gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3f(-1.0f, -1.0f, 1.0f);

        gl.glNormal3f(0.0f, 0.0f, 1.0f);
        //gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f(-1.0f, -1.0f, 1.0f); // Front Face
        //gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3f(1.0f, -1.0f, 1.0f);
        //gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3f(1.0f, 1.0f, 1.0f);
        //gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3f(-1.0f, 1.0f, 1.0f);

        gl.glNormal3f(0.0f, 0.0f, -1.0f);
        //gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3f(-1.0f, -1.0f, -1.0f); // Back Face
        //gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3f(-1.0f, 1.0f, -1.0f);
        //gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3f(1.0f, 1.0f, -1.0f);
        //gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f(1.0f, -1.0f, -1.0f);

        gl.glNormal3f(1.0f, 0.0f, 0.0f);
        //gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3f(1.0f, -1.0f, -1.0f); // Right face
        //gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3f(1.0f, 1.0f, -1.0f);
        //gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3f(1.0f, 1.0f, 1.0f);
        //gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f(1.0f, -1.0f, 1.0f);

        gl.glNormal3f(-1.0f, 0.0f, 0.0f);
        //gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f(-1.0f, -1.0f, -1.0f); // Left Face
        //gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3f(-1.0f, -1.0f, 1.0f);
        //gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3f(-1.0f, 1.0f, 1.0f);
        //gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3f(-1.0f, 1.0f, -1.0f);
        gl.glEnd();

        gl.glEndList();

        top = box + 1; // Storage For "Top" Is "Box" Plus One
        gl.glNewList(top, GL2.GL_COMPILE); // Now The "Top" Display List

        gl.glBegin(GL2.GL_QUADS);
        gl.glNormal3f(0.0f, 1.0f, 0.0f);
        //gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3f(-1.0f, 1.0f, -1.0f);// Top Face
        //gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f(-1.0f, 1.0f, 1.0f);
        //gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3f(1.0f, 1.0f, 1.0f);
        //gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3f(1.0f, 1.0f, -1.0f);
        gl.glEnd();
        gl.glEndList();
    }

    public void init(GLAutoDrawable drawable) {
        GL2 gl = (GL2) drawable.getGL();
        gl.glEnable(GL2.GL_TEXTURE_2D); // Enable Texture Mapping
        gl.glShadeModel(GL2.GL_SMOOTH); // Enable Smooth Shading
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.01f); // Black Background
        gl.glClearDepth(1.0f); // Depth Buffer Setup
        gl.glEnable(GL2.GL_DEPTH_TEST); // Enables Depth Testing

// The Type Of Depth Testing To Do
        gl.glDepthFunc(GL2.GL_LEQUAL);

// Quick And Dirty Lighting (Assumes Light0 Is Set Up)
        gl.glEnable(GL2.GL_LIGHT0);

        gl.glEnable(GL2.GL_LIGHTING); // Enable Lighting

// Enable Material Coloring
        gl.glEnable(GL2.GL_COLOR_MATERIAL);

// Really Nice Perspective Calculations
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);

        //loadGLTexture(gl);
        buildLists(gl);
    }

//    private void update() {
//        if (decreaseX)
//            xrot -= 8f;
//        if (increaseX)
//            xrot += 8f;
//        if (decreaseY)
//            yrot -= 8f;
//        if (increaseY)
//            yrot += 8f;
//    }

    public void display(GLAutoDrawable drawable) {
        GL2 gl = (GL2) drawable.getGL();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        //gl.glBindTexture(GL2.GL_TEXTURE_2D, textures[0]);
        int n = 8;
        for (yloop = 1; yloop < n; yloop++) {
            for (xloop = 0; xloop < yloop; xloop++) {
                gl.glLoadIdentity(); // Reset The View

                gl.glTranslatef(1.4f + (xloop * 2.8f) -
                        (yloop * 1.4f), ((6.0f - yloop) * 2.4f) - 7.0f, -20.0f);

                gl.glRotatef(45.0f - (2.0f * yloop) + xrot, 1.0f, 0.0f, 0.0f);
                gl.glRotatef(45.0f + yrot, 0.0f, 1.0f, 0.0f);

                float s = (float)Math.random() + 0.5f;
                gl.glScalef(s, s, s);

                gl.glColor3fv(boxcol[(yloop - 1)%boxcol.length], 0);
                gl.glCallList(box);

                //gl.glColor3fv(topcol[yloop - 1], 0);
                //gl.glCallList(top);
            }
        }
    }

    public void reshape(GLAutoDrawable drawable,
                        int xstart,
                        int ystart,
                        int width,
                        int height) {
        GL2 gl = (GL2) drawable.getGL();

        height = (height == 0) ? 1 : height;

        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        glu.gluPerspective(45, (float) width / height, 1, 1000);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

}