package nars.control;

import jcog.bag.Bag;
import jcog.pri.PLink;
import jcog.pri.PLinkUntilDeleted;
import jcog.pri.PriReference;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.task.ITask;
import nars.task.UnaryTask;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

import static nars.Op.INT;

/**
 * concept firing, activation, etc
 */
public class Activate extends UnaryTask<Concept> implements Termed {

    static final int TASKLINKS_SAMPLED = 2;
    static final int TERMLINKS_SAMPLED = 2;
    //private final BiFunction<Task,Term,Premise> premiseBuilder ;


    public Activate(@NotNull Concept c, float pri) {
        super(c, pri);
        assert (c.isNormalized()) :
                c + " not normalized";
    }


    public static void activate(@NotNull Task t, float activationApplied, @NotNull NAR n) {
//        if (n.exe.concurrent()) {
//            n.exe.execute(() -> activate(t, activationApplied, n, true));
//        } else {
        activate(t, activationApplied, n, true);
//        }
    }

    static void activate(@NotNull Task t, float activationApplied, @NotNull NAR n, boolean process) {
        // if (Util.equals(activation, t.priElseZero(), Pri.EPSILON))  //suppress emitting re-activations
        //if (activation >= EPSILON) {
        Concept cc = t.concept(n, true);
        if (cc != null) {

            Activate a = activate(t, activationApplied, cc, n);
            if (t.isInput()) {
                //sync run immediately
                a.run(n);
            }

            n.input(a);

//                        a = (BiConsumer<ConceptFire,NAR>) new Activate.ActivateSubterms(t, activation);
//                n.input(a);
        }

        if (process) {
//            if (n.exe.concurrent())
//                n.eventTask.emitAsync(/*post*/t, n.exe);
//            else
            n.eventTask.emit(t);
        }
        //}
    }

    public static Activate activate(@NotNull Task t, float activation, Concept origin, NAR n) {


//        if (activation < EPSILON) {
//            return null;
//        }

        n.emotion.onActivate(t, activation, origin, n);


        origin.tasklinks().putAsync(
                new PLinkUntilDeleted<>(t, activation)
                //new PLink<>(t, activation)
        );


//            if (origin instanceof CompoundConcept) {
//
//                //return new ConceptFire(origin, activation, new ActivateSubterms(t, activation));
//                return new ConceptFire(origin, activation);
//            } else {
//                //atomic activation)

        return new Activate(origin, activation); /*, () -> {

            }*/
//            }

    }

    @Override
    public List<Premise> run(NAR nar) {
        nar.emotion.conceptFires.increment();
        //nar.terms.commit(id); //index cache update

        final Bag<Task, PriReference<Task>> tasklinks = id.tasklinks();


        tasklinks.commit();

        int talSampled = Math.min(tasklinks.size(), TASKLINKS_SAMPLED);
        if (talSampled == 0)
            return null;

        List<PriReference<Task>> taskl = $.newArrayList(talSampled);
        tasklinks.sample(talSampled, ((Consumer<PriReference>) taskl::add));
        if (taskl.isEmpty()) {
            //nar.emotion.count("ConceptFire_run_but_zero_taskslinks_selected");
            return null;
        }


        //concept priority transfers into:
        //      its termlinks to subterms
        //      and their reverse links back to this

        float cPri = priElseZero();
        float decayRate = 1f - nar.momentum.floatValue();
        float decayed = cPri * decayRate;
        priSub(decayed); //for balanced budgeting: important

        final Bag<Term, PriReference<Term>> termlinks = id.termlinks().commit();//.normalize(0.1f);

        Set<Concept> localSubConcepts = linkTemplates(nar, decayed);


        List<PriReference<Term>> terml;
        int tlSampled = Math.min(termlinks.size(), TERMLINKS_SAMPLED);
        if (tlSampled == 0) {
            return null;
        } else {
            terml = $.newArrayList(tlSampled);
            termlinks.sample(tlSampled, ((Consumer<PriReference>) terml::add));
        }

        int tasklSize = taskl.size();
        if (tasklSize == 0) return null;
        int termlSize = terml.size();
        if (termlSize == 0) return null;

        List<Premise> premises = $.newArrayList(tasklSize * termlSize);

        for (int i = 0; i < tasklSize; i++) {
            PriReference<Task> tasklink = taskl.get(i);


            for (int j = 0; j < termlSize; j++) {
                PriReference<Term> termlink = terml.get(j);

                final Task task = tasklink.get();

                float pri = Param.tasktermLinkCombine.apply(task.priElseZero(), termlink.priElseZero());

                Premise p = new Premise(task, termlink.get(), pri, localSubConcepts);
                premises.add(p);
            }
        }


        return premises;
    }

    @NotNull
    public Set<Concept> linkTemplates(NAR nar, float decayed) {
        Collection<Termed> localTemplates = id.templates();


        if (!localTemplates.isEmpty()) {
            float subDecay = decayed / localTemplates.size();
            Term thisTerm = id.term();

            Set<Concept> localSubConcepts = new HashSet<>(); //temporary for this function call only, so as not to retain refs to Concepts

            Random rng = nar.random();
            float balance = Param.TERMLINK_BALANCE;
            float subDecayReverse = subDecay * (1f - balance);
            float subDecayForward = subDecay * balance;
            Bag<Term, PriReference<Term>> termlinks = id.termlinks();
            for (Termed localSub : localTemplates) {

                Term localSubTerm = localSub.term();

                localSub = mutateTermlink(localSubTerm, rng); //for special Termed instances, ex: RotatedInt etc
                if (localSub instanceof Bool)
                    continue; //unlucky mutation

                float d;
                if (localSubTerm.op().conceptualizable && !localSubTerm.equals(thisTerm)) {

                    Concept localSubConcept = nar.conceptualize(localSub);
                    if (localSubConcept != null) {
                        localSubConcept.activate(subDecay, nar);

                        localSubConcept.termlinks().putAsync(
                                new PLink(thisTerm, subDecayReverse)
                        );
                        localSubConcepts.add(localSubConcept);
                    }

                }

                termlinks.putAsync(
                        new PLink(localSubTerm, subDecayForward)
                );

            }

            return localSubConcepts;
        }

        return Collections.emptySet();

    }

    /**
     * preprocess termlink
     */
    private static Termed mutateTermlink(Term t, Random rng) {

        if (Param.MUTATE_INT_CONTAINING_TERMS_RATE > 0) {
            if (t.hasAny(INT)) {
                TermContainer ts = t.subterms();
                if (ts.OR(xx -> xx instanceof Int) && rng.nextFloat() <= Param.MUTATE_INT_CONTAINING_TERMS_RATE) {

                    Term[] xx = ts.toArray();
                    boolean changed = false;
                    for (int i = 0; i < xx.length; i++) {
                        Term y = xx[i];
                        if (y instanceof Int) {
                            int shift =
                                    rng.nextInt(3) - 1;
                            //nar.random().nextInt(5) - 2;
                            if (shift != 0) {
                                int yy = ((Int) y).id;
                                int j =
                                        Math.max(0 /* avoid negs for now */, yy + shift);
                                if (yy != j) {
                                    xx[i] = Int.the(j);
                                    changed = true;
                                }
                            }
                        }
                    }
                    if (changed)
                        return t.op().the(t.dt(), xx);

                }
            }

        }

        return t;
    }

    static void activateSubterms(Task task, Collection<Concept> subs, float cPri) {
        int numSubs = subs.size();
        if (numSubs == 0)
            return;

        float tfa = cPri * task.priElseZero();
        float tfaEach = tfa / numSubs;


        for (Concept localSubConcept : subs) {

            localSubConcept.tasklinks().putAsync(
                    new PLinkUntilDeleted(task, tfaEach)
            );
//                localSubConcept.termlinks().putAsync(
//                        new PLink(task.term(), tfaEach)
//                );


        }
    }

//    public void activateTaskExperiment1(NAR nar, float pri, Term thisTerm, BaseConcept cc) {
//        Termed[] taskTemplates = templates(cc, nar);
//
//        //if (templateConceptsCount > 0) {
//
//        //float momentum = 0.5f;
//        float taskTemplateActivation = pri / taskTemplates.length;
//        for (Termed ct : taskTemplates) {
//
//            Concept c = nar.conceptualize(ct);
//            //this concept activates task templates and termlinks to them
//            if (c instanceof Concept) {
//                c.termlinks().putAsync(
//                        new PLink(thisTerm, taskTemplateActivation)
//                );
//                nar.input(new Activate(c, taskTemplateActivation));
//
////                        //reverse termlink from task template to this concept
////                        //maybe this should be allowed for non-concept subterms
////                        id.termlinks().putAsync(new PLink(c, taskTemplateActivation / 2)
////                                //(concept ? (1f - momentum) : 1))
////                        );
//
//            }
//
//
//        }
//    }


    @Override
    public boolean delete() {
        throw new UnsupportedOperationException();
    }

    //    protected int premise(Derivation d, Premise p, Consumer<DerivedTask> x, int ttlPerPremise) {
//        int ttl = p.run(d, ttlPerPremise);
//        //TODO record ttl usage
//        return ttl;
//    }

    @Override
    public final Term term() {
        return id.term();
    }

}
