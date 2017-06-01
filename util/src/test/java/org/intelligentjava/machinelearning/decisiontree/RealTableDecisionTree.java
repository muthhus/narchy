package org.intelligentjava.machinelearning.decisiontree;

import jcog.learn.gng.Gasolinear;
import jcog.list.FasterList;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class RealTableDecisionTree extends DecisionTree<Integer, Float> {

    private final List<float[]> rows = new FasterList();
    private final List<NumericFeature> cols;

    class NumericFeature {

        final String name;
        final Gasolinear discretizer;
        private final int num;

        NumericFeature(int x, String name, int discretization) {
            this.num = x;
            this.name = name;
            this.discretizer = new Gasolinear(discretization);
        }

        public void learn(float x) {
            discretizer.put(x);
        }

        public Stream<Predicate<Function<Integer,Float>>> classifiers() {
            return IntStream.range(0, discretizer.node.length).mapToObj(
                    i -> new CentroidMatch(i)
            );
        }

        public class CentroidMatch implements Predicate<Function<Integer, Float>> {

            private final int v;

            CentroidMatch(int v) {
                this.v = v;
            }

            @Override
            public String toString() {
                return "col(" + name + ")~" + discretizer.node[v].getEntry(0);
            }

            @Override
            public boolean test(Function<Integer, Float> rr) {
                return discretizer.which(rr.apply(num)) == v;
            }
        }

    }


    public RealTableDecisionTree(int discretization, String... cols) {
        super();
        this.cols = IntStream.range(0, cols.length).mapToObj(x -> new NumericFeature(x, cols[x], discretization)).collect(toList());
    }

    public void add(float... row) {
        assert (row.length == cols.size());
        int i = 0;
        for (float x : row) {
            cols.get(i++).learn(x);
        }
        rows.add(row);
    }

    public void learn(int column) {
        learn(rows.stream(), column);
    }

    void learn(Stream<float[]> rows, int column) {

        //System.out.println(classifiers);

        put(column, rows.map((r)->(Function<Integer,Float>)(i -> r[i])).collect(toList()),

            //the classifiers from every non-target column
            cols.stream().
                filter(x -> x.num!=column).
                flatMap(NumericFeature::classifiers).
                collect(toList())
        );
    }


}
