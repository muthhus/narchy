package nars.guifx.highdim;

import nars.bag.BLink;
import nars.concept.Concept;
import nars.guifx.graph2.TermNode;
import nars.term.Term;
import nars.util.data.Util;

/**
 * Created by me on 2/29/16.
 */
public class AEConcept1 extends HighDim.AutoEnc2Projection {

    public AEConcept1() {
        super(20, 8, 4);
    }

    @Override
    protected void vectorizeIt(BLink<? extends Concept> clink, float[] x) {
        //x[0] = c.op().ordinal() / 16f; //approx;
        //x[1] = clink.pri();
        Concept c = clink.get();
        Term t = c.term();
        x[0] = (c.hashCode() % 8) / 8.0f;
        x[1] = (t.isCompound() ? 1f : 0f);
        x[2] = clink.pri();
        x[3] = clink.dur();
        x[4] = clink.qua();
        Util.writeBits(t.op().ordinal() + 1, 5, x, 5); //+5
        Util.writeBits(t.volume() + 1, 5, x, 10); //+5
        Util.writeBits(t.size() + 1, 5, x, 15); //+5


        //System.out.println(Arrays.toString(x));

    }

    @Override
    protected void applyIt(float[] x, TermNode target) {
        //System.out.println(Arrays.toString(x) + " " + Arrays.toString(y));
        float pri = x[2]; /* use original value directly */
        //target.move( (y[0]-y[1]) *250, pri*250);
        //target.move( (y[0]-y[2]) * 500, (y[1]-y[3]) * 500);
        target.move((2 * y1[0] + y1[1]) * 100, (2 * y1[2] + y1[3]) * 100);
        target.scale(1f + pri);
    }
}
