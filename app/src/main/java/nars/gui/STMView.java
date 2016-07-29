package nars.gui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.jogamp.opengl.GL2;
import nars.learn.gng.NeuralGasNet;
import nars.link.BLink;
import nars.op.time.MySTMClustered;
import nars.op.time.STMClustered;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.obj.ConsoleSurface;
import spacegraph.obj.GridSurface;
import spacegraph.obj.LayerSurface;
import spacegraph.render.Draw;


/**
 * Created by me on 7/20/16.
 */
public class STMView  {


    private BagChart inputBagChart;
    private final MySTMClustered stm;
    protected int limit = -1;
    public final NeuralGasNet.NeuralGasNetState state = new NeuralGasNet.NeuralGasNetState();

    class BubbleChart extends Surface {

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

                Draw.rect(gl, x * s, y * s, r, r);
            }

        }

    }
    public STMView(MySTMClustered stm, SpaceGraph s) {
        super();
        this.stm = stm;

        final float maxVolume = 64;

        s.add(new Facial(
                    new GridSurface(
                        inputBagChart = new BagChart<Task>(stm.input, -1) {
                            protected ItemVis<BLink<Task>> newItem(BLink<Task> i) {
                                @Nullable Task ii = i.get();
                                String label;
                                if (ii != null)
                                    label =  ii.toStringWithoutBudget();
                                else
                                    label = "?";

                                return new ItemVis<>(i, label(label, 16));
                            }
                        },


                        new LayerSurface(
                            new ConsoleSurface(new TopicTerminal<Task>(
                                stm.generate,
                                    (Task t) -> t.toStringWithoutBudget(),
                                    (Task t) -> {
                                        float p = t.pri() * 0.5f + 0.5f;
                                        float c = t.conf() * 0.5f + 0.5f;
                                        float f = t.freq();
                                        return TextColor.rgb((1f - f) * p * c, f * p * c, p * t.qua());
                                    },
                                    null,  //(Task t) -> TextColor.hsb(t.term().volume()/maxVolume, 0.8f, 0.25f),
                                    50, 24
                            )),
                            new BubbleChart()
                        )
                )).maximize());

        stm.nar.onFrame(xx -> {
            update();
        });


    }

    public static void show(MySTMClustered stm, int w, int h) {
        SpaceGraph<VirtualTerminal> s = new SpaceGraph<VirtualTerminal>() {
            @Override
            public void init(GL2 gl) {
                super.init(gl);
                //window.setUndecorated(true);
            }
        };
        s.show(w, h);

        STMView sv = new STMView(stm, s);


    }

    public void update() {
        inputBagChart.update();

        synchronized(state) {
            state.update(stm.net).normalize();
        }

    }


}