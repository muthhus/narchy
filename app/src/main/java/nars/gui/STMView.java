package nars.gui;

import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.jogamp.opengl.GL2;
import nars.op.time.MySTMClustered;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;


/**
 * Created by me on 7/20/16.
 */
public class STMView extends Surface {


    private final MySTMClustered stm;
    protected int limit = -1;
    private float width, height;

    public STMView(MySTMClustered stm) {
        super();
        this.stm = stm;
    }

    public static void show(MySTMClustered stm, int w, int h) {
        SpaceGraph<VirtualTerminal> s = new SpaceGraph<VirtualTerminal>();
        s.show(w, h);

        BagChart bc;
        s.add(new Facial(bc = new BagChart(stm.bag, -1, 500, 400)));
        //s.add(new Facial(new STMView(stm).size(500, 400)));

        stm.nar.onFrame(xx -> {
            //now = xx.time();
            if (bc.busy.compareAndSet(false, true)) {
                bc.update();
            }
        });
    }

    public STMView size(float w, float h) {
        this.width = w;
        this.height = h;
        return this;
    }

    @Override
    protected void paint(GL2 gl) {
        super.paint(gl);

    }
}