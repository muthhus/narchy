package nars.guifx.concept;

import nars.NAR;
import nars.guifx.chart.Plot2D;
import nars.term.Termed;

/**
 * Created by me on 3/18/16.
 */
public class BudgetGraph extends Plot2D {

    public BudgetGraph(NAR nar, PlotVis p, int history, double w, double h, Termed term) {
        super(p, history, w, h);
        add(term.toString() + ":pri", () -> nar.conceptPriority(term), 0, 1f);
    }
}
