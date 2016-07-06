package nars.vision;

import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.gs.collections.impl.factory.Lists;
import com.gs.collections.impl.map.mutable.primitive.LongObjectHashMap;
import com.jogamp.opengl.GL2;
import nars.$;
import nars.NAR;
import nars.agent.NAgent;
import nars.term.Term;
import nars.term.Termed;
import nars.util.signal.MotorConcept;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.render.ShapeDrawer;

import java.util.Collections;

/**
 * Created by me on 6/5/16.
 */
public class NARCamera implements PixelCamera.PerPixelRGB {

    public static final float minZoom = 0.5f;
    public static final float maxZoom = 5f;

    public final NAgent controller;
    public final PixelCamera cam;
    private final PixelToTerm pixelTerm;

    //final WeakHashMap<IntIntPair,Termed> terms = new WeakHashMap();
    final LongObjectHashMap<Termed> terms = new LongObjectHashMap<>();

    private final NAR nar;
    private final Term id;

    private int cx;
    private int cy;
    private int x = 0, y = 0;

    private PerPixel perPixel;
    private float z;


    public NARCamera(String id, NAR nar, PixelCamera c, PixelToTerm pixelTerm) {
        this.id = $.$(id);
        this.nar = nar;
        this.cam = c;
        this.pixelTerm = pixelTerm;
        this.controller = new NAgent(nar);
        this.cx = 0;
        this.cy = 0;
        this.z = 0.25f;
        controller.start(
                Collections.emptyList(),
                Lists.mutable.of(
                        new MotorConcept("(" + id + ", center)", nar, (b, d) -> {
                            look(0, 0, 0);
                        }),
                        new MotorConcept("(" + id + ", up)", nar, (b, d) -> {
                            look(0, -0.1f, 0);
                        }),
                        new MotorConcept("(" + id + ", down)", nar, (b, d) -> {
                            look(0, +0.1f, 0);
                        }),
                        new MotorConcept("(" + id + ", left)", nar, (b, d) -> {
                            look(-0.1f, 0, 0);
                        }),
                        new MotorConcept("(" + id + ", right)", nar, (b, d) -> {
                            look(+0.1f, 0, 0);
                        }),
                        new MotorConcept("(" + id + ", in)", nar, (b, d) -> {
                            look(0, 0, 0.1f);
                        }),
                        new MotorConcept("(" + id + ", out)", nar, (b, d) -> {
                            look(0, 0, -0.1f);
                        })

                ));

        SwingCamera scam = (SwingCamera) this.cam;

        nar.onFrame(nn -> {
            System.out.println("update camera: @(" + x + "," + y + ")x(" + scam.width + "," + scam.height + ")");
            controller.decide(-1);
        });

        x = scam.inWidth()/2;
        y = scam.inHeight()/2;
    }

    public void look(float dx, float dy, float dz) {
        SwingCamera scam = (SwingCamera) this.cam;
        int w = scam.inWidth();
        int h = scam.inHeight();

        this.x += Math.round(w * dx);

        this.y += Math.round(h * dy);

        this.z += dz;
        this.z = Math.max(minZoom,Math.min(z,maxZoom));

        center(0,0,z);
//
//        int x = cx + px;
//        int y = cy + py;
//
//        if (x < 0) x = 0;
//        if (y < 0) y = 0;
//
//        input(x, y, w, h);
    }

    public void center(int x, int y, float zoom) {
        SwingCamera scam = (SwingCamera) this.cam;

        this.cx = x;
        this.cy = y;
        int iw = Math.round(scam.inWidth() * zoom);
        int ih = Math.round(scam.inHeight() * zoom);
        input(x - iw /2 + this.x, y- ih /2 + this.y, iw, ih);
    }
    public void input(int x, int y, int w, int h) {
        SwingCamera scam = (SwingCamera) this.cam;
        scam.input(x, y, w, h);
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
        update((x, y, t, r, g, b) -> {
            pp.pixel(x, y, t, PixelCamera.rgbToMono(r, g, b));
        });
    }

    @Override
    public void pixel(int x, int y, int aRGB) {
        int r = (aRGB & 0x00ff0000) >> 16;
        int g = (aRGB & 0x0000ff00) >> 8;
        int b = (aRGB & 0x000000ff);
        perPixel.pixel(x, y, p(x, y), r, g, b);
    }

    public final Termed p(int x, int y) {
        return terms.getIfAbsentPutWithKey(l(x, y),
                xy -> nar.index.the(pixelTerm.pixel(x(xy), y(xy))));
    }

    private int x(long xy) {
        return (int) (xy >> 32);
    }

    private int y(long xy) {
        return (int) (xy & 0x0000ffff);
    }

    static long l(int x, int y) {
        return (((long) x) << 32) | ((long) y);
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
            float ar = h / w;

            float th = tw / ar;

            float dw = tw / w;
            float dh = th / h;
            camera.cam.update((x, y, r, g, b, a) -> {
                gl.glColor4f(r, g, b, a);
                ShapeDrawer.rect(gl, x * dw, th - y * dh, dw, dh);
            });

            //border
            gl.glColor4f(1f,1f,1f,1f);
            ShapeDrawer.strokeRect(gl, 0, 0, tw, th);
        }
    }
}
