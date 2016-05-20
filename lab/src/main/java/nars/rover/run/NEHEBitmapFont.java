package nars.rover.run;


import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static nars.rover.physics.gl.Box2DJoglPanel.newDefaultConfig;


public class NEHEBitmapFont implements GLEventListener {
    private static int base;  // Base Display List For The Font
    private static final int[] textures = new int[2];  // Storage For Our Font Texture
 
    private float cnt1;    // 1st Counter Used To Move Text & For Coloring
    private float cnt2;    // 2nd Counter Used To Move Text & For Coloring
 
    private final GLU glu = new GLU();
    private static ByteBuffer stringBuffer = ByteBuffer.allocate(256);

    public static void main(String[] args) {

        GLWindow w = GLWindow.create(NewtFactory.createWindow(newDefaultConfig()));
        w.addGLEventListener(new NEHEBitmapFont());

        Animator a = new Animator();
        a.add(w);
        a.start();

        w.setSize(800, 600);
        w.setVisible(true);

    }

    public NEHEBitmapFont() {
    }
 
    public static void loadGLTextures(GL2 gl) {
         
        String tileNames [] = 
            {"font.png", "bumps.png"};
 



        gl.glGenTextures(2, textures, 0);


        for (int i = 0; i < 2; i++) {
            //TextureReader.Texture texture;


            TextureData texture = null;

            try {
                //System.out.println(getClass().getClassLoader().getResource(tileNames[i]).toURI());
                GL var2 = GLContext.getCurrentGL();
                GLProfile var3 = var2.getGLProfile();

                texture = TextureIO.newTextureData(var3, new File(NEHEBitmapFont.class.getClassLoader().getResource(tileNames[i]).toURI()), true, tileNames[i]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //texture = TextureReader.readTexture(tileNames[i]);

            //Create Nearest Filtered Texture
            gl.glBindTexture(GL.GL_TEXTURE_2D, textures[i]);
 
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);



            gl.glTexImage2D(GL.GL_TEXTURE_2D,
                    0,
                    3,
                    texture.getWidth(),
                    texture.getHeight(),
                    0,
                    GL.GL_RGB,
                    GL.GL_UNSIGNED_BYTE,
                    texture.getBuffer());//getPixels());
 
 
        }
        buildFont(gl);

    }
 
    private static void buildFont(GL2 gl)  // Build Our Font Display List
    {
        float cx;      // Holds Our X Character Coord
        float cy;      // Holds Our Y Character Coord
 
        base = gl.glGenLists(256);  // Creating 256 Display Lists
        gl.glBindTexture(GL.GL_TEXTURE_2D, textures[0]);  // Select Our Font Texture
        for (int loop = 0; loop < 256; loop++)      // Loop Through All 256 Lists
        {
            cx = (float) (loop % 16) / 16.0f;  // X Position Of Current Character
            cy = (float) (loop / 16) / 16.0f;  // Y Position Of Current Character
 
            gl.glNewList(base + loop, GL2.GL_COMPILE);  // Start Building A List
            gl.glBegin(GL2.GL_QUADS);      // Use A Quad For Each Character
            gl.glTexCoord2f(cx, 1 - cy - 0.0625f);  // Texture Coord (Bottom Left)
            gl.glVertex2i(0, 0);      // Vertex Coord (Bottom Left)
            gl.glTexCoord2f(cx + 0.0625f, 1 - cy - 0.0625f);  // Texture Coord (Bottom Right)
            gl.glVertex2i(16, 0);      // Vertex Coord (Bottom Right)
            gl.glTexCoord2f(cx + 0.0625f, 1 - cy);  // Texture Coord (Top Right)
            gl.glVertex2i(16, 16);      // Vertex Coord (Top Right)
            gl.glTexCoord2f(cx, 1 - cy);    // Texture Coord (Top Left)
            gl.glVertex2i(0, 16);      // Vertex Coord (Top Left)
            gl.glEnd();          // Done Building Our Quad (Character)
            gl.glTranslated(10, 0, 0);      // Move To The Right Of The Character
            gl.glEndList();        // Done Building The Display List
        }            // Loop Until All 256 Are Built
    }
 
    // Where The Printing Happens
    public static void glPrint(GL2 gl, float x, float y, String string, int set)
    {
        if (set > 1) {
            set = 1;
        }
        gl.glBindTexture(GL.GL_TEXTURE_2D, textures[0]); // Select Our Font Texture
        gl.glDisable(GL.GL_DEPTH_TEST);       // Disables Depth Testing
        //gl.glMatrixMode(GL2.GL_PROJECTION);     // Select The Projection Matrix
        //gl.glPushMatrix();         // Store The Projection Matrix
        //gl.glLoadIdentity();         // Reset The Projection Matrix
        //gl.glOrtho(0, 640, 0, 480, -1, 1);     // Set Up An Ortho Screen
        //gl.glMatrixMode(GL2.GL_MODELVIEW);     // Select The Modelview Matrix
        gl.glPushMatrix();         // Store The Modelview Matrix
        gl.glLoadIdentity();         // Reset The Modelview Matrix
        gl.glTranslated(x, y, 0);       // Position The Text (0,0 - Bottom Left)
        gl.glListBase(base - 32 + (128 * set));     // Choose The Font Set (0 or 1)
 
        if (stringBuffer.capacity() < string.length()) {
            stringBuffer = ByteBuffer.allocate(string.length());
        }
 
        stringBuffer.clear();
        stringBuffer.put(string.getBytes());
        stringBuffer.flip();
         
        // Write The Text To The Screen
        gl.glCallLists(string.length(), GL.GL_BYTE, stringBuffer);      
        //gl.glMatrixMode(GL2.GL_PROJECTION);  // Select The Projection Matrix
        gl.glPopMatrix();      // Restore The Old Projection Matrix
        //gl.glMatrixMode(GL2.GL_MODELVIEW);  // Select The Modelview Matrix
        //gl.glPopMatrix();      // Restore The Old Projection Matrix
        //gl.glEnable(GL.GL_DEPTH_TEST);    // Enables Depth Testing
    }
 
    @Override
    public void init(GLAutoDrawable glDrawable) {
        GL2 gl = glDrawable.getGL().getGL2();
        //try {
            loadGLTextures(gl);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
 

        gl.glShadeModel(GL2.GL_SMOOTH);                 // Enables Smooth Color Shading
         
        // This Will Clear The Background Color To Black
        //gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
         
        // Enables Clearing Of The Depth Buffer
        //gl.glClearDepth(1.0);
         
        //gl.glEnable(GL.GL_DEPTH_TEST);                 // Enables Depth Testing
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);    // Select The Type Of Blending
        gl.glDepthFunc(GL.GL_LEQUAL);                  // The Type Of Depth Test To Do
         
        // Really Nice Perspective Calculations
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
        gl.glEnable(GL.GL_TEXTURE_2D);      // Enable 2D Texture Mapping
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {

    }

    @Override
    public void display(GLAutoDrawable glDrawable) {
        GL2 gl = glDrawable.getGL().getGL2();
         
        // Clear The Screen And The Depth Buffer
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);       
        gl.glLoadIdentity();  // Reset The View
 
        // Select Our Second Texture
        gl.glBindTexture(GL.GL_TEXTURE_2D, textures[1]);  
        gl.glTranslatef(0.0f, 0.0f, -5.0f);  // Move Into The Screen 5 Units
         
        // Rotate On The Z Axis 45 Degrees (Clockwise)
        gl.glRotatef(45.0f, 0.0f, 0.0f, 1.0f);  
         
        // Rotate On The X & Y Axis By cnt1 (Left To Right)
        gl.glRotatef(cnt1 * 30.0f, 1.0f, 1.0f, 0.0f);  
        gl.glDisable(GL.GL_BLEND);          // Disable Blending Before We Draw In 3D
        gl.glColor3f(1.0f, 1.0f, 1.0f);     // Bright White
        gl.glBegin(GL2.GL_QUADS);            // Draw Our First Texture Mapped Quad
        gl.glTexCoord2d(0.0f, 0.0f);        // First Texture Coord
        gl.glVertex2f(-1.0f, 1.0f);         // First Vertex
        gl.glTexCoord2d(1.0f, 0.0f);        // Second Texture Coord
        gl.glVertex2f(1.0f, 1.0f);          // Second Vertex
        gl.glTexCoord2d(1.0f, 1.0f);        // Third Texture Coord
        gl.glVertex2f(1.0f, -1.0f);         // Third Vertex
        gl.glTexCoord2d(0.0f, 1.0f);        // Fourth Texture Coord
        gl.glVertex2f(-1.0f, -1.0f);        // Fourth Vertex
        gl.glEnd();                         // Done Drawing The First Quad
         
        // Rotate On The X & Y Axis By 90 Degrees (Left To Right)
        gl.glRotatef(90.0f, 1.0f, 1.0f, 0.0f);  
        gl.glBegin(GL2.GL_QUADS);            // Draw Our Second Texture Mapped Quad
        gl.glTexCoord2d(0.0f, 0.0f);        // First Texture Coord
        gl.glVertex2f(-1.0f, 1.0f);         // First Vertex
        gl.glTexCoord2d(1.0f, 0.0f);        // Second Texture Coord
        gl.glVertex2f(1.0f, 1.0f);          // Second Vertex
        gl.glTexCoord2d(1.0f, 1.0f);        // Third Texture Coord
        gl.glVertex2f(1.0f, -1.0f);         // Third Vertex
        gl.glTexCoord2d(0.0f, 1.0f);        // Fourth Texture Coord
        gl.glVertex2f(-1.0f, -1.0f);        // Fourth Vertex
        gl.glEnd();                         // Done Drawing Our Second Quad
        gl.glEnable(GL.GL_BLEND);           // Enable Blending
 
        gl.glLoadIdentity();                // Reset The View
         
        // Pulsing Colors Based On Text Position
        gl.glColor3f((float) (Math.cos(cnt1)), (float) 
                (Math.sin(cnt2)), 1.0f - 0.5f * (float) (Math.cos(cnt1 + cnt2)));
         
        // Print GL Text To The Screen
        glPrint(gl, (int) ((280 + 250 * Math.cos(cnt1))), 
                (int) (235 + 200 * Math.sin(cnt2)), "NeHe", 0);    
 
        gl.glColor3f((float) (Math.sin(cnt2)), 1.0f - 0.5f * 
                (float) (Math.cos(cnt1 + cnt2)), (float) (Math.cos(cnt1)));
         
        // Print GL Text To The Screen
        glPrint(gl, (int) ((280 + 230 * Math.cos(cnt2))), 
                (int) (235 + 200 * Math.sin(cnt1)), "OpenGL", 1);  
 
        gl.glColor3f(0.0f, 0.0f, 1.0f);
        glPrint(gl, (int) (240 + 200 * Math.cos((cnt2 + cnt1) / 5)), 
                2, "Giuseppe D'Agata", 0);
 
        gl.glColor3f(1.0f, 1.0f, 1.0f);
        glPrint(gl, (int) (242 + 200 * Math.cos((cnt2 + cnt1) / 5)), 
                2, "Giuseppe D'Agata", 0);
 
        cnt1 += 0.01f;      // Increase The First Counter
        cnt2 += 0.0081f;    // Increase The Second Counter
    }
 
    @Override
    public void reshape(GLAutoDrawable glDrawable, int x, int y, int w, int h) {
        if (h == 0) h = 1;
        GL2 gl = glDrawable.getGL().getGL2();
         
        // Reset The Current Viewport And Perspective Transformation
        gl.glViewport(0, 0, w, h);                       
        gl.glMatrixMode(GL2.GL_PROJECTION);    // Select The Projection Matrix
        gl.glLoadIdentity();                  // Reset The Projection Matrix
         
        // Calculate The Aspect Ratio Of The Window
        glu.gluPerspective(45.0f, (float) w / (float) h, 0.1f, 100.0f);  
        gl.glMatrixMode(GL2.GL_MODELVIEW);    // Select The Modelview Matrix
        gl.glLoadIdentity();                 // Reset The ModalView Matrix
    }
 
    public void displayChanged(GLAutoDrawable glDrawable, boolean b, boolean b1) {
    }
}