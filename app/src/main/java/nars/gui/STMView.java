package nars.gui;

import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.jogamp.opengl.GL2;
import nars.learn.gng.NeuralGasNet;
import nars.op.time.MySTMClustered;
import nars.op.time.STMClustered;
import org.jetbrains.annotations.NotNull;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.obj.ConsoleSurface;
import spacegraph.render.ShapeDrawer;


/**
 * Created by me on 7/20/16.
 */
public class STMView extends Surface {


    private BagChart inputBagChart;
    private final MySTMClustered stm;
    protected int limit = -1;
    private float width, height;
    public final NeuralGasNet.NeuralGasNetState state = new NeuralGasNet.NeuralGasNetState();

    public STMView(MySTMClustered stm, SpaceGraph s) {
        super();
        this.stm = stm;

        inputBagChart = new BagChart(stm.input, -1);
        s.add(new Facial(inputBagChart).scale(100, 100));

        s.add(new Facial(new ConsoleSurface(
                new LoggerTerminal(stm.logger, 50,20)
        )).scale(500f, 400f));

        s.add(new Facial(this).scale(500,500));

        stm.nar.onFrame(xx -> {
            update();
        });
    }

    public static void show(MySTMClustered stm, int w, int h) {
        SpaceGraph<VirtualTerminal> s = new SpaceGraph<VirtualTerminal>();
        s.show(w, h);

        STMView sv = new STMView(stm, s);

    }

    public void update() {
        inputBagChart.update();

        synchronized(state) {
            state.update(stm.net).normalize();
        }

    }

    public STMView size(float w, float h) {
        this.width = w;
        this.height = h;
        return this;
    }

    @Override
    protected void paint(GL2 gl) {
        super.paint(gl);

        if (state.range.length == 0)
            return;

        if (state.range.length!=2)
            throw new UnsupportedOperationException("only dim=2 supported currently");

        float s = 1f;
        float w = 0.02f;

        double[][] coord = state.coord;

        @NotNull NeuralGasNet<STMClustered.TasksNode> net = stm.net;

        for (int i = 0, coordLength = coord.length; i < coordLength; i++) {

            STMClustered.TasksNode n = net.node(i);

            float size = n.size();
            if (size == 0)
                continue;

            double[] c = coord[i];

            float p = n.priSum()/size;

            float x = (float) c[0];
            float y = (float) c[1];

            gl.glColor4f(p, 0.5f, 0, 0.75f);

            float r = w * (float)Math.sqrt(size);

            ShapeDrawer.rect(gl, x * s, y * s, r, r);
        }

    }
}