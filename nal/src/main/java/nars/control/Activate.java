package nars.control;

import jcog.Util;
import jcog.bag.Bag;
import jcog.decide.DecideRoulette;
import jcog.pri.PLink;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.control.premise.Derivation;
import nars.task.DerivedTask;
import nars.task.ITask;
import nars.task.UnaryTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import nars.term.var.Variable;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/**
 * concept firing, activation, etc
 */
public class Activate extends UnaryTask<Concept> implements Termed {


    /**
     * rate at which ConceptFire forms premises and derives
     */
    private static final int maxSamples = 2;

    static final int TASKLINKS_SAMPLED = 4;
    static final int TERMLINKS_SAMPLED = 4;

    private Termed[] localTemplates;


    public Activate(Concept c, float pri) {
        super(c, pri);
        assert (c.isNormalized()) :
                c + " not normalized";
    }


    public static Activate activate(@NotNull Task t, float activation, TaskConcept origin, NAR n) {


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
            TaskConcept cc = t.concept(n, true);
            if (cc != null) {

                n.input(activate(t, activation, cc, n));
//                        a = (BiConsumer<ConceptFire,NAR>) new Activate.ActivateSubterms(t, activation);
//                n.input(a);
            }

            if (process) {
                if (n.exe.concurrent())
                    n.eventTaskProcess.emitAsync(/*post*/t, n.exe);
                else
                    n.eventTaskProcess.emit(t);
            }
        //}
    }

    @Override
    public ITask[] run(NAR nar) {



        nar.emotion.conceptFires.increment();



        //nar.emotion.count("ConceptFire_run_attempt");

//        float priBefore = this.pri;
//        if (priBefore != priBefore) {
//            nar.emotion.count("ConceptFire_run_but_deleted");
//            return null;
//        }
//        if (priBefore < priMinAbsolute) {
//            nar.emotion.count("ConceptFire_run_but_depleted");
//            return null;
//        }
//
//
//        final float minPri = priMinAbsolute;

        final Bag<Task, PriReference<Task>> tasklinks = id.tasklinks().commit();//.normalize(0.1f);
        if (tasklinks.isEmpty()) {
            //nar.emotion.count("ConceptFire_run_but_zero_taskslinks");
            return null;
        }

        DecideRoulette<PriReference<Task>> taskl = new DecideRoulette(DecideRoulette.linearPri);
        tasklinks.sample(TASKLINKS_SAMPLED, ((Consumer<PriReference<Task>>) taskl::add));
        if (taskl.isEmpty()) {
            //nar.emotion.count("ConceptFire_run_but_zero_taskslinks_selected");
            return null;
        }



        //nar.emotion.count("ConceptFire_run");


        @Nullable PriReference<Task> tasklink = null;
        @Nullable PriReference<Term> termlink = null;

        //int samples = 0;
        int premises = 0;
        int derivations = 0;
        float cost = 0;
        //Math.min(this.samplesMax, terml.size() * taskl.size());

//        Map<DerivedTask, DerivedTask> results = buffers.get();
//        Consumer<DerivedTask> x = (nt) -> results.merge(nt, nt, (prev, NT) -> {
////            if (pt == null) {
////                //priSub(nt.priElseZero());
////                return nt;
////            } else {
////                if (tt!=nt)
//                    //pt.merge(nt);
//
//            prev.priMax(NT.priElseZero());
//            return prev;
//                //float ptBefore = pt.priElseZero();
//
//                //float ptAfter = pt.priElseZero();
//                //priSub(ptAfter - ptBefore);
////                return pt;
////            }
//        });


        //float pLimitFactor = priElseZero() * (1f - momentum) / samplesMax;

        int ttlPerPremise = nar.matchTTL.intValue(); // * UnificationTTLMax;
        int ttl = ttlPerPremise * Util.lerp(priElseZero(), 1, maxSamples);


//        if (this.templates == null) {
//            this.templates = templates(id, nar);
//        }

        if (localTemplates==null) {
            localTemplates = templates(id, nar);
        }
        //System.out.println("templates: " + id + " " + Arrays.toString(localTemplates));

        int misfirePenalty = Math.max(1, ttlPerPremise / (2));

        Derivation d = nar.derivation();
        d.cycle(nar.deriver()); //TODO dont do this until the first premise is ready

        DecideRoulette<PriReference<Term>> terml = null;
        //Set<Premise> novel = new HashSet(); //ensures novel premise formation in this fire

        Random rng = nar.random();
        while (ttl > 0 /*samples++ < samplesMax*/) {

            int tasklSelected = taskl.decideWhich(rng);
            tasklink = taskl.get(tasklSelected);

            @Nullable PriReference<Task> tll = tasklink;
            Task task = tll.get();
            if (task == null) {
                ttl -= misfirePenalty;
                continue;
            }


            float tfa = task.priElseZero();
//            if (tfa < Pri.EPSILON) {
//                ttl -= penalty;
//                continue;
//            }

            Term taskTerm = task.term();

            //tasklink activates local subterms and their reverse termlinks to this
            float tfaEach = tfa / localTemplates.length;
            for (Termed localSub : localTemplates) {

                if (localSub instanceof Concept) {
                    Concept localSubConcept = (Concept)localSub;
                    localSubConcept.tasklinks().putAsync(
                            new PLink(task, tfaEach)
                    );
                    localSubConcept.termlinks().putAsync(
                            new PLink(taskTerm, tfaEach)
                    );
                    nar.input(new Activate(localSubConcept, tfaEach));
                }

                id.termlinks().putAsync(
                        new PLink(localSub, tfaEach)
                );

            }

            TaskConcept taskConcept = task.concept(nar, true);
            if (taskConcept == null) {
                ttl -= misfirePenalty;
                continue; //hrm
            }

            //activateTaskExperiment1(nar, pri, thisTerm, taskConcept);

            if (terml == null) {
                final Bag<Term, PriReference<Term>> termlinks = id.termlinks().commit();//.normalize(0.1f);
                terml = new DecideRoulette(DecideRoulette.linearPri);
                termlinks.sample(TERMLINKS_SAMPLED, ((Consumer<PriReference<Term>>) terml::add));
                if (terml.isEmpty()) {
                    break;
                }
            }

            int termlSelected = terml.decideWhich(rng);
            termlink = terml.get(termlSelected);


            Premise p = new Premise(tasklink, termlink);
            onPremise(p, nar);

            ttl -= ttlPerPremise; //fair disbursement


//            if (novel.add(p)) {
//                int ttlUsed = premise(d, p, nar::input, ttlPerPremise); //inline
//                //            if (ttlUsed <= 0) {
//                //                //failure penalty
//                //                tasklinkPri[tasklSelected] *= 0.9f;
//                //                termlinkPri[termlSelected] *= 0.9f;
//                //            }
//
//                //ttl -= max(ttlUsed, penalty); //stingy
//                ttl -= ttlPerPremise; //fair disbursement
//                premises++;
//            } else {
//                ttl -= misfirePenalty;
//            }

        }

//        if (templateConceptsCount > 0) {
//            float eachActivation = (/*priElseZero() **/ totalActivation) / templateConceptsCount;
//            if (eachActivation >= Pri.EPSILON) {
//                float momentum = 0.5f;
//                for (Termed c : templates) {
//
//
//                    //outgoing termlink, tasklink, and activation
//                    boolean concept = c instanceof Concept;
//                    if (concept) {
//                        Concept cc = (Concept) c;
//                        cc.termlinks().putAsync(new PLink(thisTerm, eachActivation * momentum));
//                        nar.input(new ConceptFire(cc, eachActivation));
//                    }
//
//                    //incoming termlink
//                    id.termlinks().putAsync(new PLink(c, eachActivation * (1f - momentum))
//                            //(concept ? (1f - momentum) : 1))
//                    );
//                }
//                //priSub(totalActivation);
//            }
//        }

        nar.terms.commit(id); //index cache update

//        derivations = results.size();

        //float priAfter = priElseZero();
        //cost = priBefore - priAfter;
        //System.out.println(this + " " + samples + "," + premises + "," + derivations + "," + cost);

//        if (derivations > 0) {
//            ITask[] a = results.values().toArray(new ITask[derivations]);
//            results.clear();
//            return a;
//        } else
        return null;


//        int num = premises.size();
//        if (num > 0) {
//            ITask[] pp = premises.array();
//            float spend = this.pri * spendRate;
//            this.pri -= spend;
//
//            //divide priority among the premises
//            float subPri = spend / num;
//            for (int i = 0; i < num; i++) {
//                pp[i].setPri(subPri);
//            }
//
//            return pp;
//        } else {
//            return null;
//        }
    }

    public void activateTaskExperiment1(NAR nar, float pri, Term thisTerm, TaskConcept taskConcept) {
        Termed[] taskTemplates = templates(taskConcept, nar);

        //if (templateConceptsCount > 0) {

        //float momentum = 0.5f;
        float taskTemplateActivation = pri / taskTemplates.length;
        for (Termed ct : taskTemplates) {

            Concept c = nar.conceptualize(ct);
            //this concept activates task templates and termlinks to them
            if (c instanceof Concept) {
                c.termlinks().putAsync(
                        new PLink(thisTerm, taskTemplateActivation)
                );
                nar.input(new Activate(c, taskTemplateActivation));

//                        //reverse termlink from task template to this concept
//                        //maybe this should be allowed for non-concept subterms
//                        id.termlinks().putAsync(new PLink(c, taskTemplateActivation / 2)
//                                //(concept ? (1f - momentum) : 1))
//                        );

            }


        }
    }

    public Termed[] templates(@NotNull Concept id, NAR nar) {
        TermContainer ctpl = id.templates();

//        if (ctpl == null || ctpl.size()==0) {
//            Term sampledTermLink = id.termlinks().sample().get();
//            if (sampledTermLink!=null)
//                ctpl = TermVector.the(sampledTermLink);
//        }

        if (ctpl != null) {
            Set<Termed> tc =
                    //new UnifiedSet<>(id.volume() /* estimate */);
                    new HashSet(id.volume());
            templates(tc, ctpl, nar, layers(id)-1);
            if (!tc.isEmpty())
                return tc.toArray(new Termed[tc.size()]);
        }

        //id.termlinks().sample(2, (PriReference<Term> x) -> templatize.accept(x.get()));

//                templateConcepts = Concept.EmptyArray;
//                templateConceptsCount = 0;
        return Termed.EmptyArray;

    }

    public void templates(Set<Termed> tc, TermContainer ctpl, NAR nar, int layersRemain) {

        int cs = ctpl.size();
        for (int i = 0; i < cs; i++) {
            Term b = ctpl.sub(i);
            @Nullable Concept c = (!(b instanceof Variable /* quick var prefilter */)) ?
                    nar.conceptualize(b) : null;
            TermContainer e = null;
            if (c != null) {
                if (/*!c.equals(id) && */tc.add(c)) {
                    if (layersRemain > 0 && c instanceof Compound) {
                        e = c.templates();
                    }
                }
            } else /*if (!b.equals(id))*/ {
                Term d = b.unneg();
                //variable or other non-concept term
                if (tc.add(d)) {
                    if (layersRemain > 0 && d instanceof Compound) {
                        e = ((Compound) d).subterms();
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

    @NotNull
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
                return 2;

            case IMPL:
                return 2;

            case CONJ:
                return 2;

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

    protected void onPremise(Premise p, NAR nar) {
        nar.input(p);
    }
}
