package nars;

import nars.nal.AbstractNALTest;
import nars.nal.nal6.NAL6Test;
import nars.util.OptiUnit;
import org.intelligentjava.machinelearning.decisiontree.FloatTable;
import org.intelligentjava.machinelearning.decisiontree.RealDecisionTree;

import java.util.SortedMap;

public class Repair {


    public static void main(String[] args) {

        Class[] testClasses = {
                //NAL1Test.class, NAL2Test.class,
                NAL6Test.class
        };

        OptiUnit<AbstractNALTest> o = new OptiUnit<AbstractNALTest>((x) -> {

            //OBSERVE AND COLLECT EXPERIMENT RESULTS
            SortedMap<String, Object> stat = x.nar.stats();
            stat.put("score", x.test.score);

            return stat;

        }, testClasses);

        for (int v : new int[] { 16 }) {
            o.run((x) -> {

                //SETUP EXPERIMENT
                x.test.trace = false;

                return new OptiUnit.Tweaks<>(x)
                        .set("cycles", 100)
                        .call("nar.termVolumeMax.setValue", v)
                        ;

            });
        }

        o.print(System.out);

        FloatTable<String> table = o.table(
                "concept count",
                "nar.termVolumeMax.setValue(",
                //"concept fire activates",
                //"concept fire premises",
                "belief count",
                "score");
        table.print(System.out);

        RealDecisionTree tree = new RealDecisionTree(table, 3, 8,
                "LL", "MM", "HH");
        tree.print(System.out);

    }
}
