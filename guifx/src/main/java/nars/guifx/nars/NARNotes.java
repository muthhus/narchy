package nars.guifx.nars;

import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import nars.NAR;
import nars.bag.Bag;
import nars.bag.impl.ArrayBag;
import nars.budget.merge.BudgetMerge;
import nars.guifx.InputPane;
import nars.guifx.concept.BagView;
import nars.link.BLink;
import nars.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.function.Consumer;

import static javafx.application.Platform.runLater;
import static nars.util.FX.scrolled;

/**
 * Created by me on 6/2/16.
 */
public class NARNotes extends BorderPane {

    private static final Logger logger = LoggerFactory.getLogger(NARNotes.class);

    private final Pane content;
    private final Taskversation<Note> tasks;
    private final HBox statusPanel;
    private final NAR nar;

    public NARNotes(NAR n) {
        super();

        getStyleClass().add("notes");

        this.nar = n;

        setBottom(new InputPane(n));
        setTop(statusPanel = new HBox());
        setCenter(scrolled(content =
                //new VBox()
                new FlowPane()
        ));

        statusPanel.getChildren().add(new LoopPane(n.loop()));

        tasks = new Taskversation<>(n, this::addTask);

        n.onFrame(nn -> {
           tasks.each().forEach(Note::update);
        });
        //TextField filterText = new TextField();
        //selectorPanel.getChildren().add(filterText);

    }

    private Note addTask(Task t) {
        Note p = new Note(t);
        runLater(()->{
            content.getChildren().add(p);
        });
        return p;
    }

    class Note extends VBox implements Consumer<Task> {
        //private final TextArea edit;
        //private final Button inputButton;
        private final BagView<Task> responseBox;
        private Task task = null;
        Bag<Task> responses = new ArrayBag<Task>(16, BudgetMerge.max, new HashMap<>(16));

        public Note(Task t) {
            getStyleClass().add("note");

            //setTop(new HBox(edit = new TextArea(), inputButton = new Button("->")));
            //edit.setPrefRowCount(1);
            TaskButton title = new TaskButton(t, nar) {
                @Override
                public boolean scalesText() {
                    return false;
                }

                @Override
                protected void color(Color c) {
                    //inverted
                    setBackground(new Background(new BackgroundFill(c, CornerRadii.EMPTY, Insets.EMPTY)));
                    setTextFill(Color.BLACK);
                }
            };
            getChildren().add(title);
            title.getStyleClass().add("header");

            getChildren().add(scrolled(
                responseBox = new BagView<Task>(
                        responses, (x) -> new TaskButton(x, nar), responses.capacity()))
            );
            responseBox.getStyleClass().add("content");

            this.task = t;

//                inputButton.setOnAction(e -> {
//                    runLater( () -> {
//                        try {
//                            setTask(nar.inputTask(edit.getText()));
//                            //addNewEdit();
//                        } catch (Exception ee) {
//                            logger.error("Input: {}", ee);
//                        }
//
//                    });
//                });

        }

        protected void update() {
            responses.commit();
            responseBox.update();
            //runLater(this::layout);
            //}
        }

        @Override
        public void accept(Task t) {
            responses.put(t);

            //responseBox.getChildren().add(new TaskButton(t, nar));
        }
    }

}
