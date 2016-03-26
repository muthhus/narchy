package nars.task;

import nars.NAR;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.budget.UnitBudget;
import nars.concept.util.BeliefTable;
import nars.nal.LocalRules;
import nars.nal.Tense;
import nars.term.Compound;
import nars.term.Terms;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Revision Utilities
 */
public class Revision {


    @Nullable
    public static /*Revision*/Task tryRevision(@NotNull Task newBelief, @NotNull NAR nar, List<Task> beliefs) {
        int bsize = beliefs.size();
        if (bsize == 0)
            return null; //nothing to revise with

        Compound newBeliefTerm = newBelief.term();

        //Try to select a best revision partner from existing beliefs:
        Task oldBelief = null;
        float bestRank = 0, bestConf = 0;
        Truth conclusion = null;
        long concTime = Tense.ETERNAL;
        final long now = nar.time();
        final float newBeliefConf = newBelief.conf();
        Truth newBeliefTruth = newBelief.truth();

        for (int i = 0; i < bsize; i++) {
            Task x = beliefs.get(i);

            if (!LocalRules.isRevisible(newBelief, x))
                continue;

            float matchFactor = Terms.termRelevance(newBeliefTerm, x.term());
            if (matchFactor <= 0)
                continue;

//
//            float factor = tRel * freqMatch;
//            if (factor < best) {
//                //even with conf=1.0 it wouldnt be enough to exceed existing best match
//                continue;
//            }

            final int totalEvidence = 1; //newBelief.evidence().length + x.evidence().length;
//            float minValidConf = Math.min(newBeliefConf, x.conf());
//            if (minValidConf < bestConf) continue;
//            float minValidRank = BeliefTable.rankEternalByOriginality(minValidConf, totalEvidence);
//            if (minValidRank < bestRank) continue;

            Truth oldBeliefTruth = x.truth();

            Truth c;
            long t;
            if (newBelief.isEternal()) {
                c = TruthFunctions.revision(newBeliefTruth, oldBeliefTruth, matchFactor, bestConf);
                t = Tense.ETERNAL;
            } else {
                c = TruthFunctions.revision(newBelief,
                        x, now, matchFactor, bestConf);
                t = now;
            }

            if (c == null)
                continue;

            //avoid a duplicate truth at the same time
            if (t==x.occurrence() && c.equals(oldBeliefTruth))
                continue;
            if (t==newBelief.occurrence() && c.equals(newBeliefTruth))
                continue;

            float cconf = c.conf();
            float rank = BeliefTable.rankEternalByOriginality(cconf, totalEvidence);

            if (rank > bestRank)  {
                bestRank = rank;
                bestConf = cconf;
                oldBelief = x;
                conclusion = c;
                concTime = t;
            }
        }

        if (oldBelief != null) {
            Budget revisionBudget = budgetRevision(conclusion, newBelief, oldBelief, nar);
            if (revisionBudget != null) {
                return new RevisionTask(
                        LocalRules.intermpolate(
                                newBelief, oldBelief,
                                newBeliefConf, oldBelief.conf()),
                        revisionBudget,
                        newBelief, oldBelief,
                        conclusion,
                        now, concTime);
            }
        }
        return null;
    }


    /**
     * Evaluate the quality of a revision, set its budget, and de-prioritize the premise tasks and associated links
     * <p>
     * The truth value of the judgment in the task of the premise
     * The truth value of the previously existing belief
     * The truth value of the conclusion of revision
     *
     * @return The budget for the new task
     */
    @NotNull
    public static Budget budgetRevision(Truth revised, @NotNull Task newBelief, @NotNull Task oldBelief, NAR nar) {

        Truth nTruth = newBelief.truth();
        final Budget nBudget = newBelief.budget();


        Truth bTruth = oldBelief.truth();
        float difT = revised.getExpDifAbs(nTruth);

        nBudget.andPriority(1.0f - difT);
        nBudget.andDurability(1.0f - difT);

        float cc = revised.conf();
        float proportion = cc
                / (cc + Math.min(nTruth.conf(), bTruth.conf()));

//		float dif = concTruth.conf()
//				- Math.max(nTruth.conf(), bTruth.conf());
//		if (dif < 0) {
//			String msg = ("Revision fault: previous belief " + oldBelief
//					+ " more confident than revised: " + conclusion);
////			if (Global.DEBUG) {
//				throw new RuntimeException(msg);
////			} else {
////				System.err.println(msg);
////			}
////			dif = 0;
//		}

        float priority =
                proportion * nBudget.pri();
                //or(dif, nBudget.pri());
        float durability =
                //aveAri(dif, nBudget.dur());
                proportion * nBudget.dur();
        float quality = BudgetFunctions.truthToQuality(revised);

		/*
         * if (priority < 0) { memory.nar.output(ERR.class, new
		 * RuntimeException(
		 * "BudgetValue.revise resulted in negative priority; set to 0"));
		 * priority = 0; } if (durability < 0) { memory.nar.output(ERR.class,
		 * new RuntimeException(
		 * "BudgetValue.revise resulted in negative durability; set to 0; aveAri(dif="
		 * + dif + ", task.getDurability=" + task.getDurability() +") = " +
		 * durability)); durability = 0; } if (quality < 0) {
		 * memory.nar.output(ERR.class, new RuntimeException(
		 * "BudgetValue.revise resulted in negative quality; set to 0"));
		 * quality = 0; }
		 */

        if (BudgetFunctions.valid(durability, nar)) {
            return new UnitBudget(priority, durability, quality);
        }
        return null;
    }

}
