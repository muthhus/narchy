package org.intelligentjava.machinelearning.decisiontree.feature;

import jcog.Texts;
import jcog.learn.gng.Gasolinear;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class DiscretizedScalarFeature {

    final String name;
    final Gasolinear discretizer;
    public final int num;

    public DiscretizedScalarFeature(int x, String name, int discretization) {
        this.num = x;
        this.name = name;
        this.discretizer = new Gasolinear(discretization);
    }

    public void learn(float x) {
        discretizer.put(x);
    }

    public Stream<Predicate<Function<Integer, Float>>> classifiers(@Nullable String... labels) {
        assert (labels == null || labels.length == 0 || labels.length == levels());
        return IntStream.range(0, levels()).mapToObj(
                labels != null && labels.length == levels() ?
                        i -> new CentroidMatch(i, labels[i]) :
                        i -> new CentroidMatch(i, null)
        );
    }

    public float value() {
        return (float) discretizer.node[num].getEntry(0);
    }

    protected int levels() {
        return discretizer.node.length;
    }

    class CentroidMatch implements Predicate<Function<Integer, Float>> {

        private final int v;
        private final String label;

        CentroidMatch(int v, String label) {
            this.v = v;
            this.label = label;
        }

        @Override
        public String toString() {
            double estimate = value();
            return name + "=" + ((label != null ? (label + "~") : "") + Texts.n4(estimate));
        }

        public double value() {
            return discretizer.node[v].getEntry(0);
        }

        @Override
        public boolean test(Function<Integer, Float> rr) {
            return discretizer.which(rr.apply(num)) == v;
        }
    }

}
