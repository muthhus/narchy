package nars.nal.meta.op;

import com.google.common.base.Joiner;
import nars.$;
import nars.Global;
import nars.Memory;
import nars.Premise;
import nars.bag.BLink;
import nars.budget.Budget;
import nars.concept.ConceptProcess;
import nars.nal.Tense;
import nars.nal.meta.*;
import nars.nal.meta.match.EllipsisMatch;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.transform.CompoundTransform;
import nars.term.variable.Variable;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.nal.Tense.*;
import static nars.truth.TruthFunctions.eternalize;

/**
 * Handles matched derivation results
 * < (&&, postMatch1, postMatch2) ==> derive(term) >
 */
public class Derive extends AbstractLiteral implements ProcTerm<PremiseMatch> {

    @NotNull
    private final String id;

    private final boolean anticipate;
    private final boolean eternalize;
    private final PremiseRule rule;

    /** result pattern */
    private final Term term;

    @NotNull private final BooleanCondition<PremiseMatch> postMatch; //TODO use AND condition


    public Derive(PremiseRule rule, Term term, @NotNull BooleanCondition[] postMatch, boolean anticipate, boolean eternalize) {
        this.rule = rule;
        this.postMatch = (postMatch.length > 0) ? new AndCondition(postMatch) : BooleanCondition.TRUE;
        this.term = term;
        this.anticipate = anticipate;
        this.eternalize = eternalize;

        String i = "Derive:(" + term;
        if (eternalize && anticipate) {
            i += ", {eternalize,anticipate}";
        } else if (eternalize) {
            i += ", {eternalize}";
        } else if (anticipate) {
            i += ", {anticipate}";
        }


        if (postMatch.length > 0) {
            i += ", {" + Joiner.on(',').join(postMatch) + '}';
        }

        i += ")";
        this.id = i;
    }


    @NotNull
    @Override
    public String toString() {
        return id;
    }


    /** main entry point for derivation result handler.
     * @return true to allow the matcher to continue matching,
     * false to stop it */
    @Override public final void accept(@NotNull PremiseMatch m) {

        Term derivedTerm = m.apply(term);

        if (derivedTerm == null)
            return;

        if ((derivedTerm instanceof EllipsisMatch)) {
            throw new RuntimeException("invalid ellipsis match: " + derivedTerm);
//            EllipsisMatch em = ((EllipsisMatch)derivedTerm);
//            if (em.size()!=1) {
//                throw new RuntimeException("invalid ellipsis match: " + em);
//            }
//            derivedTerm = em.term(0); //unwrap the item
        }

        if (ensureValidVolume(derivedTerm) && postMatch.booleanValueOf(m))
            derive(m, derivedTerm);


    }

    private static boolean ensureValidVolume(Term derivedTerm) {

        //HARD VOLUME LIMIT
        boolean tooLarge = derivedTerm.volume() > Global.COMPOUND_VOLUME_MAX;
        if (tooLarge) {

            if (Global.DEBUG) {
                //$.logger.error("Term volume overflow");
                /*c.forEach(x -> {
                    Terms.printRecursive(x, (String line) ->$.logger.error(line) );
                });*/

                String message = "Term volume overflow: " + derivedTerm;
                $.logger.error(message);
                System.exit(1);
                //throw new RuntimeException(message);
            }

            return false;

        }

        return true;

    }


    /** part 1 */
    private void derive(@NotNull PremiseMatch p, @Nullable Term t) {

        if ((t == null) || Variable.hasPatternVariable(t)) {
            return;
        }

        ConceptProcess premise = p.premise;
        Memory mem = premise.memory();

        //get the normalized term to determine the budget (via it's complexity)
        //this way we can determine if the budget is insufficient
        //before conceptualizating in mem.taskConcept
        Termed tNorm = mem.index.normalized(t);

        //HACK why?
        if ((tNorm == null) || !tNorm.term().isCompound())
            return;

        Truth truth = p.truth.get();

        Budget budget = p.getBudget(truth, tNorm);
        if (budget == null)
            return;

        boolean p7 = premise.nal(7);

        long now = premise.time();
        long occ = p7 ? premise.occurrenceTarget() : ETERNAL;

        Compound ct = (Compound) tNorm.term();

        if (p7) {
            this.premise = premise; //HACK
            ct = premise.nar.memory.index.transformRoot(ct, temporalize);
            this.premise = null; //HACK
        }

        derive(p, ct, truth, budget, now, occ);

    }

    /** HACK */
    transient private Premise premise;

    private final CompoundTransform temporalize = new CompoundTransform<Compound,Compound>() {

        @Override
        public boolean test(Term ct) {
            //N/A probably
            return ct.isCompound();
            //return true; //ct.op().isTemporal();
        }

        @Override
        public boolean testSuperTerm(Compound ct) {
            //N/A probably
            return ct.op().isTemporal();
        }

        final static int maxLevel = 3;

        @Override
        public Compound apply(Compound parent, Compound unused, int depth) {
            //ignore details below a certain depth
            if (depth >= maxLevel)
                return parent;

            int tDelta = ITERNAL;

            //(a ??? b)
            Term a = parent.term(0);
            Term b = parent.term(1);

            Task pt = premise.task();
            long aTask = pt.subtermTime(a);
            Task pb = premise.belief();
            long aBelief = pb!=null ? pb.subtermTime(a) : ITERNAL;
            long bTask = pt.subtermTime(b);
            long bBelief = pb!=null ? pb.subtermTime(b) : ITERNAL;

            boolean aBoth = (aTask!=TIMELESS && aBelief!=TIMELESS);
            boolean bBoth = (bTask!=TIMELESS && bBelief!=TIMELESS);

            long la = ETERNAL, lb = ETERNAL;
            if ((aTask > TIMELESS) && (aBelief <= TIMELESS))
                la = aTask;
            else if ((aBelief > TIMELESS) && (aTask <= TIMELESS))
                la = aBelief;
            if ((bTask > TIMELESS) && (bBelief <= TIMELESS))
                lb = bTask;
            else if ((bBelief > TIMELESS) && (bTask <= TIMELESS))
                lb = bBelief;

            //TODO handle case when both are known in each (avg?)

            if ((la!=ETERNAL) && (lb!=ETERNAL))
                tDelta = (int)(lb - la);

            if (tDelta != Tense.ITERNAL) {
                parent = parent.t(tDelta);
            }

            return parent;
        }
    };

    public final static class DerivedTask extends MutableTask {

        private final ConceptProcess premise;

        public DerivedTask(@NotNull Termed<Compound> tc, ConceptProcess premise) {
            super(tc);
            this.premise = premise;
        }

        @Override
        public boolean onRevision(@NotNull Task t) {
            Truth conclusion = t.truth();

            ConceptProcess p = this.premise;

            BLink<? extends Task> tLink = p.taskLink;

            //TODO check this Question case is right
            Truth tLinkTruth = tLink.get().truth();
            if (tLinkTruth!=null) {
                float oneMinusDifT = 1f - conclusion.getExpDifAbs(tLinkTruth);
                tLink.andPriority(oneMinusDifT);
                tLink.andDurability(oneMinusDifT);
            }

            Task belief = p.belief();
            if (belief!=null) {
                BLink<? extends Termed> bLink = p.termLink;
                float oneMinusDifB = 1f - conclusion.getExpDifAbs(belief.truth());
                bLink.andPriority(oneMinusDifB);
                bLink.andDurability(oneMinusDifB);
            }

            return true;

        }
    }

    /** part 2 */
    private void derive(@NotNull PremiseMatch m, @NotNull Termed<Compound> c, @Nullable Truth truth, Budget budget, long now, long occ) {

        ConceptProcess premise = m.premise;

        Task task = premise.task();
        Task belief = premise.belief();

        char punct = m.punct.get();

        MutableTask deriving = new DerivedTask(c, premise);


        if ((Global.DEBUG_DETECT_DUPLICATE_DERIVATIONS || Global.DEBUG_LOG_DERIVING_RULE) && Global.DEBUG) {
            deriving.log(rule);
        }

        boolean derivedTemporal = occ != ETERNAL;

        Task derived = deriving
                .punctuation(punct)
                .truth(truth)
                .budget(budget)
                .time(now, occ)
                .parent(task, belief /* null if single */)
                .anticipate(derivedTemporal && anticipate);

        if ((derived = derive(m, derived)) == null)
            return;

        //--------- TASK WAS DERIVED if it reaches here


        if (derivedTemporal && (truth != null) && eternalize) {

            derive(m, new DerivedTask(c, premise) //derived.term())
                    .punctuation(punct)
                    .truth(
                        truth.freq(),
                        eternalize(truth.conf())
                    )

                    .time(now, ETERNAL)

                    .budget(budget)
                    .budgetCompoundForward(premise)

                    .parent(derived)  //this is lighter weight and potentially easier on GC
                    //.parent(task, belief)

                    .log("Immediaternalized") //Immediate Eternalization
            );

        }

    }


    private static Task derive(@NotNull PremiseMatch p, Task derived) {

        //HACK this should exclude the invalid rules which form any of these

        ConceptProcess premise = p.premise;



        //pre-normalize to avoid discovering invalidity after having consumed space and survived the input queue
        derived = derived.normalize(premise.memory());
        if (derived == null) return null;

        if ((null!= premise.derive(derived))) {
            p.receiver.accept(derived);
            return derived;
        }

        return null;
    }


}
