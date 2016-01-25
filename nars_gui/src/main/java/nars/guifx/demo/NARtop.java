package nars.guifx.demo;

import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import javafx.scene.text.TextFlow;
import nars.NAR;
import nars.budget.BudgetMerge;
import nars.guifx.NARfx;
import nars.nar.Default;
import nars.task.Task;
import nars.task.flow.SetTaskPerception;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static javafx.application.Platform.runLater;

/**
 * Created by me on 12/12/15.
 */
public class NARtop implements Supplier<Pane> {

    final SetTaskPerception active;
    //final FlowPane buttons = new FlowPane();
    final Map<Task,SubButton> taskButtons = new HashMap();
    private final NAR nar;
    private final Pane base;

    public NARtop(NAR d, Pane base) {
        super();

        this.nar = d;
        this.base = base;

        //setLeft(new TreePane(d));

        active = new SetTaskPerception(d.memory, f -> {
            update();
        }, BudgetMerge.plusDQDominated);
        d.memory.eventTaskProcess.on(t -> {
            if (t.isInput()) {
                runLater( () -> {
                    addInput(t);
                });
            }
        });

    }


    @Override
    public Pane get() {
        return base;
    }

    protected void update() {
        /*taskButtons.forEach( (k,v) -> {
           v.update();
        });*/
    }

    /**
     * adds a task to be managed/displayed by this widget
     */
    protected void addInput(Task t) {
        
        taskButtons.computeIfAbsent(t, k -> {
            //TaskButton b = new TaskButton(nar,k);
            SubButton b = SubButton.make(nar, k);
            base.getChildren().add(b);
            return b;
        });
    }

    public static void main(String[] args) {

        Default n = new Default(1000, 1, 1, 3);

        NARide.show(n.loop(), (i) -> {
            NARfx.newWindow("x",
                    new NARtop(n, new TextFlow()).get()
            );
            NARfx.newWindow("y",
                    new NARtop(n, new TilePane()).get()
            );

            n.input("$0.70$ <groceries --> [bought]>!");
            n.input("$0.40$ <plants --> [watered]>!");
            n.input("$0.30$ <<perimeter --> home> --> secure>?");
            n.input("$0.50$ <weather <-> [dangerous]>?");
            n.input("$0.70$ prompt(string, \"Reason?\").");
            n.input("$0.40$ emote(happy)!");
            n.input("$0.80$ plot(line, (0, 2, 1, 3), \"Chart\").");
            n.run(6);
        });
    }

}
