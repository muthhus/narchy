package org.intelligentjava.machinelearning.decisiontree.data;

import org.intelligentjava.machinelearning.decisiontree.feature.Feature;
import org.intelligentjava.machinelearning.decisiontree.label.Label;

import java.util.Optional;

/**
 * Labeled training data sample.
 *
 * @author Ignas
 */
public interface DataSample {

    /**
     * Get sample data value from specified column.
     *
     * @return Data value.
     */
    Optional<Object> value(String column);

    /**
     * Assigned label of training data.
     *
     * @return Label.
     */
    Label label();

    /**
     * Syntactic sugar to check if data has feature.
     *
     * @param feature Feature.
     * @return True if data has feature and false otherwise.
     */
    default boolean has(Feature feature) {
        return feature.belongsTo(this);
    }

}
