package nars.experiment.minicraft;

import nars.*;
import nars.experiment.minicraft.side.SideScrollMinicraft;
import nars.experiment.minicraft.side.awtgraphics.AwtGraphicsHandler;
import nars.remote.SwingAgent;
import nars.task.MutableTask;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.time.Tense;
import nars.util.Util;
import nars.util.signal.Autoencoder;
import nars.video.MatrixSensor;
import nars.video.PixelBag;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;
import org.jetbrains.annotations.NotNull;
import spacegraph.SpaceGraph;
import spacegraph.obj.MatrixView;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import static nars.nal.UtilityFunctions.w2c;
import static nars.util.Texts.n2;
import static nars.util.Texts.n4;
import static spacegraph.obj.GridSurface.col;

/**
 * Created by me on 9/19/16.
 */
public class SideCraft extends SwingAgent {

    private final SideScrollMinicraft craft;
    private final MatrixSensor pixels;
    private PixelAutoClassifier camAE;

    public static void main(String[] args) {
        run(SideCraft::new, 6512);
    }

    public SideCraft(NAR nar) {
        super(nar, 0);

        this.craft = new SideScrollMinicraft();


        //swingCam.input(W/4, H/4, W/2, H/2); //50%

//        Scale cam = new Scale(swingCam, 48, 48);
//        SwingCamera swing = new SwingCamera(((AwtGraphicsHandler) craft.gfx).buffer);
//        nar.onFrame(nn -> {
//            swing.update();
//        });

        BufferedImage camBuffer = ((AwtGraphicsHandler) craft.gfx).buffer;

        PixelBag cam = new PixelBag(camBuffer, 64, 64).addActions("cra", this);

        final int nx = 16;
        camAE = new SideCraft.PixelAutoClassifier("cra", cam.pixels, nx, nx, (subX, subY) -> {
            //context metadata: camera zoom, to give a sense of scale
            return new float[]{subX / ((float) (nx - 1)), subY / ((float) (nx - 1)), cam.Z};
        }, 16, 4, this);
        SpaceGraph.window(
                col(
                        new MatrixView(camAE.W.length, camAE.W[0].length, arrayRenderer(camAE.W)),
                        new MatrixView(camAE.pixRecon.length, camAE.pixRecon[0].length, arrayRenderer(camAE.pixRecon))
                ),
                500, 500
        );

        pixels = addCamera("cra", cam, (v) -> $.t(v, alpha));


//        new NObj("cra", craft, nar)
//                .read(
//                    "player.health",
//                    "player.dir",
//                    "player.getTile().connectsToGrass",
//                    "player.getTile().connectsToWater"
//                ).into(this);

//        InputHandler input = craft.input;
        actionToggle("cra:(key,left)", (b) -> {
            if (b) craft.player.startLeft(false /* slow */);
            else craft.player.stopLeft();
        });
        actionToggle("cra:(key,right)", (b) -> {
            if (b) craft.player.startRight(false /* slow */);
            else craft.player.stopRight();
        });
        actionToggle("cra:(key,up)", (b) -> {
            if (b) craft.player.startClimb();
            else craft.player.stopClimb();
        });
        actionToggle("cra:(key,mouseL)", (b) -> craft.leftClick = b);
        actionToggle("cra:(key,mouseR)", (b) -> craft.rightClick = b);
        float mSpeed = 25f;
        actionBipolar("cra:(mouse,X)", (v) -> {
            int x = craft.screenMousePos.x;
            int xx = Util.clamp(x + v * mSpeed, 0, camBuffer.getWidth() - 1);
            if (xx != x) {
                craft.screenMousePos.x = xx;
                return true;
            }
            return false;
        });
        actionBipolar("cra:(mouse,Y)", (v) -> {
            int y = craft.screenMousePos.y;
            int yy = Util.clamp(y + v * mSpeed, 0, camBuffer.getHeight() - 1);
            if (yy != y) {
                craft.screenMousePos.y = yy;
                return true;
            }
            return false;
        });


//        addToggleAction("cra:up", (b) -> input.up.toggle(b) );
//        addToggleAction("cra:down", (b) -> input.down.toggle(b) );
//        addToggleAction("cra:left", (b) -> input.left.toggle(b) );
//        addToggleAction("cra:right", (b) -> input.right.toggle(b) );

        craft.startGame(false, 512);
    }


    float prevScore = 0;

    @Override
    protected float reward() {

        if (camAE != null)
            camAE.frame();

        float nextScore = craft.frame();
        float ds = nextScore - prevScore;
        this.prevScore = nextScore;
        return ds;
    }

    public static class PixelAutoClassifier extends Autoencoder {

        private final NAR nar;
        private final MetaBits metabits;

        @NotNull String TAG = ("ae");

        private final float[][] pixIn;

        private final short[][][] pixOut;
        private final float[][] pixConf;

        public final float[][] pixRecon; //reconstructed input
        private final Term root;
        private final float in[];
        private final int sw, sh;
        private final int nw, nh;
        private final int pw, ph;
        private final int batchSize;
        private boolean reconstruct = true;
        public boolean learn = true;
        private float minConf;
        private float baseConf;

//        public PixelAutoClassifier(String root, float[][] pixIn, int sw, int sh, NAgent agent) {
//            this(root, pixIn, sw, sh, sw * sh /* estimate */, 4, agent);
//        }

        public interface MetaBits {
            float[] get(int subX, int subY);
        }

        /**
         * metabits must consistently return an array of the same size, since now the size of this autoencoder is locked to its dimension
         */
        public PixelAutoClassifier(String root, float[][] pixIn, int sw, int sh, MetaBits metabits, int states, int termBatchSize, NAgent agent) {
            super(sw * sh + metabits.get(0, 0).length, states, agent.nar.random);
            this.metabits = metabits;
            this.nar = agent.nar;
            this.root = $.the(root);
            this.pixIn = pixIn;
            this.sw = sw; //stride width
            this.sh = sh; //stride height
            ph = pixIn[0].length;
            pw = pixIn.length;
            this.nw = (int) Math.ceil(pw / ((float) sw)); //number strides wide
            this.nh = (int) Math.ceil(ph / ((float) sh)); //number strides high
            this.in = new float[xx.length];
            this.pixRecon = new float[pw][ph];

            this.pixOut = new short[nw][nh][];
            this.pixConf = new float[nw][nh];

            this.batchSize = termBatchSize;

            assert (nw * nh % batchSize == 0); //evenly divides
        }

        public void frame() {
            //int q = 0;

            minConf = nar.confMin.floatValue();
            baseConf = nar.confidenceDefault(Symbols.BELIEF);

            float alpha = 0.85f; //this represents the overall rate; the sub-block rate will be a fraction of this
            float subAlpha = alpha / (nw * nh);
            float corruption = 0.1f;
            int regionPixels = sw * sh;
            float sumErr = 0;

            int states = y.length;
            float outputThresh = 1f - (1f/(states-1));
            int maxStatesPerRegion = states/4; //limit before considered ambiguous

            //forget(alpha*alpha);

            List<Task> tasks = $.newArrayList();

            for (int i = 0; i < nw; ) {
                for (int j = 0; j < nh; ) {

                    int p = 0;
                    int oi = i * sw;
                    int oj = j * sh;
                    for (int si = 0; si < sw; si++) {
                        int d = si + oi;
                        if (d >= pw)
                            break;

                        float[] col = pixIn[d];
                        for (int sj = 0; sj < sh; sj++) {

                            int c = sj + oj;

                            in[p++] = c < ph ? col[c] : 0;

                        }
                    }

                    float[] metabits = this.metabits.get(i, j);
                    for (float m : metabits) {
                        in[p++] = m;
                    }

                    boolean sigIn = true, sigOut = true;
                    short[] po = null;
                    if (learn) {
                        float regionError = train(in, subAlpha, 0f, corruption, sigIn, sigOut);
                        sumErr += regionError;

                        // must have sufficient error, and
                        // the indecision degree between selected features must not reduce the evidence
                        // below that which would cross the provided min conf thresh
                        float evi;
                        if ((evi = 1f - (regionError/regionPixels)) > 0) {
                            short[] features = max(outputThresh);
                            if (features!=null) {
                                if (features.length < maxStatesPerRegion) {
                                    evi /= features.length;
                                    if ((pixConf[i][j] = (baseConf * w2c(evi))) >= minConf) {
                                        po = features;
                                    }
                                }
                            }
                        }
                        //System.out.println(n2(y) + ", +-" + n4(regionError / y.length));
                    } else {
                        //System.out.println(n2(y));
                        recode(in, sigIn, sigOut);
                    }
                    pixOut[i][j] = po;


                    if (reconstruct) {

                        float mult = (pixOut[i][j]==null) ?  -1f  /* invert */ : +1f /* normal */;

                        float z[] = this.z;
                        p = 0;
                        for (int si = 0; si < sw; si++) {
                            int d = si + oi;
                            if (d >= pw)
                                break;

                            float[] col = pixRecon[d];
                            for (int sj = 0; sj < sh; sj++) {

                                int c = sj + oj;

                                if (c >= ph)
                                    break;

                                col[c] = z[p++] * mult;
                            }
                        }
                    }

                    j++;
                }

                i++;
            }

            if (learn) {
                //float meanErrPerPixel = sumErr / (pw * ph);
                //System.out.println(Arrays.toString(pixOut) + ", +-" + n4(meanErrPerPixel));


                for (int i = 0; i < pixOut.length; i++) {
                    short[][] po = pixOut[i];
                    float[] pc = pixConf[i];
                    for (int j = 0; j < po.length; j++) {
                        short[] v = po[j];
                        if (v == null)
                            continue;

                        float conf = pc[j];

                        Term[] vt = new Term[v.length];
                        int vtt = 0;
                        for (short x : v) {
                            vt[vtt++] = $.the(TAG.toString() + x);
                        }

                        Term Y = $.inh($.sete($.p(root, $.the(i), $.the(j))), $.seti(vt));
                        tasks.add(new MutableTask(Y, Symbols.BELIEF, 1f, conf)
                                    .present(nar)
                                    .log(TAG));

                    }
                }


                if (!tasks.isEmpty()) {
                    //TODO normalize priority of these
                    nar.inputLater(tasks);
                }

            }
        }



//        /**
//         * Autoencodes a vector of inputs and attempts to classify the current values to
//         * an item. these are input representing summary beliefs. the semantics of the
//         * autoencoding can also be input at some interval, since this can change, the
//         * assocaitions will need some continous remapping in proportion.
//         * these can be done through tensed similarity beliefs.
//         */
//        public class AutoClassifier extends Autoencoder  {
//
//            private static final Logger logger = LoggerFactory.getLogger(AutoClassifier.class);
//
//
//
//
//            //private final Compound aeBase;
//
//            //private int metaInterval = 100;
//
//            public AutoClassifier(int input, int output, Random rng) {
//                super(input, output, rng);
//            }
//
//            //    protected void input(int stride, Term which, float conf) {
//
//        GeneratedTask t = new GeneratedTask(
//                input(stride, which),
//
//
//                '.', $.t(1f, conf));
//        t.time(nar.time(), nar.time()).budget(nar.priorityDefault(Symbols.BELIEF), nar.durabilityDefault(Symbols.BELIEF));
//        nar.inputLater( t );
//
//
//    }
//
//    @NotNull
//    private static Term state(int which) {
//        //even though the state can be identified by an integer,
//        //it does not have the same meaning as integers used
//        //elsewhere. however once the autoencoder stabilizes
//        //these can be relied on as semantically secure in their context
//        //return $.p(aeBase, new Termject.IntTerm(which));
//        return $.the("X" + which);
//    }
//
//    @NotNull
//    private Compound input(int stride, Term state) {
//        Compound c = $.prop(stride(stride), state);
//        if (c == null)  {
//            $.prop(stride(stride), state);
//            throw new NullPointerException();
//        }
//        return c;
//        //return $.image(2, false, base, new Termject.IntTerm(stride), state);
//    }
//
//    private Term stride(int stride) {
//        return $.p(base, new IntTerm(stride));
//    }
//
//    /** input the 'metadata' of the autoencoder that connects the virtual concepts to their semantic inputs */
//    protected void meta() {
//        int k = 0;
//        int n = input.size();
//        //final Term unknown = $.varDep(2);
//        for (int i = 0; i < strides; i++) {
//            List<? extends SensorConcept> l = input.subList(k, Math.min(n, k + stride));
//            //TODO re-use the same eternal belief to reactivate itself
//            Compound x = $.inh(
//                    $.sete(
//                        l.stream().map(CompoundConcept::term).toArray(Term[]::new)
//                    ),
//                    //input(i, unknown).term(0) //the image internal
//                    stride(i)
//            );
//            nar.believe(x);
//            k+= stride;
//        }
//    }
//        }


    }

    public static MatrixView.ViewFunc arrayRenderer(float[][] ww) {
        return (x, y, gl) -> {
            float v = ww[x][y];
            float r, g, b;
            if (v < 0) {
                r = -v / 2f;
                g = 0f;
                b = -v;
            } else {
                r = v;
                g = v / 2;
                b = 0f;
            }
            gl.glColor3f(r, g, b);
            return 0;
        };
    }

}
