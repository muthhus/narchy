package nars.nal.space;

import nars.Memory;
import nars.bag.Bag;
import nars.concept.DefaultConcept;
import nars.task.Task;
import nars.term.Termed;

/**
 * Concept which additionally characterizes the numeric
 * shape of the vector space formed by known Space terms
 */
public class SpaceConcept extends DefaultConcept {
    public SpaceConcept(Space term, Bag<Task> taskLinks, Bag<Termed> termLinks, Memory p) {
        super(term.anonymous(), taskLinks, termLinks, p);
    }

}
