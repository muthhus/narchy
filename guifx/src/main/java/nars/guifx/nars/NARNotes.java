package nars.guifx.nars;

import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import nars.NAR;
import nars.bag.Bag;
import nars.bag.impl.ArrayBag;
import nars.guifx.InputPane;
import nars.guifx.concept.BagView;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import static javafx.application.Platform.runLater;
import static nars.util.FX.scrolled;

/**
 * Created by me on 6/2/16.
 */
public class NARNotes extends BorderPane {

    private static final Logger logger = LoggerFactory.getLogger(NARNotes.class);

    private final Pane content;
    private final Taskversation tasks;
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

        tasks = new Taskversation(n) {
            @Override
            public @NotNull Consumer<Task> newTaskResponse(Task t) {
                return addTask(t);
            }
        };

        //TextField filterText = new TextField();
        //selectorPanel.getChildren().add(filterText);

    }

    private Consumer<Task> addTask(Task t) {
        Note p = new Note(t);
        runLater(()->{
            content.getChildren().add(p);
        });
        return p;
    }

    class Note extends BorderPane implements Consumer<Task> {
        //private final TextArea edit;
        //private final Button inputButton;
        private final BagView<Task> responseBox;
        private Task task = null;
        Bag<Task> responses = new ArrayBag<>(16);

        public Note(Task t) {
            getStyleClass().add("note");

            //setTop(new HBox(edit = new TextArea(), inputButton = new Button("->")));
            //edit.setPrefRowCount(1);
            TaskButton title = new TaskButton<>(t, nar) {
                @Override
                protected void color(Color c) {
                    //inverted
                    setBackground(new Background(new BackgroundFill(c, CornerRadii.EMPTY, Insets.EMPTY)));
                    setTextFill(Color.BLACK);
                }
            };
            setTop(title);
            title.getStyleClass().add("header");

            setCenter(scrolled(
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

        @Override
        public void accept(Task t) {
            if (responses.put(t)!=t) {
                responseBox.update();
            }

            //responseBox.getChildren().add(new TaskButton(t, nar));
        }
    }

}
