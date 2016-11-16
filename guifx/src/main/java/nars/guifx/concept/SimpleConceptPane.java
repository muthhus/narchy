package nars.guifx.concept;

import javafx.scene.effect.BlendMode;
import javafx.scene.layout.StackPane;
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
    //private final TaskTablePane beliefTable;
    final ConceptMenu menu;

    public SimpleConceptPane(NAR nar, Termed concept) {
        super(nar, concept);

        setCenter(
            //new VBox(
                new StackPane(
                    budgetGraph = new BudgetGraph(nar,
                        Plot2D.BarWave, 96, 0, 32d, concept
                    ),
                    menu = new ConceptMenu(nar, concept)
                )
                //beliefTable = new TaskTablePane(nar)
            //)
        );

        menu.setBlendMode(BlendMode.EXCLUSION);
    }

    @Override
    protected void update(Concept concept, long now) {
        budgetGraph.update();
        //beliefTable.update(concept);
    }
}
