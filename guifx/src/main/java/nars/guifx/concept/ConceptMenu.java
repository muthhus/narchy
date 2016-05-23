package nars.guifx.concept;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import nars.NAR;
import nars.budget.UnitBudget;
import nars.guifx.NARfx;
import nars.guifx.nars.NARActionButton;
import nars.guifx.util.SimpleMenuItem;
import nars.nal.Tense;
import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Termed;

import static javafx.application.Platform.runLater;

/**
 * Created by me on 3/18/16.
 */
public class ConceptMenu extends FlowPane {

    public ConceptMenu(NAR nar, Termed t) {
        super();

        setAlignment(Pos.TOP_LEFT);

        Button button = new Button(t.toString());

        ContextMenu menu = new ContextMenu();
        button.setOnMouseClicked(e->{
            if (e.getButton() == MouseButton.PRIMARY) {
                menu.show(button, e.getScreenX(), e.getScreenY());
            }
        });

        Button activateButton = new NARActionButton(nar, "+", (n) -> {
                n.conceptualize(t, new UnitBudget(1f, 0.75f, 0.75f), 1f, 1f, null);
        });
        Button yesGoalButton = new NARActionButton(nar, "+!", (n) -> n.input(new MutableTask(t, '!', 1f, nar).present(nar).log("GUI Goal")));
        Button noGoalButton = new NARActionButton(nar, "-!", (n) -> n.input(new MutableTask(t, '!', 0f, nar).present(nar).log("GUI Goal")));
        Button trueButton = new NARActionButton(nar, "T", (n) -> n.input(new MutableTask(t, '.', 1f, nar).present(nar).log("GUI True")));
        Button falseButton = new NARActionButton(nar, "F", (n) -> n.input(new MutableTask(t, '.', 0f, nar).present(nar).log("GUI False")));

        Button isTrueButton = new NARActionButton(nar, "?", (n) -> n.input(new MutableTask(t, '?', null).time(Tense.Future, nar).log("GUI Question")));
        Button shouldIButton = new NARActionButton(nar, "@", (n) -> n.input(new MutableTask(t, '@', null).time(Tense.Future, nar).log("GUI Quest")));

        if (!(t instanceof Compound)) {
            trueButton.setVisible(false); //TODO dont create these task buttons in the first place
            falseButton.setVisible(false);
            yesGoalButton.setVisible(false);
            noGoalButton.setVisible(false);
            isTrueButton.setVisible(false);
            shouldIButton.setVisible(false);
        }


        menu.getItems().add(new SimpleMenuItem("Expand", () -> nar.runLater(() -> {

            //System.out.println(term + ": " + nar.conceptPriority(term, Float.NaN));
            runLater(()-> {
                NARfx.newWindow(nar, t);
            });

        })));
        menu.getItems().add(new SimpleMenuItem("Dump", () -> nar.runLater(() -> {

            //System.out.println(term + ": " + nar.conceptPriority(term, Float.NaN));
            nar.concept(t).print();

        })));

        getChildren().addAll(button, activateButton, trueButton, falseButton, yesGoalButton, noGoalButton, isTrueButton, shouldIButton);

    }

}
