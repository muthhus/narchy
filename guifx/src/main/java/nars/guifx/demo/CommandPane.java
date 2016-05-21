//package nars.guifx.demo;
//
//import javafx.beans.property.SimpleDoubleProperty;
//import javafx.beans.value.ChangeListener;
//import javafx.beans.value.ObservableValue;
//import javafx.geometry.Pos;
//import javafx.scene.Node;
//import javafx.scene.layout.BorderPane;
//import javafx.scene.layout.Pane;
//import javafx.scene.layout.VBox;
//import nars.NAR;
//import nars.budget.merge.BudgetMerge;
//import nars.guifx.NARfx;
//import nars.guifx.nars.SubButton;
//import nars.guifx.nars.TaskButton;
//import nars.guifx.util.NSlider;
//import nars.nar.Default;
//import nars.task.Task;
//import nars.task.flow.SetTaskPerception;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.function.Function;
//import java.util.function.Supplier;
//
//import static javafx.application.Platform.runLater;
//
//
//public class CommandPane<N extends Node> implements Supplier<Pane> {
//
//    final SetTaskPerception active;
//    final Map<Task,N> taskNodes = new HashMap();
//    private final NAR nar;
//    private final Pane base;
//    private final Function<Task, N> newNode;
//
//    public CommandPane(NAR n, Pane base, Function<Task,N> builder) {
//        super();
//
//        this.nar = n;
//        this.base = base;
//        this.newNode = builder;
//
//        //setLeft(new TreePane(d));
//
//        active = new SetTaskPerception(n, f -> update(), BudgetMerge.plusDQBlend);
//        n.eventTaskProcess.on(t -> {
//            if (t.isInput()) {
//                runLater( () -> addInput(t));
//            }
//        });
//
//    }
//
//
//    @Override
//    public Pane get() {
//        return base;
//    }
//
//    protected void update() {
//        /*taskButtons.forEach( (k,v) -> {
//           v.update();
//        });*/
//    }
//
//    /**
//     * adds a task to be managed/displayed by this widget
//     */
//    protected void addInput(Task t) {
//        taskNodes.computeIfAbsent(t, k -> {
//            N b = newNode.apply(k);
//            base.getChildren().add(b);
//            return b;
//        });
//    }
//
//    public static class TaskControlButton extends BorderPane {
//
//        private final TaskButton label;
//        private final NSlider priSlider;
//
//        public TaskControlButton(NAR n, Task t) {
//            super();
//
//            setCenter(this.label = new TaskButton<Task>(n, t) {
//                @Override
//                public void run() {
//                    super.run();
//                    update();
//                }
//
//                @Override
//                public boolean scalesText() {
//                    return false;
//                }
//
//            });
//
//            label.setAlignment(Pos.CENTER_LEFT);
//            setAlignment(label, Pos.CENTER_LEFT);
//
//            setLeft(this.priSlider = new NSlider(100f, 20f, 0.5f));
//
//            SimpleDoubleProperty v = priSlider.value[0];
//            v.addListener(new ChangeListener<Number>() {
//                @Override
//                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
//                    t.budget().setPriority(v.floatValue());
//                    runLater( label ); //trigger update
//                }
//            });
//        }
//
//        protected void update() {
//            layout();
//        }
//    }
//
//    public static void main(String[] args) {
//
//        Default n = new Default(1000, 1, 1, 3);
//
//        NARide.show(n.loop(), (i) -> {
//            Function<Task, Node>
//                subbuttonBuilder = t -> SubButton.make(n, t),
//                taskbuttonBuilder = t -> new TaskControlButton(n, t);
//
////            NARfx.newWindow("x",
////                new NARtop(n,
////                    new TextFlow(), subbuttonBuilder).get()
////            );
//            NARfx.newWindow("y",
//                    new CommandPane(n,
//                            //new TilePane(),
//                            new VBox(),
//                            taskbuttonBuilder).get()
//            );
//
//            n.input("$0.70$ <groceries --> [bought]>!");
//            n.input("$0.40$ <plants --> [watered]>!");
//            n.input("$0.30$ <<perimeter --> home> --> secure>?");
//            n.input("$0.50$ <weather <-> [dangerous]>?");
//            n.input("$0.70$ prompt(string, \"Reason?\").");
//            n.input("$0.40$ emote(happy)!");
//            n.input("$0.80$ plot(line, (0, 2, 1, 3), \"Chart\").");
//            n.run(6);
//        });
//    }
//
//}
