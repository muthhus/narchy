package spacegraph;

import com.jogamp.newt.Window;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL2;
import jogamp.newt.driver.bcm.vc.iv.WindowDriver;
import spacegraph.obj.ConsoleSurface;

/**
 * orthographic widget adapter
 */
public class Facial implements WindowListener {

    boolean visible;


//    public static void main(String[] args) {
//        SpaceGraph s = new SpaceGraph();
//        s.add(new Facial(new ConsoleSurface(80, 25)).scale(0.5f));
//        s.show(800, 600);
//    }

    final Surface surface;
    private boolean maximize = false;

    public Facial(Surface surface) {
        this.surface = surface;
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
        resized(s.window);
        s.addWindowListener(this);
    }

    public void render(GL2 gl) {
        surface.render(gl);
    }

    /** expand to window */
    public Facial maximize() {
        maximize = true;
        return this;
    }

    @Override
    public void windowResized(WindowEvent e) {
        resized((Window)e.getSource());
    }

    private void resized(GLWindow window) {
        if (maximize) {
            scale( window.getWidth(), window.getHeight() );
        }
    }
    private void resized(Window window) {
        if (maximize) {
            scale( window.getWidth(), window.getHeight() );
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
}
