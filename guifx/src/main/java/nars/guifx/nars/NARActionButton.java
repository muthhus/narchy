package nars.guifx.nars;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import nars.NAR;

import java.util.function.Consumer;

/**
 * supplied action is queued in nars-safe thread
 */
public class NARActionButton extends Button implements EventHandler, Runnable {
    private final Consumer<NAR> action;
    private final NAR nar;

    public NARActionButton(NAR n, String label, Consumer<NAR> action) {
        super(label);
        setOnMouseClicked(this);
        this.action = action;
        this.nar = n;
    }

    @Override
    public final void handle(Event event) {
        nar.runLater(this);
    }

    @Override
    public final void run() {
        action.accept(nar);
    }
}
