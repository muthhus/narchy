package nars.guifx.concept;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.BorderPane;
import nars.NAR;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.Termed;
import nars.util.event.FrameReaction;

import static javafx.application.Platform.runLater;

/**
 * Created by me on 3/18/16.
 */
public abstract class AbstractConceptPane extends BorderPane implements ChangeListener {

    protected final NAR nar;
    protected final Term term;

    protected transient Concept currentConcept; //caches current value, updated on each frame

    private FrameReaction reaction;

    public AbstractConceptPane(NAR nar, Termed concept) {
        super();
        this.nar = nar;
        this.term = concept.term();

        runLater(()->{
            visibleProperty().addListener(this);
            changed(null,null,null);
        });

        getStyleClass().add("ConceptPane");
    }

    protected abstract void update(Concept c, long now);

    @Override
    public void changed(ObservableValue observable, Object oldValue, Object newValue) {

        if (isVisible()) {
            if (reaction==null) {
                reaction = new FrameReaction(nar) {
                    @Override
                    public void onFrame() {
                        update(
                            currentConcept = nar.concept(term),
                            nar.time()
                        );
                    }
                };
            }
        }
        else {
            if (reaction!=null) {
                reaction.off();
                reaction = null;
            }
        }
    }
}
