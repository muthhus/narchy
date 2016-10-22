package nars.experiment.recog2d;

import com.jogamp.opengl.GL2;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.SensorConcept;
import nars.gui.BeliefTableChart;
import nars.remote.NAgents;
import nars.task.MutableTask;
import nars.truth.Truth;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.obj.GridSurface;
import spacegraph.obj.Plot2D;
import spacegraph.render.Draw;

import java.awt.*;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;

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
    private final TrainVector imgTrainer;
    BufferedImage canvas;


    int a = 0;

    int image = 0;
    final int maxImages = 4;
    int imagePeriod = 100;
    int TRAINING_PERIOD = imagePeriod * 8;

    float theta;
    float dTheta = 0.25f;

    static {
        Param.DEBUG = false;
    }

    public Recog2D(NAR n) {
        super(n, 1);

        w = 24;
        h = 24;
        canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
        g = ((Graphics2D) canvas.getGraphics());

        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        //n.beliefConfidence(0.2f);

        imgTrainer = new TrainVector(ii -> $.p($.the("i"), $.the(ii)), maxImages, this);
        imgTrainer.out.keySet().forEach(x ->
                        predictors.addAll(
                                new MutableTask($.seq(x.term(), 1, happy.term()), '?', null).time(now, now),
                                new MutableTask($.impl($.inh($.varQuery("wat"), $.the("cam")), 0, happy.term()), '?', null) {
                                    @Override
                                    public boolean onAnswered(Task answer) {
                                        System.err.println(this + "\n\t" + answer);
                                        return false;
                                    }
                                }.time(now, now)
                        )
//                predictors.add(new MutableTask(x, Symbols.QUESTION, null).present(nar.time()))
        );

        addCamera("cam", () -> canvas, w, h, v -> $.t(v, alpha));
        //addCamera("x", new Scale(() -> canvas, w, h), v -> $.t(v, alpha));


        new Thread(() -> {
            SpaceGraph.window(conceptTraining(imgTrainer, nar), 800, 600);
        }).start();

    }

    public Surface conceptTraining(TrainVector tv, NAR nar) {

        LinkedHashMap<SensorConcept, TrainVector.Neuron> out = tv.out;

        Plot2D p, s;
        GridSurface g = col(

                row(BeliefTableChart.beliefTableCharts(nar, out.keySet(), 1024)),

                row(p = new Plot2D(8192, Plot2D.Line).add("Error", () -> tv.errorSum())),
                row(s = new Plot2D(8192, Plot2D.BarWave).add("Rward", () -> rewardValue)),

                row(out.entrySet().stream().map(ccnn -> new Surface() {
                    @Override
                    protected void paint(GL2 gl) {
                        Concept c = ccnn.getKey();
                        TrainVector.Neuron nn = ccnn.getValue();

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
                                    0f; //interpret no-belief as false
                            //Float.NaN  //use NaN to force learning of negation as separate from no-belief
                            freq = defaultFreq;
                        }


                        Draw.colorPolarized(gl,
                                freq //unipolar (1 color)
                                //2f * (-0.5f + freq) //bipolar (2 colors)
                        );

                        float m = 0.5f * conf;


                        if (tv.verify) {
                            float error = nn.error;
                            if (error != error) {

                                //training phase

                            } else {

                                //verification

                                //draw backgroudn/border
                                Draw.colorPolarized(gl, -error);
                                Draw.rect(gl, 0, 0, 1f, 1f);

                                gl.glColor3f(error, 1f - error, 0f);
                                float fontSize = 0.08f;
                                Draw.text(gl, c.term().toString(), fontSize, m / 2, 1f - m / 2, 0);
                                Draw.text(gl, "err=" + n2(error), fontSize, m / 2, m / 2, 0);
                            }
                        }


                        //draw center icon
                        Draw.rect(gl, m / 2, m / 2, 1 - m, 1 - m);


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

        if (imgTrainer.verify) {
            r = 0.5f - (float) imgTrainer.errorSum() / imgTrainer.states;
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
            if (imgTrainer.verify)
                imgTrainer.train();
            else {
                imgTrainer.verify();
                image = -1;
            }

        }

        return r;
    }


    protected int nextImage() {

        image = nar.random.nextInt(maxImages);
        imgTrainer.expect(image);

        return image;
    }

    private void redraw() {
        g.clearRect(0, 0, w, h);
        FontMetrics fontMetrics = g.getFontMetrics();

        String s = String.valueOf((char) ('0' + image));
        LineMetrics lineMetrics = fontMetrics.getLineMetrics(s, g);
        //System.out.println(s + " : " + lineMetrics.getHeight() + " pixel height");

        //g.rotate(nar.random.nextFloat() * dTheta, w/2, h/2);

        g.drawString(s, w / 4, lineMetrics.getHeight());
    }

    public static void main(String[] arg) {
        NAgents.runRT(Recog2D::new);
    }
}
