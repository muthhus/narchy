package nars.nar.util;

import nars.NAR;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.budget.forget.Forget;
import nars.budget.merge.BudgetMerge;
import nars.budget.policy.DefaultConceptBudgeting;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nal.meta.PremiseEval;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * groups each derivation's tasks as a group before inputting into
 * the main perception buffer, allowing post-processing such as budget normalization.
 * <p>
 * ex: this can ensure that a premise which produces many derived tasks
 * will not consume budget unfairly relative to another premise
 * with less tasks but equal budget.
 */
public class DefaultCore extends AbstractCore {

    private final DefaultConceptBudgeting cold;
    private final DefaultConceptBudgeting warm;


    public DefaultCore(@NotNull NAR nar, PremiseEval matcher, DefaultConceptBudgeting warm, DefaultConceptBudgeting cold) {
        super(nar, matcher);
        this.warm = warm;
        this.cold = cold;
    }


    /** called when a concept is displaced from the concept bag */
    protected void deactivate(@NotNull BLink<Concept> cl) {
        Concept c = cl.get();

        //apply forgetting so that shrinking capacity will be applied to concept's components fairly
        c.tasklinks().commit(Forget.QualityToPriority);
        c.termlinks().commit(Forget.QualityToPriority);

        c.capacity(cold);

        nar.emotion.alert(1f/ concepts.size());
    }

    /** called when a concept enters the concept bag */
    protected void activate(@NotNull Concept c) {

        //set capacity first in case there are any queued items, they may join during the commit */
        c.capacity(warm);

        //clean out any deleted links since having been deactivated
        c.tasklinks().commit(Forget.QualityToPriority);
        c.termlinks().commit(Forget.QualityToPriority);
    }

    @NotNull
    @Override
    protected Bag<Concept> newConceptBag() {
        return new MonitoredCurveBag(nar, 1, nar.random)
                .merge(BudgetMerge.
                        avgDQBlend
                        //plusDQDominant
                );
    }

    /** extends CurveBag to invoke entrance/exit event handler lambda */
    public final class MonitoredCurveBag extends CurveBag<Concept> {

        final NAR nar;

        public MonitoredCurveBag(NAR nar, int capacity, @NotNull Random rng) {
            super(capacity, rng);
            this.nar = nar;
            setCapacity(capacity);
        }


        @Override
        protected @Nullable BLink<Concept> putNew(@NotNull Concept i, @NotNull BLink<Concept> b) {
            BLink<Concept> displaced = super.putNew(i, b);
            if (displaced!=null) {

                Concept dd = displaced.get();
                if (dd != i)
                    activate(i);

                deactivate(displaced);

            } else {
                activate(i);
            }

            return displaced;
        }

        @Override
        public @Nullable BLink<Concept> remove(Concept x) {
            BLink<Concept> removed = super.remove(x);
            if (removed!=null) {
                deactivate(removed);
            }
            return removed;
        }


    }





//        @NotNull
//        static HashBag<Task> detectDuplicates(@NotNull Collection<Task> buffer) {
//            HashBag<Task> taskCount = new HashBag<>();
//            taskCount.addAll(buffer);
//            taskCount.forEachWithOccurrences((t, i) -> {
//                if (i == 1) return;
//
//                System.err.println("DUPLICATE TASK(" + i + "): " + t);
//                List<Task> equiv = buffer.stream().filter(u -> u.equals(t)).collect(toList());
//                HashBag<String> rules = new HashBag();
//                equiv.forEach(u -> {
//                    String rule = u.getLogLast().toString();
//                    rules.add(rule);
//
////                    System.err.println("\t" + u );
////                    System.err.println("\t\t" + rule );
////                    System.err.println();
//                });
//                rules.forEachWithOccurrences((String r, int c) -> System.err.println("\t" + c + '\t' + r));
//                System.err.println("--");
//
//            });
//            return taskCount;
//        }

}
