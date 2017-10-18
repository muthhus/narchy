package spacegraph;

import com.jogamp.nativewindow.util.Point;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL2;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.Finger;
import spacegraph.layout.Stacking;
import spacegraph.math.v2;
import spacegraph.render.Draw;
import spacegraph.widget.Widget;

import static spacegraph.Surface.Align.None;

/**
 * orthographic widget adapter. something which goes on the "face" of a HUD ("head"s-up-display)
 */
public class Ortho extends Surface implements SurfaceRoot, WindowListener, KeyListener, MouseListener {

    boolean visible;

    final Finger finger;

    public Surface surface;
    private boolean maximize;
    public SpaceGraph window;

    public Ortho(Surface content) {

        HUDSurface.set(content);
        this.surface = HUDSurface;


        surface.align = None;
        surface.aspect = 1f;

        this.finger = new Finger(this);
//        this.scale = new AnimVector2f(3f);
//        this.pos = new AnimVector2f(3f);
    }

    @Override
    public SurfaceRoot root() {
        return this;
    }

    @Override
    public Ortho translate(float x, float y) {
        pos.set(x, y);
        return this;
    }

    @Override
    public Ortho move(float x, float y) {
        pos.add(x, y, 0);
        return this;
    }

    @Override
    public Ortho scale(float s) {
        return scale(s, s);
    }

    @Override
    public void zoom(float x, float y, float sx, float sy) {

        v2 gs = scale;
        float tx = x * gs.x;
        float ty = y * gs.y;
        translate(-tx, -ty);

        //scale()
    }

    @Override
    public Ortho scale(float sx, float sy) {
        scale.set(sx, sy);
        return this;
    }

    public void start(SpaceGraph s) {
        this.window = s;
        s.addWindowListener(this);
        s.addMouseListener(this);

        s.addKeyListener(this);
//        s.dyn.addAnimation(scale);
//        s.dyn.addAnimation(pos);
        surface.start(this);
        surface.layout();
        resized();
    }


    @Override
    protected void paint(GL2 gl) {
        surface.render(gl);
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
        visible = false;
        stop();
    }

    @Override
    public void stop() {
        surface.stop();
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {

    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        updateMouse(null);
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
        updateMouse(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        updateMouse(null);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        updateMouse(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        short[] bd = e.getButtonsDown();
        int ii = ArrayUtils.indexOf(bd, e.getButton());
        bd[ii] = -1;
        updateMouse(e, bd);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        updateMouse(e);
    }

    private void updateMouse(MouseEvent e) {
        updateMouse(e, e != null ? e.getButtonsDown() : null);
    }


    private boolean updateMouse(MouseEvent e, short[] buttonsDown) {
        float x, y;

        if (e != null && !e.isConsumed()) {

            //screen coordinates
            float sx = e.getX();
            float sy = window.getHeight() - e.getY();

            //if (!HUDSurface.updateMouseHUD(sx, sy, buttonsDown)) {

            updateMouse(e, sx, sy, buttonsDown);
            return true;
            //}

        }

        x = y = Float.NaN;
        updateMouse(null, x, y, null);

        return false;
    }

    public Surface updateMouse(@Nullable MouseEvent e, float sx, float sy, short[] buttonsDown) {

        if (e != null) {
            if (window != null) {
                SpaceGraph rw = window;
                GLWindow rww = rw.window;
                if (rww != null) {
                    Point p = rww.getLocationOnScreen(new Point());
                    Finger.pointer.set(p.getX() + e.getX(), p.getY() + e.getY());
                }
            }
            e.setConsumed(true);
        }

        /*if (e == null) {
            off();
        } else {*/
        Surface s;
        float lx = (sx - pos.x) / (scale.x);
        float ly = (sy - pos.y) / (scale.y);
        //if (lx >= 0 && ly >= 0 && lx <= 1f && ly <= 1f) {
        if ((s = finger.on(sx, sy, lx, ly, buttonsDown)) != null) {
            return s;
        }
        //}

        //}

        return null;
    }

    @Override
    public void mouseDragged(MouseEvent e) {

        updateMouse(e);
    }


    @Override
    public void mouseWheelMoved(MouseEvent e) {

    }


    final HUD HUDSurface = new HUD();


    private class HUD extends Stacking {

        float smx, smy;

//        @Override
//        protected void paint(GL2 gl) {
//            gl.glPushMatrix();
//            gl.glLoadIdentity();
//            super.paint(gl);
//            gl.glPopMatrix();
//        }

        final Widget bottomRightMenu = new Widget() {

            @Override
            protected void paintComponent(GL2 gl) {

            }
        };


        @Override
        protected void paint(GL2 gl) {
            super.paint(gl);

            gl.glPushMatrix();
            gl.glLoadIdentity();

            int W = window.getWidth();
            int H = window.getHeight();

            gl.glColor4f(0.8f, 0.6f, 0f, 0.25f);

            int borderThick = 8;
            gl.glLineWidth(borderThick);
            Draw.line(gl, 0, 0, W, 0);
            Draw.line(gl, 0, 0, 0, H);
            Draw.line(gl, W, 0, W, H);
            Draw.line(gl, 0, H, W, H);


            gl.glLineWidth(8f);

            float ch = 175f; //TODO proportional to ortho height (pixels)
            float cw = 175f; //TODO proportional to ortho width (pixels)

            gl.glColor4f(0.5f, 0.5f, 0.5f, 0.25f);
            Draw.rectStroke(gl, smx - cw / 2f, smy - ch / 2f, cw, ch);

            gl.glColor4f(0.5f, 0.5f, 0.5f, 0.5f);
            Draw.line(gl, smx, smy - ch, smx, smy + ch);
            Draw.line(gl, smx - cw, smy, smx + cw, smy);
            gl.glPopMatrix();

        }

        {
//            set(
//                    overlay
//                    //bottomRightMenu.scale(64,64)
//            );
        }

        {
//            clipTouchBounds = false;
        }

        boolean canDragBR = false;

        @Override
        public Surface onTouch(Finger finger, v2 hitPoint, short[] buttons) {

            //System.out.println(hitPoint);
            if (hitPoint != null) {
                float hudMarginThick = 0.05f; //pixels

                smx = finger.hitGlobal.x;
                smy = finger.hitGlobal.y;

                //boolean nearEdge = Math.abs(sx - )
                canDragBR = (smx > 1f - hudMarginThick && smy > hudMarginThick);
//                    if (canDragBR) {
//                        System.out.println("draggable");
//                    }
            } else {
                canDragBR = false;
            }

            Surface x = super.onTouch(finger, hitPoint, buttons);

            if (x == this) {
                return null; //pass-thru
            } else
                return x;
        }

    }
}
