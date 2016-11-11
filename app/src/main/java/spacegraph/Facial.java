package spacegraph;

import com.jogamp.newt.Window;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.Finger;

import static spacegraph.math.v3.v;

/**
 * orthographic widget adapter. something which goes on the "face" of a HUD ("head"s-up-display)
 */
public class Facial implements WindowListener, KeyListener, MouseListener {

    boolean visible;

    final Finger finger;


//    public static void main(String[] args) {
//        SpaceGraph s = new SpaceGraph();
//        s.add(new Facial(new ConsoleSurface(80, 25)).scale(0.5f));
//        s.show(800, 600);
//    }

    final Surface surface;
    private boolean maximize;
    private SpaceGraph window;

    public Facial(Surface surface) {
        this.surface = surface;
        this.finger = new Finger(surface);
    }

    public Facial move(float x, float y) {
        surface.translateLocal.set(x, y, 0);
        return this;
    }

    public Facial scale(float s) {
        return scale(s, s);
    }

    public Facial scale(float sx, float sy) {
        surface.scaleLocal.set(sx, sy);
        return this;
    }

    public void start(SpaceGraph s) {
        this.window = s;
        resized(s.window);
        s.addWindowListener(this);
        s.addMouseListener(this);
        s.addKeyListener(this);
    }

    public void render(GL2 gl) {
        surface.render(gl);
    }

    /**
     * expand to window
     */
    public Facial maximize() {
        maximize = true;
        return this;
    }

    @Override
    public void windowResized(WindowEvent e) {
        resized((Window) e.getSource());
    }

    private void resized(GLWindow window) {
        if (maximize) {
            scale(window.getWidth(), window.getHeight());
        }
    }

    private void resized(Window window) {
        if (maximize) {
            scale(window.getWidth(), window.getHeight());
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

    }

    @Override
    public void windowRepaint(WindowUpdateEvent e) {
        visible = true;
    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        update(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        update(null);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        update(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        update(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        update(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        update(e);
    }

    private void update(@Nullable MouseEvent e) {
        if (e == null) {
            finger.off();
        } else {
            float x = ((float) e.getX()) / window.getWidth();
            float y = 1f - ((float) e.getY()) / window.getHeight();
            finger.on(v(x, y), e.getButtonsDown());
        }
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {

    }
}
