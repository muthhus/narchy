//package nars.nar.experimental;
//
//import nars.NAR;
//import nars.bag.BLink;
//import nars.concept.Concept;
//import nars.concept.ConceptProcess;
//import nars.nal.meta.PremiseEval;
//import nars.task.Task;
//import nars.term.Termed;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.function.Consumer;
//
//
///**
// * particle that travels through the graph,
// * responsible for deciding what to derive
// */
//public class Derivelet {
//
//
//    /**
//     * modulating the TTL (time-to-live) allows the system to control
//     * the quality of attention it experiences.
//     * a longer TTL will cause derivelets to restart
//     * less frequently and continue exploring potentially "yarny"
//     * paths of knowledge
//     */
//    int ttl;
//
//
//    /**
//     * current location
//     */
//    public BLink<Concept> concept;
//
//    /**
//     * utility context
//     */
//    public DeriveletContext context;
//
//    @Nullable PremiseEval matcher;
//
//
//    @NotNull
//    private NAR nar() {
//        return context.nar;
//    }
//
//    /**
//     * determines a next concept to move adjacent to
//     * the concept it is currently at
//     */
//    @Nullable
//    public Concept nextConcept() {
//
//        final BLink<Concept> concept = this.concept;
//
//        if (concept == null) {
//            return null;
//        }
//
//        final float x = context.nextFloat();
//        Concept c = concept.get();
//
//        //calculate probability it will stay at this concept
//        final float stayProb = 0.5f;//(concept.getPriority()) * 0.5f;
//        if (x < stayProb) {
//            //stay here
//            return c;
//        } else {
//            float rem = 1.0f - stayProb;
//
//
//            final BLink tl = ((x > (stayProb + (rem / 2))) ?
//                    c.termlinks() :
//                    c.tasklinks())
//                    .sample();
//
//            if (tl != null) {
//                c = context.concept(((Termed) tl.get()));
//                if (c != null) return c;
//            }
//        }
//
//        return null;
//    }
//
//    /**
//     * run next iteration; true if still alive by end, false if died and needs recycled
//     */
//    public final void cycle(final long now) {
//
//        if (this.ttl-- == 0) {
//            //died
//            return ;
//        }
//
//        //TODO dont instantiate BagBudget
//        if ((this.concept = new BLink(nextConcept(), 0, 0, 0)) == null) {
//            //dead-end
//            return;
//        }
//
//
//        int tasklinks = 1;
//        int termlinks = 2;
//
////        firePremiseSquare(context.nar,
////                this.concept,
////                tasklinks, termlinks,
////                null, //Default.simpleForgetDecay,
////                null //Default.simpleForgetDecay
////        );
//
//    }
//
//    @Nullable
//    final Consumer<Task> perDerivation = ( derived) -> {
//        final NAR n = nar();
//
//        /*derived = n.validInput(derived);
//        if (derived != null)*/
//            n.input(derived);
//    };
//
////    final Consumer<ConceptProcess> perPremise = p ->
////            matcher.start(p);
//
//
//    public final void start(final Concept concept, int ttl, @NotNull final DeriveletContext context) {
//        this.context = context;
//        this.concept = new BLink(concept, 0, 0, 0); //TODO
//        this.ttl = ttl;
//        this.matcher = new PremiseEval(context.rng, DeriveletContext.deriver);
//    }
//
//    @NotNull
//    @Override
//    public String toString() {
//        return getClass().getSimpleName() + '@' + concept;
//    }
//
//
//}
