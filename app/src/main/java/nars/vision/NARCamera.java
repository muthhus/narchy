package nars.vision;

import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.gs.collections.impl.map.mutable.primitive.LongObjectHashMap;
import com.jogamp.opengl.GL2;
import nars.$;
import nars.NAR;
import nars.index.TermIndex;
import nars.term.Termed;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.obj.CrosshairSurface;
import spacegraph.render.ShapeDrawer;

/**
 * Created by me on 6/5/16.
 */
public class NARCamera implements PixelCamera.PerPixelRGB {

    public final PixelCamera cam;
    private final PixelToTerm pixelTerm;

    //final WeakHashMap<IntIntPair,Termed> terms = new WeakHashMap();
    final LongObjectHashMap<Termed> terms = new LongObjectHashMap<>();

    private final TermIndex index;
    private PerPixel perPixel;

    public NARCamera(PixelCamera c, PixelToTerm pixelTerm) {
        this($.terms, c, pixelTerm);
    }

    public NARCamera(NAR nar, PixelCamera c, PixelToTerm pixelTerm) {
        this(nar.index, c, pixelTerm);
    }

    public NARCamera(TermIndex index, PixelCamera c, PixelToTerm pixelTerm) {
        this.index = index;
        this.cam = c;
        this.pixelTerm = pixelTerm;
    }


    public interface PerPixel {
        void pixel(int x, int y, Termed t, int r, int g, int b);
    }
    public interface PerPixelMono {
        void pixel(int x, int y, Termed t, float w);
    }
    public interface PixelToTerm {
        Termed pixel(int x, int y);
    }

    public synchronized void update(PerPixel pp) {
        this.perPixel = pp;
        cam.update(this);
    }
    public void updateMono(PerPixelMono pp) {
        update((x,y,t,r,g,b) -> {
           pp.pixel(x,y,t,PixelCamera.rgbToMono(r,g,b));
        });
    }

    @Override
    public void pixel(int x, int y, int aRGB) {
        int r = (aRGB & 0x00ff0000) >> 16;
        int g = (aRGB & 0x0000ff00) >> 8;
        int b = (aRGB & 0x000000ff);
        perPixel.pixel(x, y, p(x,y), r, g, b);
    }

    public final Termed p(int x, int y) {
        return terms.getIfAbsentPutWithKey(l(x, y),
                xy -> index.the(pixelTerm.pixel(x(xy), y(xy))));
    }

    private int x(long xy) {
        return (int)(xy >> 32);
    }

    private int y(long xy) {
        return (int)(xy & 0x0000ffff);
    }

    static long l(int x, int y) {
        return (((long)x) << 32) | ((long)y);
    }

    public static void newWindow(NARCamera camera) {
        SpaceGraph<VirtualTerminal> s = new SpaceGraph<>();
        s.show(500, 500);

        s.add(new Facial(new CameraViewer(camera)));
        //s.add(new Facial(new CrosshairSurface(s)));
    }

    private static class CameraViewer extends Surface {
        private final NARCamera camera;
        float tw = 400f;

        public CameraViewer(NARCamera camera) {
            this.camera = camera;
        }

        @Override
        protected void paint(GL2 gl) {

            int w = camera.cam.width();
            int h = camera.cam.height();
            float ar = h/w;

            float th = tw/ar;

            float dw = tw/w;
            float dh = th/h;
            camera.cam.update((x,y,r,g,b,a)->{
                gl.glColor4f(r,g,b,a);
                ShapeDrawer.rect(gl, x*dw, th - y*dh, dw, dh);
            });
        }
    }
}
