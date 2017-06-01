package org.intelligentjava.machinelearning.decisiontree.impurity;


import com.google.common.math.DoubleMath;
import org.intelligentjava.machinelearning.decisiontree.data.Value;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Entropy calculator. -p log2 p - (1 - p)log2(1 - p) - this is the expected information, in bits, conveyed by somebody
 * telling you the class of a randomly drawn example; the purer the set of examples, the more predictable this message
 * becomes and the smaller the expected information.
 *
 * @author Ignas
 */
public class EntropyCalculator implements ImpurityCalculator {

    /**
     * {@inheritDoc}
     */
    @Override
    public <L> double calculateImpurity(String value, List<Value<L>> splitData) {
        List<L> labels = splitData.stream().map((x) -> x.get(value)).distinct().collect(Collectors.toList());
        if (labels.size() > 1) {
            double p = ImpurityCalculator.getEmpiricalProbability(value, splitData, labels.get(0), labels.get(1)); // TODO fix to multiple labels
            return -1.0 * p * DoubleMath.log2(p) - ((1.0 - p) * DoubleMath.log2(1.0 - p));
        } else if (labels.size() == 1) {
            return 0.0; // if only one label data is pure
        } else {
            throw new IllegalStateException("This should never happen. Probably a bug.");
        }
    }

}
