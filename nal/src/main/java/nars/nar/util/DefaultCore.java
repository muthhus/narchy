package nars.nar.util;

import nars.NAR;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.budget.merge.BudgetMerge;
import nars.concept.Concept;
import nars.link.BLink;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * groups each derivation's tasks as a group before inputting into
 * the main perception buffer, allowing post-processing such as budget normalization.
 * <p>
 * ex: this can ensure that a premise which produces many derived tasks
 * will not consume budget unfairly relative to another premise
 * with less tasks but equal budget.
 */
public class DefaultCore extends AbstractCore {


    public DefaultCore(@NotNull NAR nar) {
        super(nar);
    }


    /** called when a concept is displaced from the concept bag */
    protected void sleep(@NotNull Concept c) {
        NAR n = this.nar;

        n.policy(c, n.index.conceptBuilder().sleep(), n.time());

        n.emotion.alert(1f / concepts.size());
    }

    /** called when a concept enters the concept bag
     * @return whether to accept the item into the bag
     * */
    protected boolean awake(@NotNull Concept c) {

        NAR n = this.nar;
        n.policy(c, n.index.conceptBuilder().awake(), n.time());

        return true;
    }

    @NotNull
    @Override
    protected Bag<Concept> newConceptBag() {

        return new MonitoredCurveBag(nar, 1, ((DefaultConceptBuilder)nar.index.conceptBuilder()).defaultCurveSampler);

    }

    /** extends CurveBag to invoke entrance/exit event handler lambda */
    public final class MonitoredCurveBag extends CurveBag<Concept> {

        final NAR nar;

        public MonitoredCurveBag(NAR nar, int capacity, @NotNull CurveSampler sampler) {
            super(capacity, sampler, BudgetMerge.plusBlend, new ConcurrentHashMap<>(capacity));
            this.nar = nar;
        }

        @Override
        public void clear() {
            forEach((BLink<Concept> v) -> { if (v!=null) sleep(v.get()); }); //HACK allow opportunity to process removals
            super.clear();
        }

        @Override
        protected void onActive(@NotNull Concept c) {
            awake(c);
        }

        @Override
        protected void onRemoved(@NotNull Concept c, BLink<Concept> value) {
            if (value!=null)
                sleep(c);
        }

        @Override
        public @Nullable BLink<Concept> remove(@NotNull Concept x) {
            BLink<Concept> r = super.remove(x);
            if (r!=null) {
                sleep(x);
            }
            return r;
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
