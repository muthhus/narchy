package nars;

import nars.nal.nal1.NAL1Test;
import nars.nal.nal2.NAL2Test;
import nars.nal.nal6.NAL6Test;
import nars.nal.nal7.NAL7Test;
import nars.util.OptiUnit;

import java.io.FileNotFoundException;
import java.util.SortedMap;

public class Repair {

    Class[] testClasses = new Class[] {
        NAL1Test.class, NAL2Test.class, NAL6Test.class
    };

    public static void main(String[] args) throws FileNotFoundException {
        new OptiUnit<NAL7Test>((NAL7Test x) -> {

            //SortedMap<String, Object> stat = x.nar.stats();

            System.out.println("#" + x + " ");
            SortedMap<String, Object> stat = x.nar.stats(System.out);


            return stat;

        }, NAL7Test.class).run((NAL7Test x)->{

            x.test.trace = false;

            return new OptiUnit.Tweaks<>(x)
                .set("cycles", 5)
                .call("nar.termVolumeMax.setValue", 28)
            ;
        }).print(System.out);
    }
}
