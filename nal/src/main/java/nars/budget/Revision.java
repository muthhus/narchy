package nars.budget;

import nars.NAR;
import nars.concept.util.BeliefTable;
import nars.nal.LocalRules;
import nars.nal.Tense;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Termed;
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
    public static Task tryRevision(@NotNull Task newBelief, @NotNull NAR nar, List<Task> beliefs) {
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
            if (x.isDeleted() || !LocalRules.isRevisible(newBelief, x)) continue;

            float matchFactor = Terms.termRelevance(newBeliefTerm, x.term());
            if (matchFactor <= 0) continue;

//
//            float factor = tRel * freqMatch;
//            if (factor < best) {
//                //even with conf=1.0 it wouldnt be enough to exceed existing best match
//                continue;
//            }

            final int totalEvidence = 1; //newBelief.evidence().length + x.evidence().length;
            float minValidConf = Math.min(newBeliefConf, x.conf());
            if (minValidConf < bestConf) continue;
            float minValidRank = BeliefTable.rankEternalByOriginality(minValidConf, totalEvidence);
            if (minValidRank < bestRank) continue;

            Truth c;
            long t;
            if (newBelief.isEternal()) {
                c = TruthFunctions.revision(newBeliefTruth, x.truth(), matchFactor, minValidConf);
                if (c == null)
                    continue;
                t = Tense.ETERNAL;
            } else {
                c = TruthFunctions.revision(
                        newBelief,
                        x, now, matchFactor, minValidConf);
                if (c == null)
                    continue;
                t = now; //Math.max(newBelief.occurrence(), x.occurrence());
            }

            //TODO avoid allocating Truth's here

            //float ffreqMatch = 1f/(1f + Math.abs(newBeliefFreq - x.freq()));

            float cconf = c.conf();
            float rank = BeliefTable.rankEternalByOriginality(cconf, totalEvidence);

            if ((cconf > 0) && (rank > bestRank)) {
                bestRank = rank;
                bestConf = cconf;
                oldBelief = x;
                conclusion = c;
                concTime = t;
            }
        }

        if ( /* nothing matches */  (oldBelief == null) ||
             /* equivalent */       (conclusion.equals(newBeliefTruth) && concTime == newBelief.occurrence()))
            return null;


        Termed<Compound> term = LocalRules.intermpolate(newBelief, oldBelief, newBeliefConf, oldBelief.conf());

        MutableTask revision = new MutableTask(term, newBelief.punc())
                .truth(conclusion)
                .parent(newBelief, oldBelief)
                .time(now, concTime)
                //.state(newBelief.state())
                .because("Insertion Revision");
                /*.because("Insertion Revision (%+" +
                                Texts.n2(conclusion.freq() - newBelief.freq()) +
                        ";+" + Texts.n2(conclusion.conf() - newBelief.conf()) + "%");*/

        return budgetRevision(
                revision, newBelief, oldBelief, nar) ?
                    revision : null;
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
    public static boolean budgetRevision(@NotNull Task revision, @NotNull Task newBelief, @NotNull Task oldBelief, NAR nar) {

        Truth nTruth = newBelief.truth();
        final Budget nBudget = newBelief.budget();

        Truth concTruth = revision.truth();
        Truth bTruth = oldBelief.truth();
        float difT = concTruth.getExpDifAbs(nTruth);

        nBudget.andPriority(1.0f - difT);
        nBudget.andDurability(1.0f - difT);

        float cc = concTruth.conf();
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
        float quality = BudgetFunctions.truthToQuality(concTruth);

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

        if (BudgetFunctions.valid(revision.budget().budget(priority, durability, quality), nar)) {

            oldBelief.onRevision(revision);
            newBelief.onRevision(revision);

            float newBeliefConf = newBelief.conf();

            //decrease the budget of the parents so the priority sum among the 2 parents and the child remains the same (balanced)
            //TODO maybe consider rank (incl. evidence) not just conf()
            float newBeliefContribution = newBeliefConf / (newBeliefConf + oldBelief.conf());
            float oldBeliefContribution = 1f - newBeliefContribution;
            float revisionPri = revision.pri();
            float newDiscount = revisionPri * oldBeliefContribution;
            float oldDiscount = revisionPri * newBeliefContribution;
            float nextNewPri = newBelief.pri() - newDiscount;
            float nextOldPri = oldBelief.pri() - oldDiscount;

            if (nextNewPri < 0) {
                nextOldPri -= -nextNewPri; //subtract remainder from the other
                nextNewPri = 0;
            }
            if (nextOldPri < 0) {
                nextNewPri -= -nextOldPri; //subtract remainder from the other
                nextOldPri = 0;
            }

            if ((nextNewPri < 0) || (nextOldPri < 0))
                throw new RuntimeException("revision budget underflow");

            //apply the changes
            newBelief.budget().setPriority(nextNewPri);
            oldBelief.budget().setPriority(nextOldPri);

            return true;
        } else {
            return false;
        }
    }

}
