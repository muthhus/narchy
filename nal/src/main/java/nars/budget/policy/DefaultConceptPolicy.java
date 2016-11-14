package nars.budget.policy;

import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.util.Util;
import nars.util.data.MutableInteger;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 5/11/16.
 */
public final class DefaultConceptPolicy extends ConceptPolicy {

    public final MutableInteger beliefsMaxEte, goalsMaxEte;
    public final MutableInteger questionsMax;
    @NotNull
    public final MutableInteger termLinksCapacityMax, termLinksCapacityMin, taskLinksCapacityMax, taskLinksCapacityMin;
    private final MutableInteger beliefsMaxTemp;
    private final MutableInteger goalsMaxTemp;

    /** minimum of 3 beliefs per belief table. for eternal, this allows revision between two goals to produce a third  */
    public DefaultConceptPolicy(String id, int beliefsCapTotal, int goalsCapTotal, int questionsMax, int termlinksCapacity, int taskLinksCapacity) {
        this(   id,
                new MutableInteger(Math.max(3, beliefsCapTotal / 4)), //belief ete ~1/4
                new MutableInteger(Math.max(3, goalsCapTotal   / 4)),   //goal ete  ~1/4
                new MutableInteger(Math.max(3, beliefsCapTotal * 3 / 4)), //belief temp ~3/4
                new MutableInteger(Math.max(3, goalsCapTotal   * 3 / 4)), //goal temp  ~3/4
                new MutableInteger(questionsMax),
                new MutableInteger(termlinksCapacity),
                new MutableInteger(taskLinksCapacity)
        );
    }

    DefaultConceptPolicy(String id, MutableInteger beliefsMaxEte, MutableInteger goalsMaxEte,
                         MutableInteger beliefsMaxTemp, MutableInteger goalsMaxTemp,
                         MutableInteger questionsMax, @NotNull MutableInteger termlinksCapacity, @NotNull MutableInteger taskLinksCapacity) {
        super("___" + id);
        this.beliefsMaxEte = beliefsMaxEte;
        this.beliefsMaxTemp = beliefsMaxTemp;
        this.goalsMaxEte = goalsMaxEte;
        this.goalsMaxTemp = goalsMaxTemp;
        this.questionsMax = questionsMax;
        this.termLinksCapacityMin = new MutableInteger(Math.max(1,termlinksCapacity.intValue()/2));
        this.termLinksCapacityMax = termlinksCapacity;
        this.taskLinksCapacityMin = new MutableInteger(Math.max(1,taskLinksCapacity.intValue()/2));
        this.taskLinksCapacityMax = taskLinksCapacity;
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
    public int linkCap(@NotNull Concept c, boolean termOrTask) {
        if (termOrTask) {

            return lerp(c, this.termLinksCapacityMin, this.termLinksCapacityMax);

        } else {
            return lerp(c, this.taskLinksCapacityMin, this.taskLinksCapacityMax);
        }
    }

    public static int lerp(@NotNull Concept c, @NotNull MutableInteger _min, @NotNull MutableInteger _max) {
        int min = _min.intValue();
        int max = _max.intValue();

        float v = c.complexity();
        float complexityFactor = v / 64; //(nar.compoundVolumeMax.intValue()/2f); //HEURISTIC
        complexityFactor = Math.min(complexityFactor, 1f); //clip at +1

        return Math.max(Util.lerp(min, max, complexityFactor), 1 + c.templates().size()); //at least enough for its templates
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
