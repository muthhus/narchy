package nars.experiment.recog2d;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.jogamp.opengl.GL2;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.concept.Concept;
import nars.gui.BeliefTableChart;
import nars.remote.NAgents;
import nars.time.Tense;
import nars.truth.Truth;
import nars.util.Util;
import nars.video.PixelBag;
import nars.video.Scale;
import nars.video.Sensor2D;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.obj.GridSurface;
import spacegraph.obj.Plot2D;
import spacegraph.render.Draw;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

import static nars.util.Texts.n2;
import static spacegraph.obj.GridSurface.col;
import static spacegraph.obj.GridSurface.row;

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


    int a = 0;

    int image = 0;
    final int maxImages = 9;
    int imagePeriod = 128;
    int TRAINING_PERIOD = imagePeriod * 3;

//    float theta;
//    float dTheta = 0.25f;

    static {
        Param.DEBUG = false;
    }

    public Recog2D(NAR n) {
        super(n, 1);

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
        Sensor2D<PixelBag> sp = addCamera("x", () -> canvas, w, h, v -> $.t(v, alpha));


        //still
        //addCamera("x", new Scale(() -> canvas, w, h), v -> $.t(v, alpha));

        //nar.log();



        outs = new Outputs(ii -> $.func("x", $.the("s" + ii)), maxImages, this);
        train = new Training(
                Lists.newArrayList(Iterables.concat(sensors,sp.src.actions)),
                outs, nar);


        new Thread(() -> {
            SpaceGraph.window(conceptTraining(outs, nar), 800, 600);
        }).start();

    }

    public Surface conceptTraining(Outputs tv, NAR nar) {

        LinkedHashMap<Concept, Outputs.Neuron> out = tv.out;

        Plot2D p, s;

        int history = 1024;

        GridSurface g = col(

                row(BeliefTableChart.beliefTableCharts(nar, out.keySet(), 1024)),

                row(p = new Plot2D(history, Plot2D.Line).add("Error", () -> tv.errorSum())),
                row(s = new Plot2D(history, Plot2D.BarWave).add("Rward", () -> rewardValue)),

                row(out.entrySet().stream().map(ccnn -> new Surface() {
                    @Override
                    protected void paint(GL2 gl) {
                        Concept c = ccnn.getKey();
                        Outputs.Neuron nn = ccnn.getValue();

                        super.paint(gl);

                        float freq, conf;

                        long now = nar.time();
                        Truth t = c.belief(now);
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
                                2f * (freq - 0.5f) * conf  //unipolar (1 color)
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

        nar.onFrame(() -> {
            p.update();
            s.update();
        });

        return g;
    }


    @Override
    protected float act() {
        a++;

        float r;

        train.update(outs.verify);

        if (outs.verify) {
            r = 1f - (float) outs.errorSum();// / imgTrainer.states;
        } else {
            //r = 1f; //general positive reinforcement during training
            r = Float.NaN;
        }

        if (a % imagePeriod == 0) {
            nextImage();
        }

        redraw();


        if (a % TRAINING_PERIOD == TRAINING_PERIOD - 1) {

            //toggle
            if (outs.verify) {
                outs.train();
            } else {
                outs.verify();
                image = -1;
            }

        }

        return r;
    }


    protected int nextImage() {

        image = nar.random.nextInt(maxImages);
        outs.expect(image);

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
        NAgents.run(Recog2D::new, 5500);
    }

    public static class Training {
        private final List<? extends Concept> ins;
        private final Outputs outs;
        private final MLP trainer;
        private final NAR nar;

        private float learningRate = 0.1f;
        private float momentum = 0.6f;

        public Training(java.util.List<? extends Concept> ins, Outputs outs, NAR nar) {

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
                i[j] = ins.get(j).beliefFreq(when, 0);
            }

            return i;
        }

        protected void update(boolean train) {
            float[] i = in(null, nar.time());

            if (train) {
                float[] err = trainer.put(i, outs.expected(null), learningRate, momentum);

                System.err.println("error sum=" + Util.sum(err));
            } else {
                float[] o = trainer.get(i);
                for (int j = 0, oLength = o.length; j < oLength; j++) {
                    float y = o[j];
                    nar.goal(outs.outVector[j], Tense.Present, y);
                }
                //System.out.println(Arrays.toString(o));
            }
        }
    }


    /**
     * http://stackoverflow.com/a/12653770
     */
    public static class MLP {

        public static class MLPLayer {

            float[] output;
            float[] input;
            float[] weights;
            float[] dweights;
            boolean isSigmoid = true;

            public MLPLayer(int inputSize, int outputSize, Random r) {
                output = new float[outputSize];
                input = new float[inputSize + 1];
                weights = new float[(1 + inputSize) * outputSize];
                dweights = new float[weights.length];
                initWeights(r);
            }

            public void setIsSigmoid(boolean isSigmoid) {
                this.isSigmoid = isSigmoid;
            }

            public void initWeights(Random r) {
                for (int i = 0; i < weights.length; i++) {
                    weights[i] = (r.nextFloat() - 0.5f) * 4f;
                }
            }

            public float[] run(float[] in) {
                System.arraycopy(in, 0, input, 0, in.length);
                input[input.length - 1] = 1;
                int offs = 0;
                Arrays.fill(output, 0);
                for (int i = 0; i < output.length; i++) {
                    float o = 0;
                    for (int j = 0; j < input.length; j++) {
                        o += weights[offs + j] * input[j];
                    }
                    output[i] += o;
                    if (isSigmoid) {
                        output[i] = (float) (1 / (1 + Math.exp(-output[i])));
                    }
                    offs += input.length;
                }
                return Arrays.copyOf(output, output.length);
            }

            public float[] train(float[] error, float learningRate, float momentum) {
                int offs = 0;
                float[] nextError = new float[input.length];
                for (int i = 0; i < output.length; i++) {
                    float d = error[i];
                    if (isSigmoid) {
                        d *= output[i] * (1 - output[i]);
                    }
                    for (int j = 0; j < input.length; j++) {
                        int idx = offs + j;
                        nextError[j] += weights[idx] * d;
                        float dw = input[j] * d * learningRate;
                        weights[idx] += dweights[idx] * momentum + dw;
                        dweights[idx] = dw;
                    }
                    offs += input.length;
                }
                return nextError;
            }
        }

        final MLPLayer[] layers;

        public MLP(int inputSize, int[] layersSize, Random r) {
            layers = new MLPLayer[layersSize.length];
            for (int i = 0; i < layersSize.length; i++) {
                int inSize = i == 0 ? inputSize : layersSize[i - 1];
                layers[i] = new MLPLayer(inSize, layersSize[i], r);
            }
        }

        public float[] get(float[] input) {
            float[] actIn = input;
            for (int i = 0; i < layers.length; i++) {
                actIn = layers[i].run(actIn);
            }
            return actIn;
        }

        public float[] put(float[] input, float[] targetOutput, float learningRate, float momentum) {
            float[] calcOut = get(input);
            float[] error = new float[calcOut.length];
            for (int i = 0; i < error.length; i++) {
                error[i] = targetOutput[i] - calcOut[i]; // negative error
            }
            for (int i = layers.length - 1; i >= 0; i--) {
                error = layers[i].train(error, learningRate, momentum);
            }
            return error;
        }

        public static void main(String[] args) throws Exception {

            float[][] train = new float[][]{new float[]{0, 0}, new float[]{0, 1}, new float[]{1, 0}, new float[]{1, 1}};

            float[][] res = new float[][]{new float[]{0}, new float[]{1}, new float[]{1}, new float[]{0}};

            MLP mlp = new MLP(2, new int[]{2, 1}, new Random());
            mlp.layers[1].setIsSigmoid(false);
            Random r = new Random();
            int en = 500;
            for (int e = 0; e < en; e++) {

                for (int i = 0; i < res.length; i++) {
                    int idx = r.nextInt(res.length);
                    mlp.put(train[idx], res[idx], 0.3f, 0.6f);
                }

                if ((e + 1) % 100 == 0) {
                    System.out.println();
                    for (int i = 0; i < res.length; i++) {
                        float[] t = train[i];
                        System.out.printf("%d epoch\n", e + 1);
                        System.out.printf("%.1f, %.1f --> %.5f\n", t[0], t[1], mlp.get(t)[0]);
                    }
                }
            }
        }
    }
}
