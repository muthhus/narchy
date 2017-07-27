package nars.control;

import jcog.bag.Bag;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.task.ITask;
import nars.task.UnaryTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * concept firing, activation, etc
 */
public class Activate extends UnaryTask<Concept> implements Termed {

    static final int TASKLINKS_SAMPLED = 2;
    static final int TERMLINKS_SAMPLED = 3;


    public Activate(@NotNull Concept c, float pri) {
        super(c, pri);
        assert (c.isNormalized()) :
                c + " not normalized";
    }


    public static Activate activate(@NotNull Task t, float activation, Concept origin, NAR n) {


//        if (activation < EPSILON) {
//            return null;
//        }

        short[] x = t.cause();
        int xl = x.length;
        if (xl > 0) {
            float taskValue = origin.valueIfProcessed(t, activation, n);
            n.value(x, taskValue);
        }

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

        n.emotion.conceptActivations.increment();
        return new Activate(origin, activation); /*, () -> {

            }*/
//            }

    }

    public static void activate(@NotNull Task t, float activation, @NotNull NAR n) {
        activate(t, activation, n, true);
    }

    public static void activate(@NotNull Task t, float activation, @NotNull NAR n, boolean process) {
        // if (Util.equals(activation, t.priElseZero(), Pri.EPSILON))  //suppress emitting re-activations
        //if (activation >= EPSILON) {
        Concept cc = t.concept(n, true);
        if (cc != null) {

            n.input(activate(t, activation, cc, n));
//                        a = (BiConsumer<ConceptFire,NAR>) new Activate.ActivateSubterms(t, activation);
//                n.input(a);
        }

        if (process) {
//            if (n.exe.concurrent())
//                n.eventTaskProcess.emitAsync(/*post*/t, n.exe);
//            else
                n.eventTaskProcess.emit(t);
        }
        //}
    }

    @Override
    public ITask[] run(NAR nar) {

        nar.emotion.conceptFires.increment();

        final Bag<Task, PriReference<Task>> tasklinks = id.tasklinks().commit();//.normalize(0.1f);
        if (tasklinks.isEmpty()) {
            //nar.emotion.count("ConceptFire_run_but_zero_taskslinks");
            return null;
        }

        int talSampled = Math.min(tasklinks.size(), TASKLINKS_SAMPLED);
        List<PriReference<Task>> taskl = $.newArrayList(talSampled);
        tasklinks.sample(talSampled, ((Consumer<PriReference<Task>>) taskl::add));
        if (taskl.isEmpty()) {
            //nar.emotion.count("ConceptFire_run_but_zero_taskslinks_selected");
            return null;
        }


        Termed[] localTemplates = templates(id, nar);
        int localTemplateConcepts = 0;
        for (Termed t : localTemplates)
            if (t instanceof Concept)
                localTemplateConcepts++;


        //concept priority transfers into:
        //      its termlinks to subterms
        //      and their reverse links back to this

        float activationFactor = 1f; //priElseZero();

        if (localTemplates.length > 0) {
            float decayRate = 0.5f;
            float decayed = priElseZero() * (1f - decayRate);
            priSub(decayed);
            float subDecay = decayed / localTemplates.length;
            float balance = Param.TERMLINK_BALANCE;
            float subDecayForward = subDecay * balance;
            float subDecayReverse = subDecay * (1f - balance);
            Term thisTerm = id.term();
            for (Termed localSub : localTemplates) {
                float d;
                if (localSub instanceof Concept) {

                    Concept localSubConcept = (Concept) localSub;

                    nar.input(new Activate(localSubConcept, activationFactor * subDecay));

                    localSubConcept.termlinks().putAsync(
                            new PLink(thisTerm, subDecayReverse)
                    );

                    d = subDecayForward;

                } else {
                    d = subDecay; //full subdecay to the non-concept subterm
                }

                id.termlinks().putAsync(
                        new PLink(localSub.term(), d)
                );
            }
        }


        List<PriReference<Term>> terml;
        final Bag<Term, PriReference<Term>> termlinks = id.termlinks().commit();//.normalize(0.1f);
        if (termlinks.isEmpty()) {
            terml = Collections.emptyList();
        } else {
            int tlSampled = Math.min(termlinks.size(), TERMLINKS_SAMPLED);
            terml = $.newArrayList(tlSampled);
            termlinks.sample(tlSampled, ((Consumer<PriReference<Term>>) terml::add));
        }

        for (int i = 0, tasklSize = taskl.size(); i < tasklSize; i++) {
            PriReference<Task> tasklink = taskl.get(i);

            if (localTemplateConcepts > 0) {
                activateSubterms(tasklink, localTemplates, localTemplateConcepts);
            }

            for (int j = 0, termlSize = terml.size(); j < termlSize; j++) {
                PriReference<Term> termlink = terml.get(j);

                float pri = Param.tasktermLinkCombine.apply(tasklink.priElseZero(), termlink.priElseZero());
                premise( new Premise(tasklink.get(), termlink.get(), pri), nar);
            }
        }

        nar.terms.commit(id); //index cache update

        return null;

    }

    public void activateSubterms(PriReference<Task> tasklink, Termed[] localTemplates, int localTemplateConcepts) {
        Task task = tasklink.get();
        if (task == null)
            return;

        float tfa = tasklink.priElseZero();
        //Term taskTerm = task.term();

        //tasklink activates local subterms and their reverse termlinks to this
        float tfaEach = tfa / localTemplateConcepts;

        for (Termed localSub : localTemplates) {

            if (localSub instanceof Concept) {
                Concept localSubConcept = (Concept) localSub;
                localSubConcept.tasklinks().putAsync(
                        new PLink<>(task, tfaEach)
                );
//                localSubConcept.termlinks().putAsync(
//                        new PLink(task.term(), tfaEach)
//                );
            }


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

    public static Termed[] templates(@NotNull Concept id, NAR nar) {
        TermContainer ctpl = id.templates();

//        if (ctpl == null || ctpl.size()==0) {
//            Term sampledTermLink = id.termlinks().sample().get();
//            if (sampledTermLink!=null)
//                ctpl = TermVector.the(sampledTermLink);
//        }

        Set<Termed> tc =
                //new UnifiedSet<>(id.volume() /* estimate */);
                new HashSet(id.volume());
        templates(tc, ctpl, nar, layers(id) - 1);

        tc.add(id.term()); //add the local term but not the concept. this prevents reinserting a tasklink

        if (!tc.isEmpty())
            return tc.toArray(new Termed[tc.size()]);

        //id.termlinks().sample(2, (PriReference<Term> x) -> templatize.accept(x.get()));

//                templateConcepts = Concept.EmptyArray;
//                templateConceptsCount = 0;
        return Termed.EmptyArray;

    }

    public static void templates(Set<Termed> tc, TermContainer ctpl, NAR nar, int layersRemain) {

        int cs = ctpl.size();
        for (int i = 0; i < cs; i++) {
            Term b = ctpl.sub(i);

            @Nullable Concept c =
                    //b instanceof Concept ? ((Concept) b) :
                            (b = b.unneg()).op().conceptualizable ?
                                    nar.conceptualize(b) : null;

            TermContainer e = null;
            if (c != null) {
                if (/*!c.equals(id) && */tc.add(c)) {
                    if (layersRemain > 0) {
                        e = c.templates();
                    }
                }
            } else /*if (!b.equals(id))*/ {

                    if (tc.add(b)) { //variable or other non-conceptualizable term
                        if (layersRemain > 0) {
                            e = b.subterms();
                        }
                    }

            }

            if (e != null)
                templates(tc, e, nar, layersRemain - 1);
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

    static int layers(@NotNull Termed host) {
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

            case EQUI:
                return 3;

            case IMPL:
                return 3;

            case CONJ:
                return 3;

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

    protected void premise(Premise p, NAR nar) {
        nar.input(p);
    }
}
