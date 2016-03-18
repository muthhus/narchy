package nars.guifx.concept;

import javafx.scene.layout.VBox;
import nars.NAR;
import nars.concept.Concept;
import nars.guifx.chart.Plot2D;
import nars.term.Termed;

/**
 * Created by me on 3/18/16.
 */
public class SimpleConceptPane extends AbstractConceptPane {

    private final BudgetGraph budgetGraph;
    private final BeliefTablePane beliefTable;

    public SimpleConceptPane(NAR nar, Termed concept) {
        super(nar, concept);

        setCenter(
            new VBox(
                new ConceptMenu(nar, concept),
                budgetGraph = new BudgetGraph(nar, Plot2D.BarWave, 96, 192f, 32d, concept),
                beliefTable = new BeliefTablePane(nar)
            )
        );
    }

    @Override
    protected void update(Concept concept, long now) {
        budgetGraph.update();
        beliefTable.update(concept);
    }
}
