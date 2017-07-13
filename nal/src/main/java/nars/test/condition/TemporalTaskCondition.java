package nars.test.condition;

import nars.NAR;
import nars.Narsese;
import nars.Task;
import org.jetbrains.annotations.NotNull;

import static nars.time.Tense.ETERNAL;

/**
 * Created by me on 10/14/15.
 */
public class TemporalTaskCondition extends EternalTaskCondition {

    /**
     * occurrence time (absolute) valid range
     */
    public final long occStartStart, occStartEnd;
    private final long occEndStart;
    private final long occEndEnd;


    public TemporalTaskCondition(@NotNull NAR n, long cycleStart, long cycleEnd,
                                 long occStartStart, long occStartEnd,
                                 long occEndStart, long occEndEnd,
                                 @NotNull String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax) throws Narsese.NarseseException {
        super(n, cycleStart, cycleEnd, sentenceTerm, punc, freqMin, freqMax, confMin, confMax);
        this.occStartStart = occStartStart;
        this.occStartEnd = occStartEnd;
        this.occEndStart = occEndStart;
        this.occEndEnd = occEndEnd;
    }

    @NotNull
    @Override
    public String toString() {
        return super.toString() + " occurrs: (" + occStartStart + ',' + occStartEnd +
                ')';
    }

    @Override
    protected boolean occurrenceTimeMatches(@NotNull Task task) {
        long s = task.start();

        if (s == ETERNAL) return false;

        if ((s < occStartStart) || (s > occStartEnd))
            return false;

        long e = task.end();
        return ((e >= occEndStart) && (e <= occEndEnd));

//
////                    long at = relativeToCondition ? getCreationTime() : task.getCreationTime();
//        final boolean tmatch = false;
////                    switch () {
////                        //TODO write time matching
//////                        case Past: tmatch = oc <= (-durationWindowNear + at); break;
//////                        case Present: tmatch = oc >= (-durationWindowFar + at) && (oc <= +durationWindowFar + at); break;
//////                        case Future: tmatch = oc > (+durationWindowNear + at); break;
////                        default:
//        throw new RuntimeException("Invalid tense for non-eternal TaskCondition: " + this);
////                    }
////                    if (!tmatch) {
////                        //beyond tense boundaries
////                        //distance += rangeError(oc, -halfDur, halfDur, true) * tenseCost;
////                        distance += 1; //tenseCost + rangeError(oc, creationTime, creationTime, true); //error distance proportional to occurence time distance
////                        match = false;
////                    }
////                    else {
////                        //System.out.println("matched time");
////                    }
//
//
//        //return true;
    }
}
