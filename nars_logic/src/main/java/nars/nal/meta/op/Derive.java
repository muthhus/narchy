package nars.nal.meta.op;

import com.google.common.base.Joiner;
import nars.Global;
import nars.Memory;
import nars.bag.BLink;
import nars.budget.Budget;
import nars.nal.PremiseMatch;
import nars.nal.PremiseRule;
import nars.nal.meta.AbstractLiteral;
import nars.nal.meta.AndCondition;
import nars.nal.meta.BooleanCondition;
import nars.nal.meta.ProcTerm;
import nars.nal.nal7.Tense;
import nars.process.ConceptProcess;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import nars.term.compound.Compound;
import nars.term.match.EllipsisMatch;
import nars.term.variable.Variable;
import nars.truth.Truth;

import static nars.truth.TruthFunctions.eternalizedConfidence;

/**
 * Handles matched derivation results
 * < (&&, postMatch1, postMatch2) ==> derive(term) >
 */
public class Derive extends AbstractLiteral implements ProcTerm<PremiseMatch> {

    private final String id;

    private final boolean anticipate;
    private final boolean eternalize;
    private final PremiseRule rule;

    /** result pattern */
    private final Term term;

    public final AndCondition<PremiseMatch> postMatch; //TODO use AND condition


    public Derive(PremiseRule rule, Term term, BooleanCondition[] postMatch, boolean anticipate, boolean eternalize) {
        this.rule = rule;
        this.postMatch = postMatch.length>0 ? new AndCondition(postMatch) : null;
        this.term = term;
        this.anticipate = anticipate;
        this.eternalize = eternalize;

        String i = "Derive:(" + term;
        if (eternalize || anticipate) {
            if (eternalize && anticipate) {
                i += ", {eternalize,anticipate}";
            } else if (eternalize && !anticipate) {
                i += ", {eternalize}";
            } else if (anticipate && !eternalize) {
                i += ", {anticipate}";
            }
        }

        if (postMatch.length > 0) {
            i += ", {" + Joiner.on(',').join(postMatch) + '}';
        }

        i += ")";
        this.id = i;
    }


    @Override
    public String toString() {
        return id;
    }


    /** main entry point for derivation result handler.
     * @return true to allow the matcher to continue matching,
     * false to stop it */
    @Override public final void accept(PremiseMatch m) {

        Term tt = solve(m);

        if ((tt != null) && ((postMatch==null) || (postMatch.booleanValueOf(m))))
            derive(m, tt);

    }



    public Term solve(PremiseMatch match) {

        Term derivedTerm = match.apply(term);
        if (derivedTerm == null)
            return null;
        if ((derivedTerm instanceof EllipsisMatch)) {
            throw new RuntimeException("invalid ellipsis match: " + derivedTerm);
//            EllipsisMatch em = ((EllipsisMatch)derivedTerm);
//            if (em.size()!=1) {
//                throw new RuntimeException("invalid ellipsis match: " + em);
//            }
//            derivedTerm = em.term(0); //unwrap the item
        }


        //HARD VOLUME LIMIT
        if (derivedTerm.volume() > Global.COMPOUND_VOLUME_MAX) {
            //$.logger.error("Term volume overflow");
            /*c.forEach(x -> {
                Terms.printRecursive(x, (String line) ->$.logger.error(line) );
            });*/

            if (Global.DEBUG) {
                String message = "Term volume overflow: " + derivedTerm;
                System.err.println(message);
                System.exit(1);
                //throw new RuntimeException(message);
            } else {
                return null;
            }
        }

        return derivedTerm;
    }



    /** part 1 */
    private void derive(PremiseMatch p, Term t) {

        if (t != null && !Variable.hasPatternVariable(t)) {

            Memory mem = p.premise.memory();

            //get the normalized term to determine the budget (via it's complexity)
            //this way we can determine if the budget is insufficient
            //before conceptualizating in mem.taskConcept
            Termed tNorm = mem.index.normalized(t);

            //HACK why?
            if (tNorm == null || !tNorm.term().isCompound())
                return;

            Truth truth = p.truth.get();

            Budget budget = p.getBudget(truth, tNorm);
            if (budget == null)
                return;


            int tDelta = p.tDelta.getIfAbsent(Tense.ITERNAL);
            Termed<Compound> c;
            if (tDelta != Tense.ITERNAL) {

                Compound ct = (Compound) tNorm.term();

                //check reversal against pattern
//                if (ct.op().isCommutative() && (!p.getXY(((Compound)term).term(0)).equals(ct.term(0)))) {
//                    tDelta = -tDelta;
//                }

                //set time relation
                c = ct.t(tDelta);
            } else {
                //c = mem.taskConcept(tNorm); //accelerant: concept lookup
                c = tNorm;
            }


            if (c != null) {
                derive(p, c, truth, budget);
            }
        }

    }

    public final static class DerivedTask extends MutableTask {

        private final ConceptProcess premise;

        public DerivedTask(Termed<Compound> tc, ConceptProcess premise) {
            super(tc);
            this.premise = premise;
        }

        @Override
        public void onRevision(Truth conclusion) {
            ConceptProcess p = this.premise;

            BLink<Task> tLink = p.taskLink;
            BLink<Termed> bLink = p.termLink;

            float oneMinusDifT = 1f - conclusion.getExpDifAbs(tLink.get().getTruth());
            tLink.andPriority(oneMinusDifT);
            tLink.andDurability(oneMinusDifT);

            Task belief = p.getBelief();
            if (belief!=null) {
                float oneMinusDifB = 1f - conclusion.getExpDifAbs(belief.getTruth());
                bLink.andPriority(oneMinusDifB);
                bLink.andDurability(oneMinusDifB);
            }
        }
    }

    /** part 2 */
    private void derive(PremiseMatch m, Termed<Compound> c, Truth truth, Budget budget) {

        ConceptProcess premise = m.premise;

        Task task = premise.getTask();
        Task belief = premise.getBelief();

        char punct = m.punct.get();

        MutableTask deriving = new DerivedTask(c, premise);

        long now = premise.time();
        long occ = premise.getTask().getOccurrenceTime();


        //just not able to measure it, closed world assumption gone wild.
        if (occ != Tense.ETERNAL) {
            if (premise.isEternal() && !premise.nal(7)) {
                throw new RuntimeException("eternal premise " + premise + " should not result in non-eternal occurence time: " + deriving + " via rule " + rule);
            }
            int occDelta = m.occDelta.getIfAbsent(0);
            occ += occDelta;
        }


        if ((Global.DEBUG_DETECT_DUPLICATE_DERIVATIONS || Global.DEBUG_LOG_DERIVING_RULE) && Global.DEBUG) {
            deriving.log(rule);
        }

        Task derived = deriving
                .punctuation(punct)
                .truth(truth)
                .budget(budget)
                .time(now, occ)
                .parent(task, belief /* null if single */)
                .anticipate(occ != Tense.ETERNAL && anticipate);

        if ((derived = derive(m, derived)) == null)
            return;

        //--------- TASK WAS DERIVED if it reaches here


        if (truth != null && eternalize && !derived.isEternal()) {

            derive(m,
                    new DerivedTask(c, premise) //derived.term())
                            .punctuation(punct)
                            .truth(
                                truth.getFrequency(),
                                eternalizedConfidence(truth.getConfidence())
                            )
                            .budgetCompoundForward(premise)
                            .time(now, Tense.ETERNAL)
                            .parent(task, belief)
            );

        }

    }


    public Task derive(PremiseMatch p, Task derived) {

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

//        public void partial(RuleMatch match) {
//            Term dt = solve(match);
//            if (dt == null) return ;
//
//            //maybe this needs applied somewhre diferent
//            if (!post(match))
//                return ;
//
//            VarCachedVersionMap secondary = match.secondary;
//
//            if (!secondary.isEmpty()) {
//
//                Term rederivedTerm = dt.apply(secondary, true);
//
//                //its possible that the substitution produces an invalid term, ex: an invalid statement
//                dt = rederivedTerm;
//                if (dt == null) return;
//            }
//
//            dt = dt.normalized();
//            if (dt == null) return;
//
//
//            derive(match, dt);
//        }
