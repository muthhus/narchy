package nars.nal.space;

import nars.bag.Bag;
import nars.concept.DefaultConcept;
import nars.task.Task;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

/**
 * Concept which additionally characterizes the numeric
 * shape of the vector space formed by known Space terms
 */
public class SpaceConcept extends DefaultConcept {
    public SpaceConcept(@NotNull Space term, Bag<Task> taskLinks, Bag<Termed> termLinks) {
        super(term.anonymous(), taskLinks, termLinks);
    }

}
