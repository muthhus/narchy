package nars.conceptualize.state;

import jcog.Util;
import jcog.data.MutableInteger;
import nars.concept.BaseConcept;
import nars.concept.Concept;
import org.jetbrains.annotations.NotNull;

import static jcog.Util.clamp;

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
    public final int beliefsMaxTemp;
    public final int beliefsMinTemp;
    public final int goalsMaxTemp;
    public final int goalsMinTemp;

    /** minimum of 3 beliefs per belief table. for eternal, this allows revision between two goals to produce a third  */
    public DefaultConceptState(String id, int beliefsCapTotal, int goalsCapTotal, int questionsMax, int termlinksCapacity, int taskLinksCapacity) {
        this(   id,
                new MutableInteger(clamp(beliefsCapTotal/4, 2, 6)), //belief ete ~1/4
                new MutableInteger(clamp(beliefsCapTotal/4, 2, 6)),   //goal ete  ~1/4
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
        this.beliefsMaxTemp = beliefsMaxTemp.intValue();
        this.beliefsMinTemp = beliefsMaxTemp.intValue()/8;
        this.goalsMaxEte = goalsMaxEte;
        this.goalsMaxTemp = goalsMaxTemp.intValue();
        this.goalsMinTemp = goalsMaxTemp.intValue()/8;
        this.questionsMax = questionsMax;

        this.termLinksCapacityMin = new MutableInteger(Math.max(1,termlinksCapacity.intValue()/4));
        this.termLinksCapacityMax = termlinksCapacity;
        this.taskLinksCapacityMin = new MutableInteger(Math.max(1,taskLinksCapacity.intValue()/4));
        this.taskLinksCapacityMax = taskLinksCapacity;
    }


    @Override
    public int beliefCap(BaseConcept compoundConcept, boolean beliefOrGoal, boolean eternalOrTemporal) {
        int max, min;
        if (beliefOrGoal) {
            max = eternalOrTemporal ? beliefsMaxEte.intValue() : beliefsMaxTemp;
            min = eternalOrTemporal ? beliefsMaxEte.intValue() : beliefsMinTemp;
        } else {
            max = eternalOrTemporal ? goalsMaxEte.intValue() : goalsMaxTemp;
            min = eternalOrTemporal ? goalsMaxEte.intValue() : goalsMinTemp;
        }

        return Util.lerp(Util.unitize((-1 + compoundConcept.complexity())/32f), max, min);
        //return (int) Math.ceil(max * Math.min(1f, (1f / (compoundConcept.volume()/ beliefComplexityCapacity))));
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
        float complexityFactor = ((v-1) / 32); //(nar.compoundVolumeMax.intValue()/2f); //HEURISTIC
        complexityFactor = Util.sqr(Util.unitize(complexityFactor)); //clip at +1

        return Util.lerp(complexityFactor, max, min); //at least enough for its templates
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
