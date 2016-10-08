package nars.experiment.recog2d;

import nars.$;
import nars.NAR;
import nars.remote.SwingAgent;

import java.awt.*;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;

/**
 * Created by me on 10/8/16.
 */
public class RecogChar extends SwingAgent {

    private final Graphics2D g;
    private final int h;
    private final int w;
    BufferedImage canvas;

    boolean train = true;
    boolean verify = false;

    int imagePeriod = 8;
    int a = 0;

    int image = 0;
    final int maxImages = 9;

    public RecogChar(NAR n) {
        super(n, 32);

        w = 20;
        h = 32;
        canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
        g = ((Graphics2D) canvas.getGraphics());

        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 24 ));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        //senseSwitch("(current)", ()-> train ? -1 : image , -1, maxImages);

        for (int i = 0; i < maxImages; i++) {
            int ii = i;
            action("(img," + ii + ")", (b,d)->{

                if (train) {
                    return $.t(image == ii ? 1f : 0f, alpha);
                } else {
                    /* compute error, optionally apply feedback */
                    return null;
                }
            } );
        }

        addCamera("x", ()->canvas, w,h, v -> $.t(v, alpha));
    }

    protected void eval() {

    }



    @Override
    protected float act() {
        if (a++ % imagePeriod == 0) {
            eval();
            nextImage();
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
        System.out.println(s + " : " + lineMetrics.getHeight() + " pixel height");

        g.drawString(s, 0, lineMetrics.getHeight());
        return image;
    }

    public static void main(String[] arg) {
        SwingAgent.run(RecogChar::new, 100000);
    }
}
