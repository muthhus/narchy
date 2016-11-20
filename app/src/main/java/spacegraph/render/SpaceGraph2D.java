package spacegraph.render;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import spacegraph.AbstractSpace;
import spacegraph.SimpleSpatial;
import spacegraph.SpaceGraph;
import spacegraph.Spatial;
import spacegraph.input.KeyXY;
import spacegraph.input.KeyXYZ;
import spacegraph.input.OrbMouse;
import spacegraph.math.v3;
import spacegraph.phys.Collidable;
import spacegraph.phys.collision.ClosestRay;

import java.util.function.BiConsumer;

import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;
import static spacegraph.math.v3.v;

/**
 * 2D ortho view of physics space
 */
public class SpaceGraph2D<X> extends SpaceGraph<X> {


    private final BiConsumer<Spatial, Collidable> renderer2D = (s,body)->{
        gl.glPushMatrix();

        Draw.transform(gl, body.transform());

        s.renderRelative(gl, body);

        gl.glPopMatrix();
    };

    public SpaceGraph2D(AbstractSpace<X, ?>... cc) {
        super(cc);
        renderer = renderer2D;
    }

    public SpaceGraph2D(SimpleSpatial... x) {
        super(x);
    }


    float camWidth=1, camHeight=1;


    protected void ortho(float cx, float cy, float scale) {
        int w = getWidth();
        int h = getHeight();
        gl.glViewport(0, 0, w, h);
        gl.glMatrixMode(GL_PROJECTION);
        gl.glLoadIdentity();


        float aspect = h/((float)w);

        this.zNear = -scale;
        this.zFar = scale;

        //gl.glOrtho(-2.0, 2.0, -2.0, 2.0, -1.5, 1.5);
        camWidth = scale;
        camHeight = aspect * scale;
        gl.glOrtho(cx-camWidth/2f, cx+camWidth/2f, cy - camHeight/2f,cy + camHeight/2f,
                zNear, zFar);

//        // switch to projection mode
//        gl.glMatrixMode(gl.GL_PROJECTION);
//        // save previous matrix which contains the
//        //settings for the perspective projection
//        // gl.glPushMatrix();
//        // reset matrix
//        gl.glLoadIdentity();
//        // set a 2D orthographic projection
//        glu.gluOrtho2D(0f, screenWidth, 0f, screenHeight);
//        // invert the y axis, down is positive
//        //gl.glScalef(1f, -1f, 1f);
//        // mover the origin from the bottom left corner
//        // to the upper left corner
//        //gl.glTranslatef(0f, -screenHeight, 0f);
        gl.glMatrixMode(gl.GL_MODELVIEW);
        //gl.glLoadIdentity();

        //gl.glDisable(GL2.GL_DEPTH_TEST);




        //gl.glTranslatef(cx + w/2f, cy + h/2f, 0);

//        float s = Math.min(w, h);
//        gl.glScalef(scale*s,scale*s,1f);
    }

    @Override
    public void updateCamera() {
        float minZoomArea = 1f;
        float z =  camPos.z;
        if (z < minZoomArea)
            camPos.z = z = minZoomArea;

        //tan(A) = opposite/adjacent
        //tan(focus/2) = scale / Z
        //scale = z * tan(focus/2)
        float scale = z;// * 0.41f; //tan(pi/8)
        ortho(camPos.x,camPos.y, scale);
    }

    @Override
    protected void initLighting() {
        //none
    }

    @Override
    protected void initInput() {

        addKeyListener(new KeyXYZ(this));
        addMouseListener(new OrbMouse(this) {
            @Override public ClosestRay mousePick(v3 rayTo) {
                ClosestRay r = this.rayCallback;
                v3 camPos = v(rayTo.x, rayTo.y, SpaceGraph2D.this.camPos.z); //directly down

                space.dyn.rayTest(camPos, rayTo, r.set(camPos, rayTo), simplexSolver);
                return r;
            }
        });

    }

    public v3 rayTo(int x, int y) {
        float height = getHeight();
        return rayTo(  x / ((float) getWidth()),   (height-y) / height);
    }


    @Override
    public v3 rayTo(float x, float y, float depth) {
        return v(
                camPos.x - camWidth/2 + (camWidth * x),
                camPos.y-camHeight/2 + (camHeight * y),
                camPos.z-depth );
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



}
