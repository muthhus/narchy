package nars.table;

import nars.NAR;
import nars.Task;
import nars.concept.CompoundConcept;
import nars.concept.SensorConcept;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import nars.util.signal.ScalarSignal;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO High-efficiency datapoint table that can store many events
 * and process them in faster and less clumsy ways than ordinary Tasks
 */
public class SensorBeliefTable extends DefaultBeliefTable  {

    private static final Logger logger = LoggerFactory.getLogger(SensorBeliefTable.class);
    private final SensorConcept sensorConcept;

    public SensorBeliefTable(SensorConcept sensorConcept) {
        super();
        this.sensorConcept = sensorConcept;
    }



    @Override
    public TruthDelta add(@NotNull Task input, @NotNull QuestionTable questions, @NotNull CompoundConcept<?> concept, @NotNull NAR nar) {

        boolean refresh = false;

        long now = nar.time();

        boolean local = input instanceof ScalarSignal.SignalTask;
        if (local) {
//            //invalidate existing derived beliefs which precede a sensor input
//            temporal.removeIf((t)->{
//                if (t instanceof DerivedTask && t.end() <= input.occurrence())
//                    return true;
//                return false;
//            }, nar);
        } else if (!input.isInput()) {

            if (input.isEternal()) {
                logger.warn("reject non-authentic eternal override:\n{}", input.proof());
                return null; //reject non-input eternal derivations
            }

            long is = input.start();
            long ie = input.end();

            if (is > now && ie > now) {
                //entirely future prediction
                refresh = true;
            } else if (is <= now && ie >= now) {
                //touches present
                refresh = true;
            } else {
                //touches past
                Truth computedTruth = truth(input.end(), nar.time.dur());
                if (computedTruth!=null && computedTruth.conf() >= input.conf()) {
                    //logger.info("reject derived signal:\n{}", input);//.proof());
                    return null;
                }
            }
        }



        TruthDelta d = super.add(input, questions, concept, nar);

        boolean added = d != null;

        if (local && !added) {
            logger.warn("rejected authentic signal:{}", input);
        }

        if (!local && refresh && added)
            sensorConcept.sensor.invalidate();

        return d;
    }
}
