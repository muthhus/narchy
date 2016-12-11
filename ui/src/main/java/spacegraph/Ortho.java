package spacegraph;

import com.jogamp.newt.event.*;
import com.jogamp.opengl.GL2;
import org.apache.commons.lang3.ArrayUtils;
import spacegraph.input.Finger;
import spacegraph.math.v2;
import spacegraph.phys.util.AnimVector2f;

import static spacegraph.math.v3.v;

/**
 * orthographic widget adapter. something which goes on the "face" of a HUD ("head"s-up-display)
 */
public class Ortho implements WindowListener, KeyListener, MouseListener {

    protected final AnimVector2f translate;
    boolean visible;

    final Finger mouse;


//    public static void main(String[] args) {
//        SpaceGraph s = new SpaceGraph();
//        s.add(new Facial(new ConsoleSurface(80, 25)).scale(0.5f));
//        s.show(800, 600);
//    }

    final Surface surface;
    private boolean maximize;
    public SpaceGraph window;
    protected AnimVector2f scale;

    public Ortho(Surface surface) {
        this.surface = surface;
        this.mouse = new Finger(surface);
        this.scale = new AnimVector2f(3f);
        this.translate = new AnimVector2f(3f);
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
        surface.layout();
        resized();
    }


    public void render(GL2 gl) {
        gl.glPushMatrix();

        gl.glTranslatef(translate.x, translate.y, 0);

        surface.scaleLocal.set(scale);
        //surface.translateLocal.set(translate.x, translate.y);
        surface.render(gl, v(1,1)
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
        if (maximize && window!=null) {
            scale(window.getWidth(), window.getHeight());
            translate(0,0);
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
        updateMouse(e, e!=null ? e.getButtonsDown() : null);
    }

    private boolean updateMouse(MouseEvent e, short[] buttonsDown) {
        float x, y;

        if (e != null && !e.isConsumed()) {

            //screen coordinates
            float sx = e.getX();
            float sy = window.getHeight() - e.getY();

            //screen to local
            float lx = scale.x;
            float ly = scale.y;

            x = (sx - translate.x) / (lx);
            y = (sy - translate.y) / (ly);
            if (x >= 0 && y >= 0 && x<=1f && y<=1f) {
                mouse.update(e, x, y, buttonsDown);
                return true;
            }

        }

        x = y = Float.NaN;
        mouse.update(null, x, y, null);

        return false;
    }

    @Override
    public void mouseDragged(MouseEvent e) {

        updateMouse(e);
    }



    @Override
    public void mouseWheelMoved(MouseEvent e) {

    }

}
