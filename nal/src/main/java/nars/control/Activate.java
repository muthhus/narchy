package nars.control;

import jcog.bag.Bag;
import jcog.pri.PLink;
import jcog.pri.PLinkUntilDeleted;
import jcog.pri.PriReference;
import nars.*;
import nars.concept.Concept;
import nars.task.UnaryTask;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Int;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

import static nars.Op.INT;

/**
 * concept firing, activation, etc
 */
public class Activate extends UnaryTask<Concept> implements Termed {

    static final int TASKLINKS_SAMPLED = 2;
    static final int TERMLINKS_SAMPLED = 3;
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
        nar.terms.commit(id); //index cache update

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


        Collection<Termed> localTemplates = id.templates(nar);


        List<Concept> localSubConcepts;
        if (!localTemplates.isEmpty()) {
            float subDecay = decayed / localTemplates.size();
            float balance = Param.TERMLINK_BALANCE;
            Term thisTerm = id.term();

            localSubConcepts = $.newArrayList(); //temporary for this function call only, so as not to retain refs to Concepts

            Random rng = nar.random();
            float activationFactor = 1f; //priElseZero();
            float subDecayReverse = subDecay * (1f - balance);
            float subDecayForward = subDecay * balance;
            for (Termed localSub : localTemplates) {

                localSub = local(localSub, rng); //for special Termed instances, ex: RotatedInt etc

                float d;
                if (localSub.op().conceptualizable) {

                    Concept localSubConcept = nar.conceptualize(localSub);
                    if (localSubConcept != null) {
                        localSubConcept.activate(activationFactor * subDecay, nar);

                        localSubConcept.termlinks().putAsync(
                                new PLink(thisTerm, subDecayReverse)
                        );
                        localSubConcepts.add(localSubConcept);
                    }

                    d = subDecayForward;

                } else {
                    d = subDecay; //full subdecay to the non-concept subterm
                }

                termlinks.putAsync(
                        new PLink(localSub.term(), d)
                );
            }
        } else {
            localSubConcepts = Collections.emptyList();
        }


        List<PriReference<Term>> terml;
        if (termlinks.isEmpty()) {
            return null;
        } else {
            int tlSampled = Math.min(termlinks.size(), TERMLINKS_SAMPLED);
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


            activateSubterms(tasklink, localSubConcepts,
                    1f
                    /*decayed*/);


            for (int j = 0; j < termlSize; j++) {
                PriReference<Term> termlink = terml.get(j);

                float pri = Param.tasktermLinkCombine.apply(tasklink.priElseZero(), termlink.priElseZero());
                premises.add(new Premise(tasklink.get(), termlink.get(), pri));
            }
        }


        return premises;
    }

    /**
     * preprocess termlink
     */
    private static Termed local(Termed x, Random rng) {

        if (Param.MUTATE_INT_CONTAINING_TERMS_RATE > 0) {
            Term t = x.term();
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

        return x;
    }

    static void activateSubterms(PriReference<Task> tasklink, List<Concept> subs, float cPri) {
        if (subs.isEmpty())
            return;

        Task task = tasklink.get();
        if (task == null)
            return;

        float tfa = cPri * tasklink.priElseZero();
        float tfaEach = tfa / subs.size();


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

    public static void templates(Set<Termed> tc, Term root, NAR nar, int layersRemain) {

        Term b = root.unneg();

        Op o = b.op();
        switch (o) {
            //case VAR_DEP:
            //case VAR_INDEP:
            //case VAR_QUERY:
            //  break; //OK
            default:
                if (!o.conceptualizable)
                    return;
        }

        if (!tc.add(b))
            return; //already added

        if (b.size() == 0)
            return;


        if (--layersRemain <= 0) // || !b.op().conceptualizable || b.isAny(VAR_QUERY.bit | VAR_PATTERN.bit))
            return;

        int lb = layers(b);
        layersRemain = Math.min(lb, layersRemain);

        for (Term bb : b.subterms()) {

            templates(tc, bb, nar, layersRemain);

//            @Nullable Concept c = nar.conceptualize(b);
//
//            Iterable<? extends Termed> e = null;
//            if (c != null) {
////                    if (layersRemain > 0) {
//                e = c.subterms();
////                        if (e.size() == 0) {
////                            //System.out.println(c);
////                            //HACK TODO determine if good
////                            //c.termlinks().sample(ctpl.size(), (Consumer<PriReference<Term>>)(x->tc.add(x.get())));
////                        }
////                    }
//            } else /*if (!b.equals(id))*/ {
//
////                    if (layersRemain > 0) {
//                e = b.subterms();
////                        if (e.size() == 0) {
////                            //System.out.println(" ? " + e);
////                            //e.termlinks().sample(10, (Consumer<PriReference<Term>>)(x->tc.add(x.get())));
////                        }
////                    }
//
//
//            }

        }
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

    public static int layers(@NotNull Termed host) {
        switch (host.op()) {

            case PROD:
                return 2;

            case SETe:
            case SETi:

//            case IMGe:
//            case IMGi:
//                return 1;

            case DIFFe:
            case DIFFi:
            case SECTi:
            case SECTe:
                return 2;

            case CONJ:
                return 2;

            case SIM:
                return 3;

            case INH:
                return 3;

            case IMPL:
                return 4;


//                int s = host.size();
//                if (s <= Param.MAX_CONJ_SIZE_FOR_LAYER2_TEMPLATES) {
//                    int vars = host.vars();
//                    return (vars > 0) ? 3 : 2;
//                } else {
//                    return 2;
//                    //return (vars > 0) ? 2 : 1; //prevent long conjunctions from creating excessive templates
//                }


            default:
                throw new UnsupportedOperationException("unhandled operator type: " + host.op());


        }
    }

}
