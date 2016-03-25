package nars.guifx;

import impl.org.controlsfx.table.MappedList;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import nars.$;
import nars.NAR;
import nars.guifx.concept.SimpleConceptPane;
import nars.term.Termed;
import org.reactfx.collection.LiveArrayList;

import java.util.function.Function;

/**
 * Panel for displaying a set of concepts for a user perspective.
 * Concepts depicting beliefs represent sensors for the system to be made aware of
 * Concepts depicting goals represent user commands
 *      "top", like unix 'top' command
 *      a set of active processes with controls
 */
public class NARtop extends BorderPane {

    public final Pane content;
    public final LiveArrayList<Termed> concepts;

    public NARtop(NAR nar) {
        this(
            new VBox(),
            (t) -> new SimpleConceptPane(nar, t)
        );
    }

    public NARtop(Pane content, Function<Termed, Node> nodeBuilder) {

        setCenter(this.content = content);

        Bindings.bindContent(content.getChildren(), new MappedList<>(
            this.concepts = new LiveArrayList<>(), nodeBuilder)
        );
    }

    public NARtop addAll(Object... e) {
        for (Object x :e) {

            Termed t;
            if (x instanceof String) {
                t = $.$((String)x);
            } else if (x instanceof Termed) {
                t = (Termed)x;
            } else {
                t = null;
            }

            if (t!=null)
                concepts.add(t);
        }

        return this;
    }
}
