package org.intelligentjava.machinelearning.decisiontree.data;

import org.intelligentjava.machinelearning.decisiontree.feature.Feature;

/**
 * Labeled training data sample.
 *
 * @author Ignas
 */
public interface Value<L>  {

    /**
     * Get sample data value from specified column.
     *
     * @return Data value.
     */
    L get(String column);



    /**
     * Syntactic sugar to check if data has feature.
     *
     * @param feature Feature.
     * @return True if data has feature and false otherwise.
     */
    default boolean has(Feature feature) {
        return feature.of(this);
    }

}
