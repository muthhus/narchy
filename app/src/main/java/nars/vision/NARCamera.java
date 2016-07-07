package nars.vision;

import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.gs.collections.impl.factory.Lists;
import com.gs.collections.impl.map.mutable.primitive.LongObjectHashMap;
import com.jogamp.opengl.GL2;
import nars.$;
import nars.NAR;
import nars.agent.NAgent;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.atom.Atom;
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

    public static final float minZoom = 0.15f;
    public static final float maxZoom = 1f;

    public final NAgent controller;
    public final PixelCamera cam;
    private final PixelToTerm pixelTerm;

    //final WeakHashMap<IntIntPair,Termed> terms = new WeakHashMap();
    final LongObjectHashMap<Termed> terms = new LongObjectHashMap<>();

    private final NAR nar;
    private final Term id;


    private PerPixel perPixel;
    private float z;
    int x, y, ox, oy;

    float xySpeed = 0.02f;
    float zoomSpeed = 0.01f;


    public NARCamera(String id, NAR nar, PixelCamera c, PixelToTerm pixelTerm) {
        this.id = $.$(id);
        this.nar = nar;
        this.cam = c;
        this.pixelTerm = pixelTerm;
        this.controller = new NAgent(nar);
        this.z = 0.25f;
        controller.start(
                Collections.emptyList(),
                Lists.mutable.of(
                        new MotorConcept("(" + id + ", (center))", nar, (b, d) -> {
                            //look(0, 0, 0);
                        }),
                        new MotorConcept("(" + id + ", (up))", nar, (b, d) -> {
                            //look(0, -1, 0);
                        }),
                        new MotorConcept("(" + id + ", (--,(up)))", nar, (b, d) -> {
                            //look(0, +1f, 0);
                        }),
                        new MotorConcept("(" + id + ", (left))", nar, (b, d) -> {
                            //look(-1f, 0, 0);
                        }),
                        new MotorConcept("(" + id + ", (--,(left)))", nar, (b, d) -> {
                            //look(+1f, 0, 0);
                        }),
                        new MotorConcept("(" + id + ", (in))", nar, (b, d) -> {
                            //look(0, 0, +1f);
                        }),
                        new MotorConcept("(" + id + ", (--,(in)))", nar, (b, d) -> {
                            //look(0, 0, -1f);
                        })

                ));

        SwingCamera scam = (SwingCamera) this.cam;

//        nar.onFrame(nn -> {
//            System.out.println("update camera: @(" + x + "," + y + ")x(" + scam.width + "," + scam.height + ")");
//            controller.decide(-1);
//        });

        nar.onFrame(f -> {
            switch (controller.lastAction) {
                case 0: look(0, 0, 0); break;
                case 1: look(1, 0, 0); break;
                case 2: look(-1, 0, 0); break;
                case 3: look(0, 1, 0); break;
                case 4: look(0, -1, 0); break;
                case 5: look(0, 0, 1); break;
                case 6: look(0, 0, -1); break;
            }
        });

        x = scam.inWidth()/2;
        y = scam.inHeight()/2;
    }

    public float red(int x, int y) {
        return ((SwingCamera)cam).red(x, y);
    }
    public float green(int x, int y) {
        return ((SwingCamera)cam).green(x, y);
    }
    public float blue(int x, int y) {
        return ((SwingCamera)cam).blue(x, y);
    }

    public void look(float dx, float dy, float dz) {

        dx *= xySpeed;
        dy *= xySpeed;
        dz *= zoomSpeed;

        SwingCamera scam = (SwingCamera) this.cam;
        int w = scam.inWidth();
        int h = scam.inHeight();

        this.z += dz;
        this.z = Math.max(minZoom,Math.min(z,maxZoom));

        this.x += Math.round(w * z * dx);
        this.x = Math.max(0, Math.min(w, x));

        this.y += Math.round(h * z * dy);
        this.y = Math.max(0, Math.min(h, y));


        center(x,y,z);
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

        int iw = Math.round(scam.inWidth() * zoom);
        int ih = Math.round(scam.inHeight() * zoom);
        input(x - iw /2 + this.ox, y- ih /2 + this.oy, iw, ih);
    }
    public void input(int x, int y, int w, int h) {
        SwingCamera scam = (SwingCamera) this.cam;
        scam.input(x, y, w, h);
        scam.update();
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
            ((SwingCamera)camera.cam).updateBuffered((x, y, r, g, b, a) -> {
                gl.glColor4f(r, g, b, a);
                ShapeDrawer.rect(gl, x * dw, th - y * dh, dw, dh);
            });

            //border
            gl.glColor4f(1f,1f,1f,1f);
            ShapeDrawer.strokeRect(gl, 0, 0, tw+dw, th+dh);
        }
    }


    public static Term quadp(int level, int x, int y, int width, int height) {

        if (width <= 1 || height <= 1) {
            return null; //dir; //$.p(dir);
        }

        int cx = width/2;
        int cy = height/2;

        boolean left = x < cx;
        boolean up = y < cy;


        char c1 = (left ? 'l' : 'r');
        char c2 = (up ? 'u' : 'd');
        if (!left)
            x -= cx;
        if (!up)
            y -= cy;
        int area = width * height;

        //Term d = $.the(c1 + "" + c2);
        //Term d = $.secti($.the(c1), $.the(c2));
        //Term d = $.seti($.the(c1), $.the(c2));
//		Term d = level > 0  ?
//					$.seti($.the(c1), $.the(c2), $.the(area)) :
//					$.seti($.the(c1), $.the(c2) );
        Term d = level > 0  ?
                $.secte($.the(c1), $.the(c2), $.the(area)) :
                $.secte($.the(c1), $.the(c2) );

        //Term dir = $.p(d,$.the(area));
        //Term dir = level == 0 ? d : $.p(d,$.the(area));
        //Term dir = level == 0 ? d : $.p(d,$.the(area));
        //Term dir = level == 0 ? d : $.inh(d,$.the(area));
        //Term dir = $.inh(d,$.the(area));

        //Term dir = $.inh(d,$.the(area));
        //Term dir = level == 0 ? d : $.inh(d,$.the(area));

        Term q = quadp(level+1, x, y, width / 2, height / 2);
        if (q!=null) {
            //return $.p(dir, q);
            //return $.image(0, false, dir, $.sete(q));

            //return $.inh(q, dir);
            //return $.inst(q, (level== 0 ? d : $.seti(d, $.the(area))));
            return $.inst(q, d);
            //return $.diffe(dir, q);

            //return $.sete(q, dir);
            //return $.inst(q, dir);
            //return $.instprop(q, dir);
            //return $.p(q, dir);
            //return $.image(0, false, dir, q);
        }
        else {
            return d;
            //return $.p(dir);
            //return $.inst($.varDep(0), dir);
        }
    }

    private Compound quadpFlat(int x, int y, int width, int height) {
        int cx = width/2;
        int cy = height/2;

        boolean left = x < cx;
        boolean up = y < cy;


        char c1 = (left ? 'l' : 'r');
        char c2 = (up ? 'u' : 'd');
        if (!left)
            x -= cx;
        if (!up)
            y -= cy;

        Atom dir = $.the(c1 + "" + c2);

        if (width>1 || height > 1) {
            Compound q = quadpFlat(x, y, width / 2, height / 2);
            if (q!=null)
                return $.p(Terms.concat(new Term[] { dir }, q.terms()));
            else
                return $.p(dir);
        } else {
            return null; //dir; //$.p(dir);
        }
    }

    public static int log2(int width) {
        return (int)Math.ceil(Math.log(width)/Math.log(2));
    }

    public Term binaryp(int x, int depth) {
        String s = Integer.toBinaryString(x);
        int i = s.length()-1;
        Term n = null;
        for (int d = 0; d < depth; d++, i--) {
            Atom next = $.the(i < 0 || s.charAt(i) == '0' ? 0 : 1);

            //next = $.the( ((char)(d+'a')) + "" + next.toString());

            if (n == null) {
                //n = next;
                n = $.p(next);
                //n = $.p(next, $.varDep(0));
            } else {
                n = $.p(next, n);
                //n = $.inh(n, next);
            }
        }
        return n;
    }

}

