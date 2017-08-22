package nars.control;

import jcog.bag.Bag;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.task.UnaryTask;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static nars.Op.VAR_PATTERN;
import static nars.Op.VAR_QUERY;

/**
 * concept firing, activation, etc
 */
public class Activate extends UnaryTask<Concept> implements Termed {

    static final int TASKLINKS_SAMPLED = 1;
    static final int TERMLINKS_SAMPLED = 2;
    //private final BiFunction<Task,Term,Premise> premiseBuilder ;


    public Activate(@NotNull Concept c, float pri) {
        super(c, pri);
        assert (c.isNormalized()) :
                c + " not normalized";
    }


    public static void activate(@NotNull Task t, float activationApplied, @NotNull NAR n) {
        if (n.exe.concurrent()) {
            n.exe.execute(() -> activate(t, activationApplied, n, true));
        } else {
            activate(t, activationApplied, n, true);
        }
    }

    static void activate(@NotNull Task t, float activationApplied, @NotNull NAR n, boolean process) {
        // if (Util.equals(activation, t.priElseZero(), Pri.EPSILON))  //suppress emitting re-activations
        //if (activation >= EPSILON) {
        Concept cc = t.concept(n, true);
        if (cc != null) {

            n.input(activate(t, activationApplied, cc, n));
//                        a = (BiConsumer<ConceptFire,NAR>) new Activate.ActivateSubterms(t, activation);
//                n.input(a);
        }

        if (process) {
//            if (n.exe.concurrent())
//                n.eventTaskProcess.emitAsync(/*post*/t, n.exe);
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
                //new PLinkUntilDeleted<>(t, activation)
                new PLink<>(t, activation)
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

    public List<Premise> run(NAR nar) {
        nar.emotion.conceptFires.increment();
        nar.terms.commit(id); //index cache update

        final Bag<Task, PriReference<Task>> tasklinks = id.tasklinks().commit();//.normalize(0.1f);
        if (tasklinks.isEmpty()) {
            //nar.emotion.count("ConceptFire_run_but_zero_taskslinks");
            return emptyList();
        }

        int talSampled = Math.min(tasklinks.size(), TASKLINKS_SAMPLED);
        List<PriReference<Task>> taskl = $.newArrayList(talSampled);
        tasklinks.sample(talSampled, ((Consumer<PriReference<Task>>) taskl::add));
        if (taskl.isEmpty()) {
            //nar.emotion.count("ConceptFire_run_but_zero_taskslinks_selected");
            return emptyList();
        }




        //concept priority transfers into:
        //      its termlinks to subterms
        //      and their reverse links back to this

        float cPri = priElseZero();
        float decayRate = 1f - nar.momentum.floatValue();
        float decayed = cPri * decayRate;
        priSub(decayed); //for balanced budgeting: important

        Collection<Termed> localTemplates = id.templates(nar);
        float subDecay = decayed / localTemplates.size();
        float balance = Param.TERMLINK_BALANCE;
        float subDecayForward = subDecay * balance;
        float subDecayReverse = subDecay * (1f - balance);
        Term thisTerm = id.term();
        float activationFactor = 1f; //priElseZero();

        List<Concept> localSubConcepts = $.newArrayList(); //temporary for this function call only, so as not to retain refs to Concepts

        for (Termed localSub : localTemplates) {
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

            id.termlinks().putAsync(
                    new PLink(localSub.term(), d)
            );
        }


        List<PriReference<Term>> terml;
        final Bag<Term, PriReference<Term>> termlinks = id.termlinks().commit();//.normalize(0.1f);
        if (termlinks.isEmpty()) {
            return emptyList();
        } else {
            int tlSampled = Math.min(termlinks.size(), TERMLINKS_SAMPLED);
            terml = $.newArrayList(tlSampled);
            termlinks.sample(tlSampled, ((Consumer<PriReference<Term>>) terml::add));
        }

        int tasklSize = taskl.size();  if (tasklSize == 0) return emptyList();
        int termlSize = terml.size();  if (termlSize == 0) return emptyList();

        List<Premise> premises = $.newArrayList(tasklSize * termlSize);

        for (int i = 0; i < tasklSize; i++) {
            PriReference<Task> tasklink = taskl.get(i);


            activateSubterms(tasklink, localSubConcepts,
                    1f
                        /*decayed*/);


            for (int j = 0; j < termlSize; j++) {
                PriReference<Term> termlink = terml.get(j);

                float pri = Param.tasktermLinkCombine.apply(tasklink.priElseZero(), termlink.priElseZero());
                premises.add(nar.premise(tasklink.get(), termlink.get(), pri));
            }
        }


        return premises;
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
                    new PLink<>(task, tfaEach)
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

    public static void templates(Set<Termed> tc, Iterable<? extends Termed> next, NAR nar, int layersRemain) {

        for (Termed bb : next) {

            Term b = bb.unneg();

            if (!tc.add(b))
                continue; //already added

            if (!b.op().conceptualizable || b.hasAny(VAR_QUERY.bit | VAR_PATTERN.bit))
                continue;

            if (layersRemain > 0)
                templates(tc, b.subterms(), nar, layersRemain-1);

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


            case SETe:
            case SETi:

//            case IMGe:
//            case IMGi:
//                return 1;

            case DIFFe:
            case DIFFi:
            case SECTi:
            case SECTe:
                return 1;

            case SIM:
                return 2;

            case INH:
                return 3;

            case IMPL:
                return 3;

            case CONJ:
                return 1;

//                int s = host.size();
//                if (s <= Param.MAX_CONJ_SIZE_FOR_LAYER2_TEMPLATES) {
//                    int vars = host.vars();
//                    return (vars > 0) ? 3 : 2;
//                } else {
//                    return 2;
//                    //return (vars > 0) ? 2 : 1; //prevent long conjunctions from creating excessive templates
//                }


            default:
                return 1;
            //throw new UnsupportedOperationException("unhandled operator type: " + host.op());
        }
    }

}
