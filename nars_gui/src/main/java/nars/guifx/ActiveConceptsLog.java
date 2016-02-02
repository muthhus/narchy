package nars.guifx;

import javafx.scene.Node;
import nars.Global;
import nars.NAR;
import nars.concept.Concept;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static javafx.application.Platform.runLater;

/**
 * Created by me on 10/15/15.
 */
public abstract class ActiveConceptsLog extends LogPane {

    private List<Node> displayed;
    LinkedHashSet<Concept> display = new LinkedHashSet();
    int maxShown = 64;

    final AtomicBoolean pendingShown = new AtomicBoolean(false);

    long now;

    public ActiveConceptsLog(NAR n) {

        now = n.time();

        n.onFrame(nn-> {
            now = n.time();
            if (displayed!=null)
                displayed.forEach(this::update);
        });

        n.memory.eventTaskProcess.on(tp -> {

            Concept c = n.concept(tp.concept());

            //TODO more efficient:
            display.remove(c);
            display.add(c);

            //if (!pendingUpdate) ..
            //  runLater(update);
            if (pendingShown.compareAndSet(false, true)) {

                displayed = Global.newArrayList();
                Iterator<Concept> ii = display.iterator();
                int toSkip = display.size() - maxShown;
                while (ii.hasNext()) {

                    Concept cc = ii.next();

                    if (toSkip > 0) {
                        ii.remove();
                        toSkip--;
                        continue;
                    }

                    displayed.add( node(cc) );
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
