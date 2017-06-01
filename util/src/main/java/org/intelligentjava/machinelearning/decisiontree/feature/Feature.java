package org.intelligentjava.machinelearning.decisiontree.feature;

import com.google.common.collect.Lists;
import org.intelligentjava.machinelearning.decisiontree.data.Value;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.partitioningBy;

/**
 * Feature interface. Each data sample either have or does not have a feature and it can be split based on that.
 *
 * @author Ignas
 */
public interface Feature {

    /**
     * Calculates and checks if data contains feature.
     *
     * @param x Data sample.
     * @return true if data has this feature and false otherwise.
     */
    boolean of(Value x);

    /**
     * Split data according to if it has this feature.
     *
     * @param data Data to by split by this feature.
     * @return Sublists of split data samples.
     */
    default <L> List<List<Value<L>>> split(Collection<Value<L>> data) {
        // TODO:  maybe use sublist streams instead of creating new list just track indexes
        // http://stackoverflow.com/questions/22917270/how-to-get-a-range-of-items-from-stream-using-java-8-lambda
        Map<Boolean, List<Value<L>>> split = data.stream().collect(partitioningBy(this::of));

        return Lists.newArrayList(split.get(true), split.get(false));
    }

}
