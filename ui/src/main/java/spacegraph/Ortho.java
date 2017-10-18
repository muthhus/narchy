package spacegraph;

import com.jogamp.nativewindow.util.Point;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL2;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.Finger;
import spacegraph.layout.Layout;
import spacegraph.layout.Stacking;
import spacegraph.math.v2;
import spacegraph.render.Draw;
import spacegraph.widget.Widget;

import static spacegraph.Surface.Align.None;
import static spacegraph.math.v3.v;

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

        this.surface =
            new Stacking(
                content,
                HUDSurface
            );

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

            x = (sx - pos.x) / (scale.x);
            y = (sy - pos.y) / (scale.y);
            if (x >= 0 && y >= 0 && x <= 1f && y <= 1f) {
                updateMouse(e, x, y, buttonsDown);
                return true;
            }


        }

        x = y = Float.NaN;
        updateMouse(null, x, y, null);

        return false;
    }

    public Surface updateMouse(@Nullable MouseEvent e, float x, float y, short[] buttonsDown) {

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
        if ((s = finger.on(v(x, y), buttonsDown)) != null) {
            return s;
        }

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


    private class HUD extends Layout {

        @Override
        protected void paint(GL2 gl) {
            gl.glPushMatrix();
            gl.glLoadIdentity();
            super.paint(gl);
            gl.glPopMatrix();
        }

        Surface windowBorder = new Surface() {


            @Override
            protected void paint(GL2 gl) {
                int W = window.getWidth();
                int H = window.getHeight();


                gl.glColor4f(0.8f, 0.0f, 0.8f, 0.75f);

                int borderThick = 8;
                gl.glLineWidth(borderThick);
                Draw.line(gl, 0, 0, W, 0);
                Draw.line(gl, 0, 0, 0, H);
                Draw.line(gl, W, 0, W, H);
                Draw.line(gl, 0, H, W, H);

                gl.glLineWidth(0);

//        gl.glColor4f(0.5f, 0.5f, 0.5f, 0.5f);
//        gl.glLineWidth(4f);
//
//        float ch = 175f; //TODO proportional to ortho height (pixels)
//        float cw = 175f; //TODO proportional to ortho width (pixels)
//        Draw.rectStroke(gl, this.finger.-cw/2f, smy-ch/2f, cw, ch);
//
//        float hl = 1.25f; //cross hair length
//        Draw.line(gl, smx, smy-ch*hl, smx, smy+ch*hl);
//        Draw.line(gl, smx-cw*hl, smy, smx+cw*hl, smy);

            }
        };

        final Widget bottomRightMenu = new Widget() {

            public boolean hover;

            @Override
            protected boolean onTouching(Finger finger, v2 hitPoint, short[] buttons) {
                hover = hitPoint!=null;
                return true;
            }

            @Override
            protected void paintComponent(GL2 gl) {
                if (hover) {
                    gl.glColor3f(1f, 0f, 0f);
                } else {
                    gl.glColor3f(0f, 1f, 0f);
                }
                Draw.rect(gl, 0, 0, 1, 1);
            }
        };

        {
            float borderPanelThick = 64;
            set(
                    windowBorder,
                    bottomRightMenu
                        .scale(borderPanelThick, borderPanelThick).align(Align.RightCenter)
            );
        }

//        @Override
//        protected boolean onTouching(Finger finger, v2 hitPoint, short[] buttons) {
//            System.out.println(hitPoint);
////           int W = window.getWidth();
////            int H = window.getHeight();
//
//            float hudMarginThick = 0.1f; //pixels
//
//            float sx = hitPoint.x, sy = hitPoint.y;
//            if (sx > 0.9f - hudMarginThick && sy > hudMarginThick) {
//                return true;
//            }
//
//            return false;
//
//        }



    }
}
