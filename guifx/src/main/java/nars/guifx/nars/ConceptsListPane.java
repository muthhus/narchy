package nars.guifx.nars;

import javafx.scene.Node;
import nars.$;
import nars.NAR;
import nars.concept.Concept;
import nars.guifx.LogPane;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static javafx.application.Platform.runLater;

/**
 * Created by me on 10/15/15.
 */
public abstract class ConceptsListPane extends LogPane {

    private List<Node> displayed;
    final LinkedHashSet<Concept> pendingDisplay = new LinkedHashSet();
    final int maxShown = 64;

    final AtomicBoolean pendingShown = new AtomicBoolean(false);

    final long now;

    public ConceptsListPane(NAR n) {

        now = n.time();

        n.onFrame(nn-> {
            if (displayed!=null)
                displayed.forEach(this::update);
        });

        n.eventTaskProcess.on(tp -> {

            Concept c = n.concept(tp.concept(n));

            //TODO more efficient:
            pendingDisplay.remove(c);
            pendingDisplay.add(c);


            //if (!pendingUpdate) ..
            //  runLater(update);
            if (pendingShown.compareAndSet(false, true)) {

                List<Node> toDisplay = this.displayed = $.newArrayList( Math.max(maxShown, pendingDisplay.size()) );
                Iterator<Concept> ii = pendingDisplay.iterator();
                int toSkip = toDisplay.size() - maxShown;

                while (ii.hasNext()) {

                    Concept cc = ii.next();

                    if (toSkip > 0) {
                        ii.remove();
                        toSkip--;
                        continue;
                    }

                    toDisplay.add( node(cc) );
                }

                runLater(() -> {
                    pendingShown.set(false);
                    commit(displayed);
                });
            }
        });

    }



    final Map<Concept,Node> cache =
            new WeakHashMap();

    public abstract Node make(Concept cc);

    Node node(Concept cc) {
        Node cp = cache.computeIfAbsent(cc, this::make);
        if (cp instanceof ConceptSummaryPane)
            ((ConceptSummaryPane)cp).update(true,true, now);
        return cp;
    }

    protected void update(Node node) {
        if (node instanceof ConceptSummaryPane) {
            ((ConceptSummaryPane)node).update(true, false, now);
        }
    }
}
