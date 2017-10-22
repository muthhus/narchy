package nars.concept;

import jcog.list.FasterList;
import jcog.pri.PLink;
import jcog.pri.PLinkUntilDeleted;
import jcog.pri.Pri;
import jcog.pri.Prioritized;
import nars.*;
import nars.control.Activate;
import nars.control.BatchActivate;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Int;
import nars.term.container.TermContainer;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static java.util.Collections.emptyList;
import static nars.Op.INT;

public enum TermLinks {
    ;


    public static Collection<Termed> templates(Term term) {

        if (term.subs() > 0) {

            Collection<Termed> templates;

            Set<Termed> tc =
                    //new UnifiedSet<>(id.volume() /* estimate */);
                    new HashSet<>(term.volume());

            TermLinks.templates(term, tc, layers(term));

            int tcs = tc.size();

            if (tcs > 0)
                return new FasterList<>(tc.toArray(new Termed[tcs])); //store as list for compactness and fast iteration
            else
                return emptyList();
        } else {

            return List.of(term);
        }
    }

    /** recurses */
    static void templates(Term root, Set<Termed> tc, int layersRemain) {

        Term b = root.unneg();

        Op o = b.op();
        switch (o) {
            case VAR_QUERY:
            case VAR_DEP:
            case VAR_INDEP:
                return; //NO
                //break; //YES

        }

        if (!tc.add(b))
            return; //already added

        if ((--layersRemain <= 0) || !b.op().conceptualizable)
            return;

        int bs = b.subs();
        if (bs == 0)
            return;

        TermContainer bb = b.subterms();
        for (int i = 0; i < bs; i++) {
            templates(bb.sub(i), tc, layersRemain);
        }
    }

    /** includes the host as layer 0, so if this returns 1 it will only include the host */
    static int layers(Term host) {
        switch (host.op()) {


            case SETe:
            case SETi:
                return 1;

            case PROD:
                return 2;

            case DIFFe:
            case DIFFi:
            case SECTi:
            case SECTe:


                return 2;

            case CONJ:
                return 3;

            case SIM:
                return 3;

            case INH:
                return 4;

            case IMPL:
//                if (host.hasAny(Op.CONJ))
                    return 4;
//                else
//                    return 3;

            default:
                throw new UnsupportedOperationException("unhandled operator type: " + host.op());

        }
    }

    public static void linkTask(Task t, float activationApplied, NAR n, Concept cc) {

        n.emotion.onActivate(t, activationApplied, cc, n);

        float evalAmp = n.evaluate(t.cause());

        activationApplied *= evalAmp;

        cc.tasklinks().putAsync(
                new PLinkUntilDeleted<>(t, activationApplied)
                //new PLink<>(t, activation)
        );

        n.input(new Activate(cc, activationApplied));

        if (activationApplied >= Prioritized.EPSILON_VISIBLE) {
            n.eventTask.emit(t);
        }
    }

    public static void linkTemplate(Concept src, Termed target, float priForward, float priReverse, BatchActivate a, NAR nar, MutableFloat refund) {

        float priSum = priForward + priReverse;
        if (target instanceof Concept) {
            Concept c = (Concept) target;
            c.termlinks().put(
                    new PLink(src.term(), priReverse), refund
            );
            a.put(c, priSum);
        } else {
            refund.add(priReverse);
        }

        src.termlinks().put(
                new PLink(target.term(), priForward), refund
        );

    }

    @Nullable
    public static List<Termed> templates(Concept id, NAR nar) {

        Collection<Termed> localTemplates = id.templates();
        int n = localTemplates.size();
        if (n <= 0)
            return null;

        Term thisTerm = id.term();

        List<Termed> localSubConcepts =
                //new HashSet<>(); //temporary for this function call only, so as not to retain refs to Concepts
                //new UnifiedSet(); //allows concurrent read
                $.newArrayList(n); //maybe contain duplicates but its ok, this is fast to construct

        //Random rng = nar.random();
        //float balance = Param.TERMLINK_BALANCE;

        float spent = 0;
        for (Termed localSub : localTemplates) {

            //localSub = mutateTermlink(localSub.term(), rng); //for special Termed instances, ex: RotatedInt etc
//                if (localSub instanceof Bool)
//                    continue; //unlucky mutation


            Termed target = localSub.term(); //if mutated then localSubTerm would change so do it here

            float d;

            if (target.op().conceptualizable && !target.equals(thisTerm)) {

                Concept targetConcept = nar.conceptualize(localSub);
                if (targetConcept != null) {
                    target = (targetConcept);

                }
            }

            localSubConcepts.add(target);
        }

        return !localSubConcepts.isEmpty() ? localSubConcepts : null;

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

    public static Concept[] templateConcepts(List<Termed> templates) {
        if (templates.isEmpty())
            return Concept.EmptyArray;

        FasterList<Concept> templateConcepts = new FasterList(0, new Concept[templates.size()]);
        for (int i = 0, templatesSize = templates.size(); i < templatesSize; i++) {
            Termed x = templates.get(i);
            if (x instanceof Concept)
                templateConcepts.add((Concept) x);
        }
        return templateConcepts.toArrayRecycled(Concept[]::new);
    }

    /** send some activation, returns the cost */
    public static float linkTemplates(Concept src, List<Termed> templates, float totalBudget, float momentum, NAR nar) {

        int n = templates.size();
        if (n == 0)
            return 0;

        float freed = 1f - momentum;
        int toFire = (int) Math.ceil(n * freed);
        float budgeted = totalBudget * freed;

        float budgetedToEach = budgeted / toFire;
        if (budgetedToEach < Pri.EPSILON)
            return 0;

        MutableFloat refund = new MutableFloat(0);
        BatchActivate ba = BatchActivate.get();

        int nextTarget = nar.random().nextInt(n);
        for (int i = 0; i < toFire; i++) {

            Termed t = templates.get(nextTarget++);
            if (nextTarget == n) nextTarget = 0; //wrap around

            linkTemplate(src, t, budgetedToEach/2f, budgetedToEach/2f, ba, nar, refund);
        }

        float r = refund.floatValue();
        float cost = budgeted - r;
        return cost;
    }

    public static void linkTask(Task task, Collection<Concept> subs, float cPri) {
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
}
