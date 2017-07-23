package nars;

import com.google.common.graph.ValueGraph;
import nars.nal.AbstractNALTest;
import nars.nal.nal1.NAL1Test;
import nars.nal.nal2.NAL2Test;
import nars.nal.nal3.NAL3Test;
import nars.nal.nal4.NAL4MultistepTest;
import nars.nar.exe.FocusedExecutioner;
import nars.util.OptiUnit;
import org.intelligentjava.machinelearning.decisiontree.DecisionTree;
import org.intelligentjava.machinelearning.decisiontree.FloatTable;
import org.intelligentjava.machinelearning.decisiontree.RealDecisionTree;

import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

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


        for (int subCycles : new int[]{ 2 }) {
            for (int ttl : new int[]{ 4, 16, 32, 64, 128 }) {
            /*for (int termVol : new int[]{16})*/
                o.add((x) -> {

                    //SETUP EXPERIMENT
                    x.test.trace = false;


                    return new OptiUnit.Tweaks<>(x)

                            //.set("cycles", 100)
                            .call("subCycles", (n, v) -> {
                                ((FocusedExecutioner)(n.nar.exe)).subCycles = v;
                            }, subCycles)
                            .call("nar.matchTTL.setValue", ttl)
                            //.call("nar.termVolumeMax.setValue", termVol)
                            ;

                });
            }
        }

        o.run();

        o.print(System.out);

        FloatTable<String> table = o.table(
                "score",
                //"subCycles",
                "nar.matchTTL.setValue"
                //"nar.termVolumeMax.setValue(",
                //"concept fire activations",
                //"concept fire premises",
                //"concept fire premises",
                //"concept fire activations",
                //"concept count"
                //"belief count",
                );


        table.print(System.out);

        RealDecisionTree tree = new RealDecisionTree(table, 0, 4,
                "LL", "MM", "HH");
        tree.print(System.out);

        DecisionTree.Node<Float> MAX = tree.max();
        DecisionTree.Node<Float> MIN = tree.min();
        System.out.println("MAX=" + MAX);
        System.out.println("MIN=" + MIN);


        tree.explanations().forEach((k,v) -> System.out.println(k + " " +
                v.toString()
        ));





    }
}
