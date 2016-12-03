package nars.guifx.nars;

import nars.NAR;
import nars.guifx.chart.Plot2D;
import nars.guifx.chart.PlotBox;

/**
 * Created by me on 3/26/16.
 */
public class NARPlot extends PlotBox {

    private final NAR nar;

    public NARPlot(NAR n, Plot2D... charts) {
        super(charts);
        this.nar = n;
        n.onCycle((nn) -> update());

    }
}
