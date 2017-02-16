package nars.experiment.recog2d;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.learn.MLP;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.concept.Concept;
import nars.gui.BeliefTableChart;
import nars.remote.NAgents;
import nars.time.Tense;
import nars.truth.Truth;
import nars.video.PixelBag;
import nars.video.Sensor2D;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.render.Draw;
import spacegraph.space.layout.Grid;
import spacegraph.space.widget.Plot2D;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static jcog.Texts.n2;
import static spacegraph.space.layout.Grid.col;
import static spacegraph.space.layout.Grid.row;

/**
 * Created by me on 10/8/16.
 */
public class Recog2D extends NAgents {

    private final Graphics2D g;
    private final int h;
    private final int w;
    private final Outputs outs;
    private final Training train;
    BufferedImage canvas;

    public final AtomicBoolean neural = new AtomicBoolean(true);




    int image;
    final int maxImages = 6;
    int imagePeriod = 64;

//    float theta;
//    float dTheta = 0.25f;

    static {
        Param.DEBUG = false;
    }

    public Recog2D(NAR n) {
        super("x", n, 8);

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
//                                new MutableTask($.seq(x.term(), 1, happy.term()), '?', null).time(now, now),
//                                new MutableTask($.impl($.inh($.varQuery("wat"), $.the("cam")), 0, happy.term()), '?', null) {
//                                    @Override
//                                    public boolean onAnswered(Task answer) {
//                                        System.err.println(this + "\n\t" + answer);
//                                        return false;
//                                    }
//                                }.time(now, now)
//                        )
//                predictors.add(new MutableTask(x, Symbols.QUESTION, null).present(nar.time()))


        //retina
        Sensor2D<PixelBag> sp = senseCameraRetina("x", () -> canvas, w, h, v -> $.t(v, alpha));


        //still
        //addCamera("x", new Scale(() -> canvas, w, h), v -> $.t(v, alpha));

        //nar.log();



        outs = new Outputs(ii -> $.func("x", $.the("s" + ii)), maxImages, this);
        train = new Training(
                //sensors,
                Lists.newArrayList(Iterables.concat(sensors,sp.src.actions)),
                outs, nar);
        //epsilonProbability = 0; //disable curiosity

        new Thread(() -> {
            SpaceGraph.window(conceptTraining(outs, nar), 800, 600);
        }).start();

    }

    public Surface conceptTraining(Outputs tv, NAR nar) {

        LinkedHashMap<Concept, Outputs.Neuron> out = tv.out;

        Plot2D p;

        int history = 1024;

        Grid g = col(

                row(BeliefTableChart.beliefTableCharts(nar, out.keySet(), 1024)),

                row(p = new Plot2D(history, Plot2D.Line).add("Error", () -> tv.errorSum())),
                //row(s = new Plot2D(history, Plot2D.BarWave).add("Rward", () -> rewardValue)),

                row(out.entrySet().stream().map(ccnn -> new Surface() {
                    @Override
                    protected void paint(GL2 gl) {
                        Concept c = ccnn.getKey();
                        Outputs.Neuron nn = ccnn.getValue();

                        super.paint(gl);

                        float freq, conf;

                        long now = nar.time();
                        float dur = nar.time.dur();

                        Truth t = c.belief(now, dur);
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


                        Draw.colorPolarized(gl,
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
                outs.train();
            }
            //} else {
              //  outs.expect(-1);
              //  outs.verify();
            //}

            if (neural.get()) {
                train.update(outs.train, true);
            }

            p.update();
            //s.update();
        });

        return g;
    }


    @Override
    protected float act() {

        float r;


     //   if (outs.verify) {
            r = 0.5f - (float) outs.errorSum()
                    / outs.states;
        //        } else {
//            //r = 1f; //general positive reinforcement during training
//            r = Float.NaN; //no feedback during training
//        }



        return r;
    }


    protected int nextImage() {

        image = nar.random.nextInt(maxImages);


        return image;
    }

    private void redraw() {
        g.clearRect(0, 0, w, h);
        FontMetrics fontMetrics = g.getFontMetrics();

        String s = String.valueOf((char) ('0' + image));
        //LineMetrics lineMetrics = fontMetrics.getLineMetrics(s, g);
        Rectangle2D sb = fontMetrics.getStringBounds(s, g);

        //System.out.println(s + " : " + lineMetrics.getHeight() + " pixel height");

        //g.rotate(nar.random.nextFloat() * dTheta, w/2, h/2);

        g.drawString(s, Math.round(w / 2f - sb.getCenterX()), Math.round(h / 2f - sb.getCenterY()));
    }

    public static void main(String[] arg) {
        NAgents.run(Recog2D::new, 50000);
    }

    public static class Training {
        private final List<Concept> ins;
        private final Outputs outs;
        private final MLP trainer;
        private final NAR nar;

        private final float learningRate = 0.3f;

        /** Introduction of the momentum rate allows the attenuation of oscillations in the gradient descent. The geometric idea behind this idea can probably best be understood in terms of an eigenspace analysis in the linear case. If the ratio between lowest and largest eigenvalue is large then performing a gradient descent is slow even if the learning rate large due to the conditioning of the matrix. The momentum introduces some balancing in the update between the eigenvectors associated to lower and larger eigenvalues.

         For more detail I refer to

         http://page.mi.fu-berlin.de/rojas/neural/chapter/K8.pdf */
        private final float momentum = 0.6f;

        public Training(java.util.List<Concept> ins, Outputs outs, NAR nar) {

            this.nar = nar;
            this.ins = ins;
            this.outs = outs;


            this.trainer = new MLP(ins.size(), new int[]{ (ins.size()+outs.states)/2,  outs.states }, nar.random);
            trainer.layers[1].setIsSigmoid(false);

        }


        float[] in(float[] i, long when) {
            int s = ins.size();

            if (i == null || i.length!= s)
                i = new float[s];
            for (int j = 0, insSize = ins.size(); j < insSize; j++) {
                i[j] = ins.get(j).beliefFreq(when, 0.5f);
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

            if (apply && errSum < 0.25f) {
                float[] o = trainer.get(i);
                for (int j = 0, oLength = o.length; j < oLength; j++) {
                    float y = o[j];
                    //nar.goal(
                    nar.believe(
                            outs.outVector[j], Tense.Present, y, nar.confidenceDefault('.') * (1f - errSum));

                }
                //System.out.println(Arrays.toString(o));
            }
        }
    }


}
