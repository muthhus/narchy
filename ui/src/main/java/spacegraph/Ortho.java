package spacegraph;

import com.jogamp.nativewindow.util.Point;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL2;
import spacegraph.input.Finger;
import spacegraph.math.v2;
import spacegraph.phys.util.AnimVector2f;

import java.util.Arrays;

import static spacegraph.Surface.Align.None;
import static spacegraph.math.v3.v;

/**
 * orthographic widget adapter. something which goes on the "face" of a HUD ("head"s-up-display)
 */
public class Ortho implements WindowListener, KeyListener, MouseListener {

    public final AnimVector2f translate;
    boolean visible;

    final Finger finger;


//    public static void main(String[] args) {
//        SpaceGraph s = new SpaceGraph();
//        s.add(new Facial(new ConsoleSurface(80, 25)).scale(0.5f));
//        s.show(800, 600);
//    }

    public final Surface surface;
    private boolean maximize;
    public SpaceGraph window;
    protected AnimVector2f scale;

    public Ortho(Surface content) {


        this.surface = content;
        surface.align = None;

        this.finger = newFinger();
        this.scale = new AnimVector2f(5f);
        this.translate = new AnimVector2f(3f);


    }

    protected Finger newFinger() {
        return new Finger(this) {
            @Override
            public void zoom(Surface s) {

            }
        };
    }



    public Ortho translate(float x, float y) {
        translate.set(x, y);
        return this;
    }

    public Ortho move(float x, float y) {
        translate.add(x, y);
        return this;
    }

    public Ortho scale(float s) {
        return scale(s, s);
    }

    public v2 scale() {
        return scale;
    }

    public Ortho scale(float sx, float sy) {
        scale.set(sx, sy);
        return this;
    }

    public void start(SpaceGraph s) {
        this.window = s;
        s.addWindowListener(this);
        s.addMouseListener(this);

        s.addKeyListener(this);
        s.dyn.addAnimation(scale);
        s.dyn.addAnimation(translate);
        surface.start(null);
        surface.layout();
        resized();

        window.addFrameListener(f -> {
            try {
                surface.onTouch(finger);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void render(GL2 gl) {
        gl.glPushMatrix();

        gl.glTranslatef(translate.x, translate.y, 0);

        surface.scaleLocal.set(scale);
        //surface.translateLocal.set(translate.x, translate.y);
        surface.render(gl, v(1, 1)
                //(v2) v(window.getWidth(), window.getHeight())
                //v(window.getWidth(),window.getHeight()).normalize().scale(Math.min(window.getWidth(),window.getHeight()))
        );

        gl.glPopMatrix();
    }

    /**
     * expand to window
     */
    public Ortho maximize() {
        maximize = true;
        resized();
        return this;
    }

    @Override
    public void windowResized(WindowEvent e) {
        resized();
    }

    private void resized() {
        //TODO resize preserving aspect, translation, etc
        if (maximize && window != null) {
            scale(window.getWidth(), window.getHeight());
            translate(0, 0);
        }
    }

    @Override
    public void windowMoved(WindowEvent e) {

    }

    @Override
    public void windowDestroyNotify(WindowEvent e) {

    }

    @Override
    public void windowDestroyed(WindowEvent e) {
        stop();
    }

    private void stop() {
        visible = false;
        surface.stop();
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {

    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        //updateMousePosition(null);
    }

    @Override
    public void windowRepaint(WindowUpdateEvent e) {

        visible = true;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        surface.onKey(e, true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        surface.onKey(e, false);
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        updateMousePosition(e);
        updateMouseButtons(e, false);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        finger.nextHit = null;
        finger.nextButtonDown = null;
        //updateMouse(null);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        updateMouseButtons(e, true);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        updateMouseButtons(e, false);
    }

    private void updateMouseButtons(MouseEvent e, boolean down) {
        //TODO do a copy on write here, an interruption could accidentally click its own buttons and stuff
        Arrays.fill(finger.buttonDown, false);

        if (down) {
            finger.nextButtonDown = e.getButtonsDown();
            if (finger.nextButtonDown.length > 0) {
                int which = finger.nextButtonDown[0];
                if (which != -1)
                    finger.buttonDown[which] = true;
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        updateMousePosition(e);
    }

    private void updateMousePosition(MouseEvent e) {
        updateMouse(e.getX(), e.getY());
    }

    private boolean updateMouse(int ex, int ey) {
        float x, y;

        //if (e != null && !e.isConsumed()) {

        //screen coordinates
        float sx = ex;
        float sy = window.getHeight() - ey;

        //screen to local
        float lx = scale.x;
        float ly = scale.y;

        x = (sx - translate.x) / (lx);
        y = (sy - translate.y) / (ly);

        if (x >= 0 && y >= 0 && x <= 1f && y <= 1f) {

            finger.nextHit = new v2(x, y);

            Ortho r = finger.root;
            if (r != null) {
                if (r.window != null) {
                    GLWindow rww = r.window.window;
                    if (rww != null) {
                        Point p = rww.getLocationOnScreen(new Point());
                        finger.pointer.set(p.getX() + ex, p.getY() + ey);
                    }
                }
            }

            return true;
        }

        finger.nextHit = null;


        return false;
    }


    @Override
    public void mouseDragged(MouseEvent e) {
        updateMousePosition(e);
    }


    @Override
    public void mouseWheelMoved(MouseEvent e) {

    }

}
