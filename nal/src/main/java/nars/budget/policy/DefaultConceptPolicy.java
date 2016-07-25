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
        this.termlinksCapacityMin = new MutableInteger(Math.max(1,termlinksCapacity.intValue()/2));
        this.termlinksCapacityMax = termlinksCapacity;
        this.taskLinksCapacity = taskLinksCapacity;
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

            int v = Math.max(1, c.complexity());
            float complexityFactor = 1f - 1f / (1 + v); //smaller concepts get more termlinks

            int l = Math.round(Util.lerp(min, max, complexityFactor));
            if (c instanceof CompoundConcept)
                l = Math.max(l, ((CompoundConcept)c).templates.size()); //at least enough for its templates

            return l;

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
