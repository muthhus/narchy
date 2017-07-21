package nars;

import com.google.common.graph.ValueGraph;
import nars.nal.AbstractNALTest;
import nars.nal.nal2.NAL2Test;
import nars.util.OptiUnit;
import org.intelligentjava.machinelearning.decisiontree.DecisionTree;
import org.intelligentjava.machinelearning.decisiontree.FloatTable;
import org.intelligentjava.machinelearning.decisiontree.RealDecisionTree;

import java.util.SortedMap;

public class Repair {


    public static void main(String[] args) {

        Class[] testClasses = {
                //NAL1Test.class, NAL2Test.class,
                NAL2Test.class
                //AllNAL.class
        };

        OptiUnit<AbstractNALTest> o = new OptiUnit<AbstractNALTest>((x) -> {

            //OBSERVE AND COLLECT EXPERIMENT RESULTS
            SortedMap<String, Object> stat = x.nar.stats();
            stat.put("score", x.test.score);

            return stat;

        }, testClasses);

        for (int ttl : new int[]{32, 64, 96, 128, 192}) {
            /*for (int termVol : new int[]{16})*/
            o.run((x) -> {

                //SETUP EXPERIMENT
                x.test.trace = false;

                return new OptiUnit.Tweaks<>(x)

                        //.set("cycles", 100)
                        .call("nar.matchTTL.setValue", ttl)
                        //.call("nar.termVolumeMax.setValue", termVol)
                        ;

            });
        }

        o.print(System.out);

        FloatTable<String> table = o.table(
                "nar.matchTTL.setValue(",
                //"nar.termVolumeMax.setValue(",
                //"concept fire activates",
                //"concept fire premises",
                "concept fire premises",
                "concept fire activations",
                "concept count",
                "belief count",
                "score");


        table.print(System.out);

        RealDecisionTree tree = new RealDecisionTree(table, 5, 8,
                "LL", "HH");
        tree.print(System.out);

        ValueGraph<DecisionTree.Node<Float>, Boolean> g = tree.graph();
        System.out.println(g);

        //Network<DecisionTree.Node<Float>, String> h = Graphs.transpose(g);
        //Graph<DecisionTree.Node<Float>> gg = Graphs.transitiveClosure(g);

        DecisionTree.Node<Float> MAX = tree.max();
        System.out.println("MAX=" + MAX);
        DecisionTree.Node<Float> MIN = tree.min();
        System.out.println("MIN=" + MIN);



        g.edges().forEach(k -> {

            DecisionTree.Node<Float> s = k.source();
            DecisionTree.Node<Float> t = k.target();
            //g.edgeValue(
            System.out.println(s + " "  + k+ " " + t);

        });

        System.out.println();

        //ValueGraph<DecisionTree.Node<Float>,Boolean> sg = Graphs.inducedSubgraph(Graphs.transpose(g), List.of(MAX));

        for (DecisionTree.Node<Float> tgt : new DecisionTree.Node[] { MAX,MIN } ){
            g.predecessors(tgt).forEach(k -> {
                boolean b = g.edgeValue(k, tgt);
                String ks = "\"" + k + "\"";
                System.out.println("(" + (b ? ks : ("--" + k)) + " ==> score(" + tgt + "))");
            });
        }

        System.out.println();


        /* TODO
            traverse the tree collecting each node as an AND condition until a leaf node is reached. this becomes
            the predicting predicate of the value at the leaf node. so a stack is necessary
         */

    }
}
