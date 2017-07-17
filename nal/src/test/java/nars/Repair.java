package nars;

import nars.nal.nal1.NAL1Test;
import nars.nal.nal2.NAL2Test;
import nars.nal.nal6.NAL6Test;
import nars.nal.nal7.NAL7Test;
import nars.util.OptiUnit;
import org.intelligentjava.machinelearning.decisiontree.FloatTable;

import java.io.FileNotFoundException;
import java.util.SortedMap;

public class Repair {

    Class[] testClasses = {
        NAL1Test.class, NAL2Test.class, NAL6Test.class
    };

    public static void main(String[] args) {
        OptiUnit o = new OptiUnit<>((x) -> {

            //OBSERVE AND COLLECT EXPERIMENT RESULTS
            SortedMap<String, Object> stat = x.nar.stats();
            stat.put("score", x.test.score);

            return stat;

        }, NAL7Test.class).run((x) -> {

            //SETUP EXPERIMENT
            x.test.trace = false;

            return new OptiUnit.Tweaks<>(x)
                    .set("cycles", 5)
                    .call("nar.termVolumeMax.setValue", 28)
                    ;

        });

        FloatTable<String> table = o.table("concept count", "concept fire activates", "score");
        table.print(System.out);

        o.print(System.out);
    }
}
