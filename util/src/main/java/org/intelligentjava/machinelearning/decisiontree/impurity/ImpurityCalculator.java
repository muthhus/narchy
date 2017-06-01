package org.intelligentjava.machinelearning.decisiontree.impurity;

import org.intelligentjava.machinelearning.decisiontree.data.Value;

import java.util.List;

/**
 * Impurity calculation method of decision tree. It is used during training while trying to find best split. For example
 * one split results in 5 positive and 5 negative labels and another in 9 positive and 1 negative. Impurity calculator
 * should return high value in the first case and low value in the second. That means that during training we will
 * prefer second split as that will provide most information. Most popular methods are entropy and gini index.
 * <p>
 * Annotated as functional interface to allow lambda usage.
 *
 * @author Ignas
 */
@FunctionalInterface
public interface ImpurityCalculator {

    /**
     * Calculates impurity value. High impurity implies low information gain and more random labels of data which in
     * turn means that split is not very good.
     *
     *
     * @param value
     * @param splitData Data subset on which impurity is calculated.
     * @return Impurity.
     */
    <L> double calculateImpurity(String value, List<Value<L>> splitData);


    /**
     * Calculate and return empirical probability of positive class. p+ = n+ / (n+ + n-).
     *
     * @param splitData Data on which positive label probability is calculated.
     * @return Empirical probability.
     */
    static <L> double getEmpiricalProbability(String value, List<Value<L>> splitData, L positive, L negative) {
        // TODO cache calculated counts
        return (double) splitData.stream().filter(d -> d.get(value).equals(positive)).count() / splitData.size();
    }
}
