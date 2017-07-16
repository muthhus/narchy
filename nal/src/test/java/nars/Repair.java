package nars;

import nars.nal.nal1.NAL1Test;
import nars.nal.nal2.NAL2Test;
import nars.nal.nal6.NAL6Test;
import nars.nal.nal7.NAL7Test;
import nars.util.OptiUnit;

import java.util.SortedMap;

public class Repair {

    Class[] testClasses = new Class[] {
        NAL1Test.class, NAL2Test.class, NAL6Test.class
    };

    public static void main(String[] args) {
        new OptiUnit<NAL7Test>((NAL7Test x) -> {

            //SortedMap<String, Object> stat = x.nar.stats();

            System.out.println("#" + x + " ");
            SortedMap<String, Object> stat = x.nar.stats(System.out);


            return stat;

        }, NAL7Test.class).run((NAL7Test x)->{

            x.test.trace = false;

            OptiUnit.TweakMap t = new OptiUnit.TweakMap(x);
            t.set("cycles", 28);
            t.call("nar.termVolumeMax.setValue", 28);

            System.out.println(t);

            return t;

        });
    }
}
