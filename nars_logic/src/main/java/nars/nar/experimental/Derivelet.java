package nars.nar.experimental;

import nars.Global;
import nars.NAR;
import nars.bag.BLink;
import nars.concept.Concept;
import nars.concept.ConceptProcess;
import nars.nal.meta.PremiseMatch;
import nars.task.Task;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.compound.Compound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * particle that travels through the graph,
 * responsible for deciding what to derive
 */
public class Derivelet {


    /**
     * modulating the TTL (time-to-live) allows the system to control
     * the quality of attention it experiences.
     * a longer TTL will cause derivelets to restart
     * less frequently and continue exploring potentially "yarny"
     * paths of knowledge
     */
    int ttl;


    /**
     * current location
     */
    public BLink<Concept> concept;

    /**
     * utility context
     */
    public DeriveletContext context;

    PremiseMatch matcher;

    /**
     * temporary re-usable array for batch firing
     */
    private final Set<BLink<Termed>> terms = Global.newHashSet(1);
    /**
     * temporary re-usable array for batch firing
     */
    private final Set<BLink<Task>> tasks = Global.newHashSet(1);

    @NotNull
    private BLink[] termsArray = new BLink[0];
    @NotNull
    private BLink[] tasksArray = new BLink[0];

    public static void firePremises(@NotNull BLink<Concept> conceptLink, @NotNull BLink<Task>[] tasks, @NotNull BLink<Termed>[] terms, @NotNull Consumer<ConceptProcess> proc, @NotNull NAR nar) {

        for (BLink<Task> taskLink : tasks) {

            if (taskLink == null) break;

            Compound taskLinkTerm = taskLink.get().term();

            for (BLink<Termed> termLink : terms) {
                if (termLink == null) break;

                if (!Terms.equalSubTermsInRespectToImageAndProduct(taskLinkTerm, termLink.get().term()))
                    ConceptProcess.fireAll(
                        nar, conceptLink, taskLink, termLink, proc);
            }
        }

    }


    /**
     * iteratively supplies a matrix of premises from the next N tasklinks and M termlinks
     * (recycles buffers, non-thread safe, one thread use this at a time)
     */
    public void firePremiseSquare(
            @NotNull NAR nar,
            @NotNull Consumer<ConceptProcess> proc,
            @NotNull BLink<Concept> conceptLink,
            int tasklinks, int termlinks,
            Predicate<BLink<Termed>> eachTermLink,
            Predicate<BLink<Task>> eachTaskLink) {

        Concept concept = conceptLink.get();

        Set<BLink<Task>> tasksBuffer = this.tasks;
        concept.getTaskLinks().sample(tasklinks, eachTaskLink, tasksBuffer).commit();
        if (tasksBuffer.isEmpty()) return;

        Set<BLink<Termed>> termsBuffer = this.terms;
        concept.getTermLinks().sample(termlinks, eachTermLink, termsBuffer).commit();
        if (termsBuffer.isEmpty()) return;

        //convert to array for fast for-within-for iterations
        BLink[] tasksArray = this.tasksArray = tasksBuffer.toArray(this.tasksArray);
        tasksBuffer.clear();

        BLink[] termsArray = this.termsArray = termsBuffer.toArray(this.termsArray);
        termsBuffer.clear();

        firePremises(conceptLink,
                tasksArray, termsArray,
                proc, nar);

    }


    @NotNull
    private NAR nar() {
        return context.nar;
    }

    /**
     * determines a next concept to move adjacent to
     * the concept it is currently at
     */
    @Nullable
    public Concept nextConcept() {

        final BLink<Concept> concept = this.concept;

        if (concept == null) {
            return null;
        }

        final float x = context.nextFloat();
        Concept c = concept.get();

        //calculate probability it will stay at this concept
        final float stayProb = 0.5f;//(concept.getPriority()) * 0.5f;
        if (x < stayProb) {
            //stay here
            return c;
        } else {
            float rem = 1.0f - stayProb;


            final BLink tl = ((x > (stayProb + (rem / 2))) ?
                    c.getTermLinks() :
                    c.getTaskLinks())
                    .sample();

            if (tl != null) {
                c = context.concept(((Termed) tl.get()));
                if (c != null) return c;
            }
        }

        return null;
    }

    /**
     * run next iteration; true if still alive by end, false if died and needs recycled
     */
    public final void cycle(final long now) {

        if (this.ttl-- == 0) {
            //died
            return ;
        }

        //TODO dont instantiate BagBudget
        if ((this.concept = new BLink(nextConcept(), 0, 0, 0)) == null) {
            //dead-end
            return;
        }


        int tasklinks = 1;
        int termlinks = 2;

        firePremiseSquare(context.nar,
                perPremise, this.concept,
                tasklinks, termlinks,
                null, //Default.simpleForgetDecay,
                null //Default.simpleForgetDecay
        );

    }

    @Nullable
    final Consumer<Task> perDerivation = ( derived) -> {
        final NAR n = nar();

        derived = n.validInput(derived);
        if (derived != null)
            n.process(derived);
    };

    final Consumer<ConceptProcess> perPremise = p ->
            DeriveletContext.deriver.run(p, matcher, perDerivation);


    public final void start(final Concept concept, int ttl, @NotNull final DeriveletContext context) {
        this.context = context;
        this.concept = new BLink(concept, 0, 0, 0); //TODO
        this.ttl = ttl;
        this.matcher = new PremiseMatch(context.rng);
    }

    @NotNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + '@' + concept;
    }


}
