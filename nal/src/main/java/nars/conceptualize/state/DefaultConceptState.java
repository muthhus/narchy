package nars.conceptualize.state;

import jcog.Util;
import jcog.data.MutableInteger;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 5/11/16.
 */
public final class DefaultConceptState extends ConceptState {

    /** fall-off rate for belief table capacity */
    float beliefComplexityCapacity = 10;

    public final MutableInteger beliefsMaxEte, goalsMaxEte;
    public final MutableInteger questionsMax;
    @NotNull
    public final MutableInteger termLinksCapacityMax, termLinksCapacityMin, taskLinksCapacityMax, taskLinksCapacityMin;
    public final MutableInteger beliefsMaxTemp;
    private final MutableInteger goalsMaxTemp;

    /** minimum of 3 beliefs per belief table. for eternal, this allows revision between two goals to produce a third  */
    public DefaultConceptState(String id, int beliefsCapTotal, int goalsCapTotal, int questionsMax, int termlinksCapacity, int taskLinksCapacity) {
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

    DefaultConceptState(String id, MutableInteger beliefsMaxEte, MutableInteger goalsMaxEte,
                        MutableInteger beliefsMaxTemp, MutableInteger goalsMaxTemp,
                        MutableInteger questionsMax, @NotNull MutableInteger termlinksCapacity, @NotNull MutableInteger taskLinksCapacity) {
        super("___" + id);
        this.beliefsMaxEte = beliefsMaxEte;
        this.beliefsMaxTemp = beliefsMaxTemp;
        this.goalsMaxEte = goalsMaxEte;
        this.goalsMaxTemp = goalsMaxTemp;
        this.questionsMax = questionsMax;
        this.termLinksCapacityMin = new MutableInteger(Math.max(1,termlinksCapacity.intValue()/8));
        this.termLinksCapacityMax = termlinksCapacity;
        this.taskLinksCapacityMin = new MutableInteger(Math.max(1,taskLinksCapacity.intValue()/8));
        this.taskLinksCapacityMax = taskLinksCapacity;
    }


    @Override
    public int beliefCap(CompoundConcept compoundConcept, boolean beliefOrGoal, boolean eternalOrTemporal) {
        int max;
        if (beliefOrGoal) {
            max = eternalOrTemporal ? beliefsMaxEte.intValue() : beliefsMaxTemp.intValue();
        } else {
            max = eternalOrTemporal ? goalsMaxEte.intValue() : goalsMaxTemp.intValue();
        }
        return tasks(compoundConcept, beliefComplexityCapacity, max);
    }

    static int tasks(CompoundConcept compoundConcept, float complexityCost, int b) {
        return (int) Math.ceil(b * Math.min(1f, (1f / (compoundConcept.volume()/complexityCost))));
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
        float complexityFactor = v / 8; //(nar.compoundVolumeMax.intValue()/2f); //HEURISTIC
        complexityFactor = Math.min(complexityFactor, 1f); //clip at +1

        return Util.lerp(complexityFactor, min, max); //at least enough for its templates
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
