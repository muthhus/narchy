package nars.experiment.recog2d;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.learn.MLPMap;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.Param;
import nars.concept.BaseConcept;
import nars.concept.Concept;
import nars.gui.BeliefTableChart;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.time.Tense;
import nars.truth.Truth;
import nars.video.CameraSensor;
import nars.video.PixelBag;
import nars.video.Scale;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.layout.Grid;
import spacegraph.render.Draw;
import spacegraph.widget.meter.Plot2D;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.stream.Collectors.toList;
import static jcog.Texts.n2;
import static nars.Op.BELIEF;
import static spacegraph.layout.Grid.col;
import static spacegraph.layout.Grid.row;

/**
 * Created by me on 10/8/16.
 */
public class Recog2D extends NAgentX {


    private final Graphics2D g;
    private final int h;
    private final int w;
    private final Outputs outs;

    private final Training train;

    boolean mlpLearn = true, mlpSupport;

    BufferedImage canvas;

    public final AtomicBoolean neural = new AtomicBoolean(false);


    int image;
    final int maxImages = 4;

    int imagePeriod = 100;
    FloatToFloatFunction goalInfluence = (x) -> x > 0.5f ? 1 : 0; //1f/(maxImages); //how much goal feedback will influence beliefs, <=1

//    float theta;
//    float dTheta = 0.25f;

    static {
        Param.DEBUG = false;
    }

    public Recog2D(NAR n) {
        super("x", n);


        w = 10;
        h = 12;
        canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
        g = ((Graphics2D) canvas.getGraphics());

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        //n.beliefConfidence(0.2f);

//        imgTrainer.out.keySet().forEach(x ->
//                        predictors.addAll(
//                                new TaskBuilder($.seq(x.term(), 1, happy.term()), '?', null).time(now, now),
//                                new TaskBuilder($.impl($.inh($.varQuery("wat"), $.the("cam")), 0, happy.term()), '?', null) {
//                                    @Override
//                                    public boolean onAnswered(Task answer) {
//                                        System.err.println(this + "\n\t" + answer);
//                                        return false;
//                                    }
//                                }.time(now, now)
//                        )
//                predictors.add(new TaskBuilder(x, Symbols.QUESTION, null).present(nar.time()))


        //retina
//        Sensor2D spR = senseCameraRetina($.p(id, $.the("full")).toString(),
//                () -> canvas, w, h, v -> $.t(v, nar.confDefault(BELIEF)));

        //still
        CameraSensor sp = senseCamera(
                id
                //$.p(id,
                //$.the("zoom")
                //)
                ,
                /*new Blink*/(new Scale(() -> canvas, w, h)/*, 0.8f*/));

        //nar.log();

        outs = new Outputs(ii -> $.inh(Atomic.the("s" + ii), id), maxImages, this, goalInfluence);
        train = new Training(
                //sensors,
                Lists.newArrayList(
                        sp.src instanceof PixelBag ? Iterables.concat(sensors.keySet(), ((PixelBag) sp.src).actions ) : sensors.keySet()
                ),
                outs, nar);

        //new Thread(() -> {
        SpaceGraph.window(conceptTraining(outs, nar), 800, 600);
        //}).start();

    }

    public Surface conceptTraining(Outputs tv, NAR nar) {

        LinkedHashMap<BaseConcept, Outputs.Neuron> out = tv.out;

        Plot2D p;

        int history = 1024;

        Grid g = col(

                row(beliefTableCharts(nar, out.keySet(), 1024)),

                row(p = new Plot2D(history, Plot2D.Line).add("Reward", () ->
                                reward
                        //tv.errorSum()
                )),
                //row(s = new Plot2D(history, Plot2D.BarWave).add("Rward", () -> rewardValue)),

                row(out.entrySet().stream().map(ccnn -> new Surface() {
                    @Override
                    protected void paint(GL2 gl) {
                        Concept c = ccnn.getKey();
                        Outputs.Neuron nn = ccnn.getValue();

                        super.paint(gl);

                        float freq, conf;

                        Truth t = nar.beliefTruth(c, nar.time());
                        if (t != null) {
                            conf = t.conf();
                            freq = t.freq();
                        } else {
                            conf = nar.confMin.floatValue();
                            float defaultFreq =
                                    0.5f; //interpret no-belief as maybe
                            //Float.NaN  //use NaN to force learning of negation as separate from no-belief
                            freq = defaultFreq;
                        }


                        Draw.colorBipolar(gl,
                                2f * (freq - 0.5f)
                                //2f * (freq - 0.5f) * conf  //unipolar (1 color)
                                //2f * (-0.5f + freq) //bipolar (2 colors)
                        );

                        float m = 0.5f * conf;

                        Draw.rect(gl, 0, 0, 1f, 1f);

                        if (tv.verify) {
                            float error = nn.error;
                            if (error != error) {

                                //training phase
                                //Draw.rect(gl, m / 2, m / 2, 1 - m, 1 - m);
                            } else {

                                //verification

                                //draw backgroudn/border
                                //gl.glColor3f(error, 1f - error, 0f);

                                float fontSize = 0.08f;
                                gl.glColor3f(1f, 1f, 1f);
                                Draw.text(gl, c.term().toString(), fontSize, m / 2, 1f - m / 2, 0);
                                Draw.text(gl, "err=" + n2(error), fontSize, m / 2, m / 2, 0);
                            }
                        }


                    }
                }).toArray(Surface[]::new)));

        nar.onCycle(() -> {

            if (nar.time() % imagePeriod == 0) {
                nextImage();
            }

            redraw();

            if (neural.get()) {
                //if (nar.time() < trainFrames) {
                outs.expect(image);
            }
            //} else {
            //  outs.expect(-1);
            //  outs.verify();
            //}


            if (neural.get()) {
                train.update(mlpLearn, mlpSupport);
            }

            p.update();
            //s.update();
        });

        return g;
    }

    @Deprecated
    public static List<Surface> beliefTableCharts(NAR nar, Collection<? extends Termed> terms, long window) {
        long[] btRange = new long[2];
        nar.onCycle(nn -> {
            long now = nn.time();
            btRange[0] = now - window;
            btRange[1] = now + window;
        });
        return terms.stream().map(c -> new BeliefTableChart(nar, c, btRange)).collect(toList());
    }


    @Override
    protected float act() {

        float error = 0;

        for (int i = 0; i < maxImages; i++) {


            long when = nar.time();
            Truth g = nar.beliefTruth(outs.outVector[i], when);

            if (g == null) {
                error += 0.5;
            } else {
                error += Math.abs(g.freq() - ((image == i) ? 1f : 0f)); //smooth
                //error += ((image == i) ? g.freq() > 0.5f : g.freq() < 0.5f) ? 1f : 0f; //discrete
            }
        }

        float sharp = 1;
        return (float) (1f - 2 * Math.pow((error / maxImages), sharp));

//            r = 0.5f - (float) outs.errorSum()
//                    / outs.states;
        //return r;


    }


    protected int nextImage() {

        image = nar.random().nextInt(maxImages);


        return image;
    }

    private void redraw() {
        g.clearRect(0, 0, w, h);
        FontMetrics fontMetrics = g.getFontMetrics();

        String s = String.valueOf((char) ('0' + image));
        //LineMetrics lineMetrics = fontMetrics.getLineMetrics(s, g);
        Rectangle2D sb = fontMetrics.getStringBounds(s, g);

        //System.out.println(s + " : " + sb);

        //g.rotate(nar.random.nextFloat() * dTheta, w/2, h/2);

        g.drawString(s, Math.round(w / 2f - sb.getCenterX()), Math.round(h / 2f - sb.getCenterY()));
    }

    public static void main(String[] arg) {

        NAgentX.runRT((n) -> {

            Recog2D a = new Recog2D(n);
            return a;

        }, 15);
    }

    public static class Training {
        private final List<Concept> ins;
        private final Outputs outs;
        private final MLPMap trainer;
        private final NAR nar;

        private final float learningRate = 0.3f;

        /**
         * Introduction of the momentum rate allows the attenuation of oscillations in the gradient descent. The geometric idea behind this idea can probably best be understood in terms of an eigenspace analysis in the linear case. If the ratio between lowest and largest eigenvalue is large then performing a gradient descent is slow even if the learning rate large due to the conditioning of the matrix. The momentum introduces some balancing in the update between the eigenvectors associated to lower and larger eigenvalues.
         * <p>
         * For more detail I refer to
         * <p>
         * http://page.mi.fu-berlin.de/rojas/neural/chapter/K8.pdf
         */
        private final float momentum = 0.6f;

        public Training(java.util.List<Concept> ins, Outputs outs, NAR nar) {

            this.nar = nar;
            this.ins = ins;
            this.outs = outs;


            this.trainer = new MLPMap(ins.size(), new int[]{(ins.size() + outs.states) / 2, outs.states}, nar.random());
            trainer.layers[1].setIsSigmoid(false);

        }


        float[] in(float[] i, long when) {
            int s = ins.size();

            if (i == null || i.length != s)
                i = new float[s];
            for (int j = 0, insSize = ins.size(); j < insSize; j++) {
                float b = nar.beliefTruth(ins.get(j), when).freq();
                if (b != b) //dont input NaN
                    b = 0.5f;
                i[j] = b;
            }

            return i;
        }

        protected void update(boolean train, boolean apply) {
            float[] i = in(null, nar.time());

            float errSum;
            if (train) {
                float[] err = trainer.put(i, outs.expected(null), learningRate, momentum);
                //System.err.println("error=" + Texts.n2(err));
                errSum = Util.sumAbs(err) / err.length;
                System.err.println("  error sum=" + errSum);
            } else {
                errSum = 0f;
            }

            if (apply/* && errSum < 0.25f*/) {
                float[] o = trainer.get(i);
                for (int j = 0, oLength = o.length; j < oLength; j++) {
                    float y = o[j];
                    //nar.goal(
                    float c = nar.confDefault(BELIEF) * (1f - errSum);
                    if (c > 0) {
                        nar.believe(
                                outs.outVector[j].term(),
                                Tense.Present, y, c);
                    }

                }
                //System.out.println(Arrays.toString(o));
            }
        }
    }


}
