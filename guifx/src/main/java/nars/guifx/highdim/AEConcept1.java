package nars.guifx.highdim;

import nars.concept.Concept;
import nars.guifx.graph2.TermNode;
import nars.link.BLink;
import nars.term.Compound;
import nars.term.Term;
import nars.util.Util;

/**
 * Created by me on 2/29/16.
 */
public class AEConcept1 extends HighDim.AutoEnc2Projection<BLink<? extends Concept>> {

    public AEConcept1() {
        super(19, 8, 4);
    }

    @Override
    public float[] vectorize(BLink<? extends Concept> clink, float[] x) {
        //x[0] = c.op().ordinal() / 16f; //approx;
        //x[1] = clink.pri();
        Concept c = clink.get();
        Term t = c.term();
        x[0] = (c.hashCode() % 8) / 8.0f;
        x[1] = (t instanceof Compound ? 1f : 0f);
        x[2] = clink.pri();
        x[3] = clink.qua();
        Util.writeBits(t.op().ordinal() + 1, 5, x, 4); //+5
        Util.writeBits(t.volume() + 1, 5, x, 9); //+5
        Util.writeBits(t.size() + 1, 5, x, 14); //+5


        //System.out.println(Arrays.toString(x));

        return x;

    }

    @Override
    protected void applyIt(float[] x, TermNode target) {
        //System.out.println(Arrays.toString(x) + " " + Arrays.toString(y));
        float pri = x[2]; /* use original value directly */
        //target.move( (y[0]-y[1]) *250, pri*250);
        //target.move( (y[0]-y[2]) * 500, (y[1]-y[3]) * 500);
        float px = (2 * y1[0] + y1[1]) * 100;
        float py = (2 * y1[2] + y1[3]) * 100;
        target.move(px, py);
        target.scale(1f + pri);
    }
}
