package nars.audio;

import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.concept.SensorConcept;
import nars.gui.ConceptIcon;
import nars.remote.NAgents;
import nars.time.RealTime;
import nars.util.Util;
import spacegraph.SpaceGraph;
import spacegraph.audio.AudioSource;
import spacegraph.audio.WaveCapture;
import spacegraph.obj.widget.FloatSlider;

import java.util.List;

import static spacegraph.obj.layout.Grid.*;

/**
 * Created by me on 11/29/16.
 */
public class NARHear extends NAgent {

    public static void main(String[] args) {
        new NARHear(NAgents.newMultiThreadNAR(2, new RealTime.CS(true))).runRT(10);
    }

    public NARHear(NAR nar) {
        super(nar);
        AudioSource audio = new AudioSource(7, 20);
        WaveCapture au = new WaveCapture(
                audio,
                //new SineSource(128),
                20);

        List<SensorConcept> freqInputs = senseNumber(0, au.freqSamplesPerFrame,
                i -> $.func("a", $.the(i)).toString(),
                i -> () -> (Util.clamp(au.history[i], -1f, 1f)+1f)/2f);

        SpaceGraph.window(
                col(
                    row(
                            au.newMonitorPane(),
                            new FloatSlider(audio.gain)
                    ),
                    grid(freqInputs, s -> new ConceptIcon(nar, s))
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
