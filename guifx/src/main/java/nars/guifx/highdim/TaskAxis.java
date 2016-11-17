package nars.guifx.highdim;

import javafx.scene.Group;
import javafx.scene.control.Label;
import nars.Task;
import nars.guifx.graph2.TermNode;
import nars.term.Term;
import nars.util.Util;

/**
 * TODO multiple modes:
 *      time
 *      truth freq
 *      truth conf
 *      truth expect
 *      pri
 *      summary
 *      complexity
 */
public class TaskAxis extends HighDim.AutoEnc2Projection<Task> {

    public final Group axes;

    final double xscale = 100;

    public TaskAxis() {
        super(21, 8, 4);

        this.axes = new Group();
        for (int i = 0; i < 10; i++) {
            Label l = new Label(Integer.toString(i));
            l.setTranslateX(i * xscale);
            axes.getChildren().add(l);
        }
    }

    @Override
    public float[] vectorize(Task c, float[] x) {
        //x[0] = c.op().ordinal() / 16f; //approx;
        //x[1] = clink.pri();

        Term t = c.term();
        //x[0] = (c.hashCode() % 32) / 32.0f;
        //x[1] = (t instanceof Compound ? 1f : 0f);
        x[0] = c.priIfFiniteElseZero();
        x[1] = c.qua();
        Util.writeBits(t.op().ordinal() + 1, 5, x, 2); //+5
        Util.writeBits(t.volume() + 1, 5, x, 7); //+5
        Util.writeBits(t.hashCode(), 5, x, 12); //+5
        Util.writeBits((int)(x[0] * 16), 4, x, 17); //+5
        //Util.writeBits(t.size() + 1, 5, x, 13); //+5



        //System.out.println(Arrays.toString(x));

        return x;
    }

    @Override
    protected void applyIt(float[] x, TermNode<Task> target) {
        //System.out.println(Arrays.toString(x) + " " + Arrays.toString(y));
        float pri = x[2]; /* use original value directly */
        //target.move( (y[0]-y[1]) *250, pri*250);
        //target.move( (y[0]-y[2]) * 500, (y[1]-y[3]) * 500);
        float py = (y1[0] * 1 + y1[1] * 2 + y1[2] * 4) * 50;


        long when = target.term.occurrence();


        double px = when * xscale;
        //System.out.println(target + " " + px + " " + py);
        target.move(px, py);
        target.scale(1f + pri);


    }
}
