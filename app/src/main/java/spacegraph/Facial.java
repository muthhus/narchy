package spacegraph;

import com.jogamp.opengl.GL2;
import spacegraph.obj.ConsoleSurface;

/**
 * orthographic widget adapter
 */
public class Facial {

    public static void main(String[] args) {
        SpaceGraph s = new SpaceGraph();
        s.add(new Facial(new ConsoleSurface(80, 25)).scale(0.5f));
        s.show(800, 600);
    }

    final Surface surface;

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

    }

    public void render(GL2 gl) {
        surface.render(gl);
    }
}
