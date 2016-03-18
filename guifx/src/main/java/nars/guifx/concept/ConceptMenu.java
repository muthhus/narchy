package nars.guifx.concept;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.FlowPane;
import nars.NAR;
import nars.budget.UnitBudget;
import nars.guifx.nars.NARActionButton;
import nars.guifx.util.SimpleMenuItem;
import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Termed;

import static nars.$.neg;

/**
 * Created by me on 3/18/16.
 */
public class ConceptMenu extends FlowPane {

    public ConceptMenu(NAR nar, Termed t) {
        super();

        setAlignment(Pos.TOP_LEFT);

        Menu conceptMenu = new Menu(t.toString());

        Button activateButton = new NARActionButton(nar, "+", (n) -> n.conceptualize(t, new UnitBudget(1f, 0.75f, 0.75f), 1f));
        Button yesGoalButton = new NARActionButton(nar, "+!", (n) -> n.input(new MutableTask(t, '!').present(nar).log("GUI Goal")));
        Button noGoalButton = new NARActionButton(nar, "-!", (n) -> n.input(new MutableTask(t, '!').truth(0f, (nar).getDefaultConfidence('!')).present(nar).log("GUI Goal")));
        Button trueButton = new NARActionButton(nar, "T", (n) -> n.input(new MutableTask(t, '.').present(nar).log("GUI True")));
        Button falseButton = new NARActionButton(nar, "F", (n) -> n.input(new MutableTask(neg(t.term()), '.').present(nar).log("GUI False")));
        Button isTrueButton = new NARActionButton(nar, "?", (n) -> n.input(new MutableTask(t, '?').present(nar).log("GUI Question")));

        if (!(t instanceof Compound)) {
            trueButton.setVisible(false); //TODO dont create these task buttons in the first place
            falseButton.setVisible(false);
            yesGoalButton.setVisible(false);
            noGoalButton.setVisible(false);
            isTrueButton.setVisible(false);
        }


        conceptMenu.getItems().add(new SimpleMenuItem("Dump", () -> nar.runLater(() -> {

            //System.out.println(term + ": " + nar.conceptPriority(term, Float.NaN));
            nar.concept(t).print();

        })));

        getChildren().addAll(new MenuBar(conceptMenu), activateButton, trueButton, falseButton, yesGoalButton, noGoalButton, isTrueButton);

    }

}
