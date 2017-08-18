package nars;

import jcog.pri.op.PriMerge;
import nars.exe.FocusExec;
import nars.nal.nal1.NAL1Test;
import nars.nal.nal2.NAL2Test;
import nars.nal.nal3.NAL3Test;
import nars.util.AbstractNALTest;
import nars.util.OptiUnit;
import org.intelligentjava.machinelearning.decisiontree.DecisionTree;
import org.intelligentjava.machinelearning.decisiontree.FloatTable;
import org.intelligentjava.machinelearning.decisiontree.RealDecisionTree;

import java.util.SortedMap;

public class Repair {


    public static void main(String[] args) {

        Class[] testClasses = {
                NAL1Test.class,
                NAL2Test.class,
                NAL3Test.class,
                //NAL4MultistepTest.class
                //AllNAL.class
        };

        OptiUnit<AbstractNALTest> o = new OptiUnit<AbstractNALTest>((x) -> {

            //OBSERVE AND COLLECT EXPERIMENT RESULTS
            SortedMap<String, Object> stat = x.nar.stats();
            stat.put("score", x.test.score);

            return stat;

        }, testClasses);


        for (PriMerge termlinkMerge : new PriMerge[]{PriMerge.max, PriMerge.avg.plus, PriMerge.or}) {
            for (int subCycles : new int[]{2}) {
                for (int ttl : new int[]{64}) {
            /*for (int termVol : new int[]{16})*/
                    o.add((x) -> {

                        //SETUP EXPERIMENT
                        x.test.trace = false;


                        return new OptiUnit.Tweaks<>(x)

                                //.set("cycles", 100)
                                .call("subCycles", (n, v) -> {
                                    ((FocusExec) (n.nar.exe)).subCycles = v;
                                }, subCycles)
                                .call("termlinkMerge", (n, v) -> {
                                    Param.termlinkMerge = termlinkMerge;
                                }, termlinkMerge)
                                .call("nar.matchTTL.setValue", ttl)
                                //.call("nar.termVolumeMax.setValue", termVol)
                                ;

                    });
                }
            }
        }

        o.run();

        o.print(System.out);

        //tree(o);


    }

    static void tree(OptiUnit<AbstractNALTest> o) {
        FloatTable<String> table = o.table( //(f) -> f[0] /* score */ > 0,
                "score",
                //"subCycles",
                "nar.matchTTL.setValue",
                //"nar.termVolumeMax.setValue(",
                "concept fire activations",
                "concept fire premises",
                "concept count",
                "belief count"
        );


        table.print(System.out);

        RealDecisionTree tree = new RealDecisionTree(table, 0, 4,
                "LL", "MM", "HH");
        tree.print(System.out);

        DecisionTree.Node<Float> MAX = tree.max();
        DecisionTree.Node<Float> MIN = tree.min();
        System.out.println("MAX=" + MAX);
        System.out.println("MIN=" + MIN);


        tree.explanations().forEach((k, v) -> System.out.println(k + " " +
                v
        ));
    }
}
