package nars.control;

import jcog.Util;
import jcog.bag.Bag;
import jcog.list.FasterList;
import jcog.pri.PLink;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TermLinks;
import nars.term.Term;
import nars.term.Termed;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * concept firing, activation, etc
 */
public class Activate extends PLink<Concept> implements Termed {

//    /** lazily computed */
//    private List<Termed> templates;
//
//    /** lazily computed */
//    private Concept[] templateConcepts;

    public Activate(Concept c, float pri) {
        super(c, pri);
    }

    public Iterable<Premise> hypothesize(NAR nar, BatchActivation ba, int premisesMax) {

        assert(premisesMax > 0);

        nar.emotion.conceptFires.increment();

//        if (templates == null) {
//            synchronized(id) {
//                if (templates == null) {
//                    this.templates = TermLinks.templates(id, nar);
//                    this.templateConcepts = TermLinks.templateConcepts(templates);
//                }
//            }
//        }

        float cost = TermLinks.linkTemplates(id, id.templates(), priElseZero(), nar.momentum.floatValue(), nar, ba);
        if (cost >= Pri.EPSILON)
            priSub(cost);

        List<Premise> next = new FasterList(premisesMax);

        final Bag<Term, PriReference<Term>> termlinks = id.termlinks();

        termlinks.commit(termlinks.forget(Param.LINK_FORGET_TEMPERATURE));
        int ntermlinks = termlinks.size();
        if (ntermlinks == 0)
            return null;

        //TODO add a termlink vs. tasklink balance parameter
        int TERMLINKS_SAMPLED = (int) Math.ceil((float)Math.sqrt(premisesMax));

        int tlSampled = Math.min(ntermlinks, TERMLINKS_SAMPLED);
        FasterList<PriReference<Term>> terml = new FasterList(tlSampled);
        termlinks.sample(tlSampled, ((Consumer<PriReference>) terml::add));
        int termlSize = terml.size();
        if (termlSize <= 0) return null;


        final Bag<Task, PriReference<Task>> tasklinks = id.tasklinks();
        tasklinks.commit(tasklinks.forget(Param.LINK_FORGET_TEMPERATURE));
        int ntasklinks = tasklinks.size();
        if (ntasklinks == 0) return null;

        int TASKLINKS_SAMPLED = (int) Math.ceil(((float)premisesMax) / termlSize);

        final int[] remaining = {premisesMax};

        Random rng = nar.random();
        tasklinks.sample(Math.min(ntasklinks, TASKLINKS_SAMPLED), (tasklink) -> {

            final Task task = tasklink.get();
            if (task == null)
                return;

            float tPri = task.priElseZero();
            for (int j = 0; j < termlSize && remaining[0]-- > 0; j++) {
                PriReference<Term> termlink = terml.get(j);

                final Term term = termlink.get();
                if (term != null) {

                    float pri1 = Param.termTaskLinkToPremise.apply(tPri, termlink.priElseZero());

                    Premise p = new Premise(task, term, pri1,

                            //targets:
                            randomTemplateConcepts(rng, TERMLINKS_SAMPLED /* heuristic */, nar)

                    );

                    next.add(p);
                }
            }
        });

        return next;
    }


    private List<Concept> randomTemplateConcepts(Random rng, int count, NAR nar) {

//            {
//                //this allows the tasklink, if activated to be inserted to termlinks of this concept
//                //this is messy, it propagates the tasklink further than if the 'callback' were to local templates
//                List<Concept> tlConcepts = terml.stream().map(t ->
//                        //TODO exclude self link to same concept, ie. task.concept().term
//                        nar.concept(t.get())
//                ).filter(Objects::nonNull).collect(toList());
//            }
        {
            //Util.selectRoulette(templateConcepts.length, )

        }


        List<Termed> tt = id.templates();
        List<Concept> uu = $.newArrayList(count);
        Util.selectRouletteUnique(count, rng, tt.size(), (w) -> {
            Term t = tt.get(w).term();
            return t.op().conceptualizable && !t.equals(id.term()) ? 1f : 0f;
            //TODO try biasing toward larger template components so the activation trickles down to atoms with less probabilty
        }, z -> {
            Concept cc = nar.conceptualize(tt.get(z));
            if (cc==null)
                return true;
            uu.add(cc);
            return (uu.size() < count);
        });
        return uu;
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
    public Term term() {
        return id.term();
    }
}
