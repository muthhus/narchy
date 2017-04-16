package nars.gui;

import jcog.event.On;
import nars.$;
import nars.NAR;
import nars.util.data.Mix;
import spacegraph.Surface;
import spacegraph.layout.Grid;
import spacegraph.widget.meter.Plot2D;
import spacegraph.widget.slider.FloatSlider;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by me on 4/3/17.
 */
public class MixBoard extends Grid implements Consumer<NAR> {

    private final On on;
    private final Plot2D priInPlot, priOutPlot;
    private final Plot2D quaPlot;
    private final Mix mix;

    public MixBoard(NAR nar, Mix<Object, ?> mix) {
        super(HORIZONTAL);

        this.mix = mix;

        List<Surface> sliders = $.newArrayList();

        priInPlot = new Plot2D(32, Plot2D.Line);
        priOutPlot = new Plot2D(32, Plot2D.Line);
        quaPlot = new Plot2D(32, Plot2D.Line);
        mix.streams.forEach((k, v) -> {
            priInPlot.add(k + " in", v.priMeterIn::sum);
            priOutPlot.add(k + " out", v.priMeterOut::sum);
            sliders.add(
                new FloatSlider(v) {
                    @Override public String labelText() {
                        return k.toString();
                    }
                }
            );
        });

        set(col(sliders), col(priInPlot, priOutPlot, quaPlot));

        on = nar.onCycle(this);
    }

    @Override
    public Surface hide() {
        on.off();
        return this;
    }

    @Override
    public void accept(NAR pLinks) {
        priInPlot.update();
        priOutPlot.update();

        quaPlot.update();

        mix.commit();
    }

//    private static class PriDifference implements DoubleSupplier {
//        private final PeriodMeter v;
//        double last;
//
//        public PriDifference(PeriodMeter v) {
//            this.v = v;
//            last = 0;
//        }
//
//        @Override
//        public double getAsDouble() {
//            double s = v.sum();
//            double last = this.last;
//            this.last = s;
//            return s-last;
//        }
//
//    }
}
