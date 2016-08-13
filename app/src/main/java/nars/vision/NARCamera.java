package nars.vision;

import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.gs.collections.api.block.predicate.primitive.IntIntPredicate;
import com.gs.collections.impl.factory.Lists;
import com.gs.collections.impl.map.mutable.primitive.LongObjectHashMap;
import com.jogamp.opengl.GL2;
import nars.$;
import nars.NAR;
import nars.agent.NAgent;
import nars.nal.Tense;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.term.obj.Termject;
import nars.truth.Truth;
import nars.util.Util;
import nars.util.signal.MotorConcept;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.render.Draw;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static nars.vision.PixelCamera.*;

/**
 * Created by me on 6/5/16.
 */
public class NARCamera implements PixelCamera.PerPixelRGB {

    public static final float minZoom = 0.25f;
    public static final float maxZoom = 1f;

    public final NAgent controller;
    public final PixelCamera cam;
    private final PixelToTerm pixelTerm;

    //final WeakHashMap<IntIntPair,Termed> terms = new WeakHashMap();
    final LongObjectHashMap<Termed> terms = new LongObjectHashMap<>();

    private final NAR nar;
    private final Term id;


    private PerPixel perPixel;
    public float z;
    public int x;
    public int y;
    int ox;
    int oy;

    float xySpeed = 1f;
    float zoomSpeed = 0.05f;


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

                        new MotorConcept("cam(up)", nar, (b, d) -> {
                            //look(0, -1*(d), 0);
                            //center(x, (int)(-1 * d * ((SwingCamera) this.cam).inHeight()), z);
                            return d;
                        }),
//                        new MotorConcept("(" + id + "_down)", nar, (b, d) -> {
//                            //look(0, +1f*w2c(d), 0);
//                            //center(x, (int)(+1 * d * ((SwingCamera) this.cam).inHeight()), z);
//                            //return d;
//                        }),
                        new MotorConcept("cam(left)", nar, (b, d) -> {
                            //look(-1f*(d), 0, 0);
                            return d;
                        }),
//                        new MotorConcept("(" + id + "_right)", nar, (b, d) -> {
//                            //look(+1f*(d), 0, 0);
////                            System.out.println("right: " + d);
////                            int w = ((SwingCamera) this.cam).inWidth();
////                            center((int)(+0.5f * d * w + w/2), y, z);
//                           //return d;
//                        }),
                        new MotorConcept("cam(in)", nar, (b, d) -> {
                            //look(0, 0, +1f*(d));
                            return d;
                        })
//                        new MotorConcept("(" + id + "_out)", nar, (b, d) -> {
//                           //look(0, 0, -1f*(d));
//                           //return d;
//                        })

                ));

        SwingCamera scam = (SwingCamera) this.cam;

        for (MotorConcept m : controller.actions) {
            //nar.goal(m, Tense.Eternal, 1f, 0.02f);
            nar.believe(m, Tense.Present, 0.5f, 0.1f); //center
        }
        for (MotorConcept m : controller.actions) {
            //nar.goal(m, Tense.Eternal, 0f, 0.02f);
        }

        nar.onFrame(nn -> {
            //System.out.println("update camera: @(" + x + "," + y + ")x(" + scam.width + "," + scam.height + ")");
            //controller.decide(-1);

            long now = nar.time();

            //float up = desire(1, now);
            //float down = desire(0, now);
            float upDown = desire(0, now) - 0.5f;
            //float upDown =   up - down;
            //float left = desire(3, now);
            //float right = desire(2, now);
            //float leftRight = left - right;
            float leftRight = desire(1, now) - 0.5f;
//            float in = desire(5, now);
//            float out = desire(4, now);
//            float inOut = in - out;
            float inOut = desire(2, now) - 0.5f;


            int ww = ((SwingCamera) this.cam).inWidth();
            int hh = ((SwingCamera) this.cam).inHeight();
            int x = (int) (2 * leftRight * ww / 2f + ww / 2f);
            int y = (int) (2 * upDown * hh / 2f + hh / 2f);
            float z = 0.5f * Util.clampBi(inOut) * (maxZoom - minZoom) + minZoom;

            //System.out.println(leftRight + ".." + x + " "+ upDown + ".." + y);

            center(x, y, z);


            controller.reinforce();

            //float reward = 0.5f * (controller.happy.get() - controller.sad.get());
            float sadness = 1f - controller.happy.asFloat();
            float alpha = 0.8f;
            int mm = 0;
            for (MotorConcept m : controller.actions) {
                if (Math.random() < sadness) {
                    //System.out.println("random train " + m);
                    nar.
                            goal
                            //believe
                                (nar.DEFAULT_GOAL_PRIORITY,
                                    m, nar.time()+1,
                                    (float)Math.random(),
                                    //Util.clamp(desire(mm, nar.time()) + dx * (float) Math.random()), //offset current value by something
                            0.15f + alpha*sadness*0.55f);
                    //0.1f + sadness * (float)Math.random() * 0.8f);

                }
                mm++;
            }


        });


        x = scam.inWidth() / 2;
        y = scam.inHeight() / 2;
    }

    public float desire(int m, long now) {

        MotorConcept cc = controller.actions.get(m);
        if (!cc.hasBeliefs())
            return 0.5f;

        Truth x = cc
                //.desire(now).expectation();
                .belief(now);

        if (x==null)
            return 0.5f;

        float d = x.freq();

        return d;
//        float b = controller.actions.get(m).belief(now).expectation();
//        if (d + b == 0) return 0;
//        return
//                d / (d + b);
    }

    public float red(int x, int y) {
        return ((SwingCamera) cam).red(x, y);
    }

    public float green(int x, int y) {
        return ((SwingCamera) cam).green(x, y);
    }

    public float blue(int x, int y) {
        return ((SwingCamera) cam).blue(x, y);
    }
    public float blue(IntIntPredicate select) {
        final float[] blueTotal = {0};
        final int[] num = {0};
        SwingCamera c = ((SwingCamera) cam);
        c.updateBuffered((x,y,r,g,b,a)-> {
            if (select.accept(x, y)) {
                blueTotal[0] += b;
                num[0]++;
            }
        });
        return blueTotal[0] / num[0];
    }

    public void look(float dx, float dy, float dz) {

        dx *= xySpeed;// * z;
        dy *= xySpeed;// * z;
        dz *= zoomSpeed;

        SwingCamera scam = (SwingCamera) this.cam;
        int w = scam.inWidth();
        int h = scam.inHeight();

        this.z += dz;

        //center(Math.round(w * dx),Math.round(h * dy),z);
//
//        int x = cx + px;
//        int y = cy + py;
//
//        if (x < 0) x = 0;
//        if (y < 0) y = 0;
//
//        input(x, y, w, h);
    }


    public void move(float dx, float dy, float dz) {
        center(Math.round(x + dx * xySpeed), Math.round(y + dy * xySpeed), z + dz * zoomSpeed);
    }

    public void center(int x, int y, float z) {
        this.z = Math.max(minZoom, Math.min(z, maxZoom));

        SwingCamera scam = (SwingCamera) this.cam;
        int w = scam.inWidth();
        int h = scam.inHeight();
        this.x = Math.max(0, Math.min(w, x));
        this.y = Math.max(0, Math.min(h, y));


        int iw = Math.round(scam.inWidth() * z);
        int ih = Math.round(scam.inHeight() * z);
        input(x - iw / 2 + this.ox, y - ih / 2 + this.oy, iw, ih);
    }

    public void input(int x, int y, int w, int h) {
        SwingCamera scam = (SwingCamera) this.cam;
        scam.input(x, y, w, h);
        scam.update();
    }

    public Term snapshot() {
        Set<Term> s = new HashSet();
        ((SwingCamera)cam).updateBuffered((x, y, r, g, b, a)->{
            s.add( $.p(p(x,y).term(), ($.the(b > 0.5f ? "b" : "x")) ) ); //HACK this only does blue plane
        });
        return $.sete(s);
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

    public static void newWindow(SwingCamera camera) {
        SpaceGraph<VirtualTerminal> s = new SpaceGraph<>();
        s.show(700, 800);

        s.add(new Facial(new CameraViewer(camera)));
        //s.add(new Facial(new CrosshairSurface(s)));
    }

    public static class CameraViewer extends Surface {
        private final SwingCamera camera;
        float tw = 1f;

        public CameraViewer(SwingCamera camera) {
            this.camera = camera;
        }

        @Override
        protected void paint(GL2 gl) {

            //gl.glScalef(0.25f,0.25f,0.25f);
            //draw(gl, this.camera.out);
            draw(gl,this.camera.out);
            //drawLegend(gl);

        }
//
//        public void drawLegend(GL2 gl) {
//
//            gl.glPushMatrix();
//
//
//            gl.glColor3f(0.5f,0.5f,0.5f);
//            int hh = camera.inHeight();
//            int ww = camera.inWidth();
//            Draw.strokeRect(gl,0,0, 1, hh);
//
//            if (camera.out!=null)
//                Draw.strokeRect(gl,camera.input.x,camera.input.y, camera.input.width, camera.input.height);
//
//            gl.glPopMatrix();
//
//        }

        private void draw(GL2 gl, BufferedImage b) {
            if (b == null)
                return;

            int w = b.getWidth();
            int h = b.getHeight();
            if ((w == 0) || (h == 0))
                return;

            //float ar = h / w;
            float ar = 1f;


            float th = tw / ar;

            float dw = tw / w;
            float dh = th / h;

            //float a = 1f;
            for (int y = 0; y < b.getHeight(); y++) {
                for (int x = 0; x < b.getWidth(); x++) {
                    int p = b.getRGB(x, y);
                    float r = decodeRed(p);
                    float g = decodeGreen(p);
                    float bl = decodeBlue(p);

                    gl.glColor3f(r, g, bl);
                    Draw.rect(gl, x * dw, th - y * dh, dw, dh);

                }
            }


            //border
            gl.glColor4f(1f, 1f, 1f, 1f);
            Draw.rectStroke(gl, 0, 0, tw + dw, th + dh);
        }


    }


    public static Term quadp(int level, int x, int y, int width, int height) {

        if (width <= 1 || height <= 1) {
            return null; //dir; //$.p(dir);
        }

        int cx = width / 2;
        int cy = height / 2;

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
        Term d = level > 0 ?
                $.secte($.the(c1), $.the(c2), new Termject.IntTerm(area)) :
                $.secte($.the(c1), $.the(c2));

        //Term dir = $.p(d,$.the(area));
        //Term dir = level == 0 ? d : $.p(d,$.the(area));
        //Term dir = level == 0 ? d : $.p(d,$.the(area));
        //Term dir = level == 0 ? d : $.inh(d,$.the(area));
        //Term dir = $.inh(d,$.the(area));

        //Term dir = $.inh(d,$.the(area));
        //Term dir = level == 0 ? d : $.inh(d,$.the(area));

        Term q = quadp(level + 1, x, y, width / 2, height / 2);
        if (q != null) {
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
        } else {
            return d;
            //return $.p(dir);
            //return $.inst($.varDep(0), dir);
        }
    }

    private Compound quadpFlat(int x, int y, int width, int height) {
        int cx = width / 2;
        int cy = height / 2;

        boolean left = x < cx;
        boolean up = y < cy;


        char c1 = (left ? 'l' : 'r');
        char c2 = (up ? 'u' : 'd');
        if (!left)
            x -= cx;
        if (!up)
            y -= cy;

        Atom dir = $.the(c1 + "" + c2);

        if (width > 1 || height > 1) {
            Compound q = quadpFlat(x, y, width / 2, height / 2);
            if (q != null)
                return $.p(Terms.concat(new Term[]{dir}, q.terms()));
            else
                return $.p(dir);
        } else {
            return null; //dir; //$.p(dir);
        }
    }

    public static int log2(int width) {
        return (int) Math.ceil(Math.log(width) / Math.log(2));
    }

    public Term binaryp(int x, int depth) {
        String s = Integer.toBinaryString(x);
        int i = s.length() - 1;
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

