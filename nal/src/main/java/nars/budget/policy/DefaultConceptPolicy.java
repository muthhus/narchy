package nars.budget.policy;

import nars.concept.AbstractConcept;
import nars.concept.CompoundConcept;
import nars.util.Util;
import nars.util.data.MutableInteger;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 5/11/16.
 */
public final class DefaultConceptPolicy implements ConceptPolicy {

    public final MutableInteger beliefsMaxEte, goalsMaxEte;
    public final MutableInteger questionsMax;
    @NotNull
    public final MutableInteger termlinksCapacityMax, termlinksCapacityMin, taskLinksCapacity;
    private final MutableInteger beliefsMaxTemp;
    private final MutableInteger goalsMaxTemp;

    public DefaultConceptPolicy(int beliefsCapTotal, int goalsCapTotal, int questionsMax, int termlinksCapacity, int taskLinksCapacity) {
        this(new MutableInteger(Math.max(1, beliefsCapTotal / 2)),
                new MutableInteger(Math.max(1, goalsCapTotal / 2)),
                new MutableInteger(questionsMax),
                new MutableInteger(termlinksCapacity),
                new MutableInteger(taskLinksCapacity)
        );
    }

    DefaultConceptPolicy(MutableInteger beliefsMaxEte, MutableInteger goalsMaxEte, MutableInteger questionsMax, @NotNull MutableInteger termlinksCapacity, @NotNull MutableInteger taskLinksCapacity) {
        this.beliefsMaxEte = beliefsMaxEte;
        this.beliefsMaxTemp = beliefsMaxEte;
        this.goalsMaxEte = goalsMaxEte;
        this.goalsMaxTemp = goalsMaxEte;
        this.questionsMax = questionsMax;
        this.termlinksCapacityMin = termlinksCapacity;
        this.termlinksCapacityMax = new MutableInteger(termlinksCapacity.intValue()); //HACK
        this.taskLinksCapacity = taskLinksCapacity;
    }

    /** no eternal; use allocated eternal capacity added to temporals */
    public static void beliefCapacityNonEternal(@NotNull CompoundConcept c, @NotNull ConceptPolicy p, int multiplier) {
        c.beliefs().capacity(0,
                Math.max(p.beliefCap(c, true, true),p.beliefCap(c, true, false)) * multiplier
        );
    }

    /** no eternal; use allocated eternal capacity added to temporals */
    public static void goalCapacityNonEternal(@NotNull CompoundConcept c, @NotNull ConceptPolicy p) {
        c.goals().capacity(0, p.beliefCap(c, false, true) + p.beliefCap(c, false, false));
    }
    /** no eternal; use allocated eternal capacity added to temporals */
    public static void goalCapacityOneEternal(@NotNull CompoundConcept c, @NotNull ConceptPolicy p) {
        c.goals().capacity(1, p.beliefCap(c, false, true) + p.beliefCap(c, false, false));
    }
    /** no eternal; use allocated eternal capacity added to temporals */
    public static void goalCapacityOneEternal(@NotNull CompoundConcept c, @NotNull ConceptPolicy p, int multiplier) {
        c.goals().capacity(1, Math.max(p.beliefCap(c, false, true), p.beliefCap(c, false, false)) * multiplier);
    }

    @Override
    public int beliefCap(CompoundConcept compoundConcept, boolean beliefOrGoal, boolean eternalOrTemporal) {
        if (beliefOrGoal) {
            if (eternalOrTemporal)
                return beliefsMaxEte.intValue();
            else
                return beliefsMaxTemp.intValue();
        } else {
            if (eternalOrTemporal)
                return goalsMaxEte.intValue();
            else
                return goalsMaxTemp.intValue();
        }
    }

    @Override
    public int linkCap(@NotNull AbstractConcept c, boolean termOrTask) {
        if (termOrTask) {
            int min = termlinksCapacityMin.intValue();
            int max = termlinksCapacityMax.intValue();
            int v = Math.max(1, c.volume());

            int tl = (int)Math.ceil(Util.lerp(min, max, 1f - 1f/(v)));
            //System.out.println(c + " " + min + " " + max + " " + c.volume() + " " + ":" + tl);
            return tl;

            //return termlinksCapacity.intValue();
        } else {
            return taskLinksCapacity.intValue();
        }
    }

    @Override
    public final int questionCap(boolean questionOrQuest) {
        return questionsMax.intValue();
    }

//        public int getBeliefsCapacity(Termed t);
//        public int getGoalsCapabity(Termed t);
//        public int getTermLinksCapacity(Termed t);
//        public int getTaskLinksCapacity(Termed t);
}
