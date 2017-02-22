package nars.audio;

import jcog.Util;
import jcog.data.random.XorShift128PlusRandom;
import jcog.signal.Autoencoder;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.Narsese;
import nars.concept.SensorConcept;
import nars.gui.Vis;
import nars.nar.NARBuilder;
import nars.time.RealTime;
import spacegraph.SpaceGraph;
import spacegraph.audio.AudioSource;
import spacegraph.audio.WaveCapture;
import spacegraph.render.Draw;
import spacegraph.space.widget.FloatSlider;
import spacegraph.space.widget.MatrixView;

import java.util.List;

import static spacegraph.space.layout.Grid.grid;
import static spacegraph.space.layout.Grid.row;

/**
 * Created by me on 11/29/16.
 */
public class NARHear extends NAgent {

    public static void main(String[] args) {
        new NARHear(NARBuilder.newMultiThreadNAR(2, new RealTime.CS(true).dur(0.2f))).runRT(20);
    }

    public NARHear(NAR nar) {
        super(nar);
        AudioSource audio = new AudioSource(7, 20);
        WaveCapture au = new WaveCapture(
                audio,
                //new SineSource(128),
                20);

        List<SensorConcept> freqInputs = null; //absolute value unipolar
        try {
            freqInputs = senseNumber(0, au.freqSamplesPerFrame,
                    i -> $.func("f", $.the(i)).toString(),

            //        i -> () -> (Util.clamp(au.history[i], -1f, 1f)+1f)/2f); //raw bipolar
                    i -> () -> (Util.sqr(Util.clamp(au.data[i], 0f, 1f))));
        } catch (Narsese.NarseseException e) {
            e.printStackTrace();
        }

        freqInputs.forEach(s -> s.resolution(0.05f));

        Autoencoder ae = new Autoencoder(au.data.length, 16, new XorShift128PlusRandom(1));
        nar.onCycle(f->{
            ae.train(au.data, 0.15f, 0.01f, 0.1f, true, true, true);
        });

        SpaceGraph.window(
                grid(
                    row(
                            au.newMonitorPane(),
                            new FloatSlider(audio.gain)
                    ),
                    new MatrixView(ae.W.length, ae.W[0].length, MatrixView.arrayRenderer(ae.W)),
                    new MatrixView(ae.y, 4, (v, gl) -> { Draw.colorPolarized(gl, v); return 0; }),
                    Vis.conceptLinePlot(nar, freqInputs, 64)
                ),
                1200, 1200);

        //Vis.conceptsWindow2D(nar, 64, 4).show(800, 800);

//            b.setScene(new Scene(au.newMonitorPane(), 500, 400));
//            b.show();
//        });
    }

    @Override
    protected float act() {
        return 0;
    }


}
