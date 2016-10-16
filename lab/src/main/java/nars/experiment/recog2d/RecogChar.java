package nars.experiment.recog2d;

import com.jogamp.opengl.GL2;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.SensorConcept;
import nars.gui.BeliefTableChart;
import nars.remote.SwingAgent;
import nars.task.MutableTask;
import nars.truth.Truth;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.obj.GridSurface;
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
public class RecogChar extends SwingAgent {

    private final Graphics2D g;
    private final int h;
    private final int w;
    private final TrainVector imgTrainer;
    BufferedImage canvas;


    int a = 0;

    int image = 0;
    final int maxImages = 5;
    int imagePeriod = 16;
    int TRAINING_PERIOD = imagePeriod * 4;

    float theta;
    float dTheta = 0.25f;

    static {
        Param.DEBUG = true;
    }

    public RecogChar(NAR n) {
        super(n, 32);

        w = 24;
        h = 24;
        canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
        g = ((Graphics2D) canvas.getGraphics());

        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


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

        addCamera("cam", ()->canvas, w,h, v -> $.t(v, alpha));
        //addCamera("x", new Scale(() -> canvas, w, h), v -> $.t(v, alpha));


        new Thread(() -> {
            Facial f = new Facial(conceptTraining(imgTrainer, nar));
            new SpaceGraph().add(f.maximize()).show(800, 600);
        }).start();

    }

    public static Surface conceptTraining(TrainVector tv, NAR nar) {

        LinkedHashMap<SensorConcept, TrainVector.Neuron> out = tv.out;

        GridSurface g = col(

                row(BeliefTableChart.beliefTableCharts(nar, out.keySet(), 1024)),

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
                            conf = 0.75f;
                            freq = Float.NaN;
                        }


                        Draw.colorPolarized(gl, 2f * (-0.5f + freq));

                        float m = 0.25f * 1f * conf;
                        Draw.rect(gl, m / 2, m / 2, 1 - m, 1 - m);

                        if (tv.verify) {
                            float error = nn.error;
                            if (error != error) {

                            } else {
                                gl.glColor3f(error, 1f - error, 0f);
                                float fontSize = 0.08f;
                                Draw.text(gl, c.term().toString(), fontSize, m / 2, 1f - m / 2, 0);
                                Draw.text(gl, "err=" + n2(error), fontSize, m / 2, m / 2, 0);
                            }
                        }
                    }
                }).toArray(Surface[]::new)));
        return g;
    }


    @Override
    protected float act() {
        a++;

        float r;

        if (imgTrainer.verify) {
            r = (float) -imgTrainer.errorSum() + 0.5f;
        } else {
            r = 1f; //general positive reinforcement during training
            // Float.NaN;
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

        g.drawString(s, w/4, lineMetrics.getHeight());
    }

    public static void main(String[] arg) {
        SwingAgent.run(RecogChar::new, 100000);
    }
}
