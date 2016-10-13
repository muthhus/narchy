package nars.experiment.recog2d;

import nars.$;
import nars.NAR;
import nars.Symbols;
import nars.concept.SensorConcept;
import nars.gui.Vis;
import nars.guifx.Spacegraph;
import nars.remote.SwingAgent;
import nars.task.MutableTask;
import nars.term.Termed;
import nars.time.Tense;
import nars.util.data.list.FasterList;
import nars.video.Scale;
import spacegraph.Facial;
import spacegraph.SpaceGraph;

import java.awt.*;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.util.Collection;

/**
 * Created by me on 10/8/16.
 */
public class RecogChar extends SwingAgent {

    private final Graphics2D g;
    private final int h;
    private final int w;
    private final Collection<SensorConcept> predictions;
    BufferedImage canvas;

    boolean reset = true;
    boolean train = true;
    boolean verify = false;

    int a = 0;

    int image = 0;
    final int maxImages = 4;
    private int TRAINING_PERIOD = 64;
    int imagePeriod = 16;

    public RecogChar(NAR n) {
        super(n, 32);

        w = 20;
        h = 32;
        canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
        g = ((Graphics2D) canvas.getGraphics());

        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 24 ));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        //senseSwitch("(current)", ()-> train ? -1 : image , -1, maxImages);

        predictions = $.newArrayList(maxImages);
        for (int i = 0; i < maxImages; i++) {
            int ii = i;
            SensorConcept x = sense("((#xy --> img) <=>+0 i" + ii + ")", () -> {

                if (train) {
                    return image == ii ? 1f : 0.5f - (1f/maxImages);
                } else {
                        /* compute error, optionally apply feedback */
//                    if (reset) {
//                        beliefs().re
//                        return 0.5f;
//                    }

                    return
                            //0.5f;
                            Float.NaN;
                }
            }).setLatched(false /* for predictive capabiities to not be silenced */);
            predictions.add( x );

            predictors.add(new MutableTask(x.term(), Symbols.QUESTION, null).present(nar.time()));
        }

        //addCamera("x", ()->canvas, w,h, v -> $.t(v, alpha));
        addCamera("img", new Scale(()->canvas, w, h), v -> $.t(v, alpha));


        new Thread(()->{
            Facial f = new Facial(Vis.newBeliefLEDs(predictions, nar));
            new SpaceGraph().add(f.maximize()).show(800,600);
        }).start();

    }

    protected void eval() {

    }



    @Override
    protected float act() {
        if (a++ % imagePeriod == 0) {
            eval();
            nextImage();
        }

        if (nar.time() % TRAINING_PERIOD == TRAINING_PERIOD-1) {
            train = !train;
            verify = !verify;
            if (verify) {
                image = -1;
                reset = true; //for one frame

//                predictions.forEach(p->{
//                    p.beliefs().clear(nar);
//                });
            }
        } else {
            reset = false;
        }

        if (verify) {
            return 0;
        } else {


            return Float.NaN;
        }
    }


    protected int nextImage() {

        image = nar.random.nextInt(maxImages);

        g.clearRect(0, 0, w, h);
        FontMetrics fontMetrics = g.getFontMetrics();

        String s = String.valueOf((char) ('0' + image));
        LineMetrics lineMetrics = fontMetrics.getLineMetrics(s, g);
        //System.out.println(s + " : " + lineMetrics.getHeight() + " pixel height");

        g.drawString(s, 0, lineMetrics.getHeight());
        return image;
    }

    public static void main(String[] arg) {
        SwingAgent.run(RecogChar::new, 100000);
    }
}
