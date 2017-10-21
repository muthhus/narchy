package spacegraph;

import com.jogamp.newt.event.*;
import com.jogamp.opengl.GL2;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.Finger;
import spacegraph.math.v2;
import spacegraph.phys.util.AnimVector2f;
import spacegraph.phys.util.AnimVector3f;
import spacegraph.phys.util.Animated;

/**
 * orthographic widget adapter. something which goes on the "face" of a HUD ("head"s-up-display)
 */
public class Ortho extends Surface implements SurfaceRoot, WindowListener, KeyListener, MouseListener {

    boolean visible;

    final Finger finger;

    public Surface surface;
    private boolean maximize;
    public SpaceGraph window;


    public Ortho() {
        this.finger = new Finger(this);
        this.scale = new AnimVector2f(1, 1, 6f);
        this.pos = new AnimVector3f(8f);
    }

    public Ortho(Surface content) {
        this();
        setSurface(content);
    }

    public void setSurface(Surface content) {
        this.surface = content;
    }


    public void start(SpaceGraph s) {
        this.window = s;
        s.addWindowListener(this);
        s.addMouseListener(this);
        s.addKeyListener(this);
        s.dyn.addAnimation((Animated) scale);
        s.dyn.addAnimation((Animated) pos);
        surface.start(this);
        surface.layout();
        resized();
    }

    @Override
    public SurfaceRoot root() {
        return this;
    }

    @Override
    public Ortho translate(float x, float y) {
        pos.set(x, y, 0);
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

        //System.out.println(x + "," + y + " " + sx + "x" + sy + ".." + scale);
        v2 gs = scale;
        translate(-(x - 1) * scale.x/2,  -(y - 1)*scale.y/2);

        //scale()
    }

    @Override
    public Ortho scale(float sx, float sy) {
        scale.set(sx, sy);
        return this;
    }


    @Override
    protected void paint(GL2 gl) {
        gl.glTranslatef(-0.5f, -0.5f, 0);
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
            int W = window.getWidth();
            int H = window.getHeight();
            scale(W, H);
            translate(W / 2, H / 2);
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

        } else {

            x = y = Float.NaN;
            updateMouse(null, x, y, null);

            return false;
        }
    }

    public Surface updateMouse(@Nullable MouseEvent e, float sx, float sy, short[] buttonsDown) {

        if (e != null) {
            if (window != null) {
                if (window.window != null) {
                    Finger.pointer.set(window.windowX + e.getX(), window.windowY + e.getY());
                }
            }
            e.setConsumed(true);
        }

        /*if (e == null) {
            off();
        } else {*/
        Surface s;
        float lx = 0.5f + (sx - pos.x) / (scale.x);
        float ly = 0.5f + (sy - pos.y) / (scale.y);
        //System.out.println(lx + " " + ly);
        //if (lx >= 0 && ly >= 0 && lx <= 1f && ly <= 1f) {
        if ((s = finger.on(sx, sy, lx, ly, buttonsDown)) != null) {
            return s;
        } else {
            return null;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {

        updateMouse(e);
    }


    @Override
    public void mouseWheelMoved(MouseEvent e) {

    }


}
