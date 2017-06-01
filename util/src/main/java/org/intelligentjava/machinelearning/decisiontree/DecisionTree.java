package org.intelligentjava.machinelearning.decisiontree;

import com.google.common.collect.Iterables;
import jcog.list.FasterList;
import org.intelligentjava.machinelearning.decisiontree.data.Value;
import org.intelligentjava.machinelearning.decisiontree.feature.Feature;
import org.intelligentjava.machinelearning.decisiontree.impurity.GiniIndexImpurityCalculation;
import org.intelligentjava.machinelearning.decisiontree.impurity.ImpurityCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.intelligentjava.machinelearning.decisiontree.DecisionTree.Node.leaf;

/**
 * Decision tree implementation.
 *
 * @author Ignas
 */
public class DecisionTree<L> {

    /**
     * When data is considered homogeneous and node becomes leaf and is labeled. If it is equal 1.0 then absolutely all
     * data must be of the same label that node would be considered a leaf.
     */
    public static final double homogenityPercentage = 0.90;
    /**
     * Logger.
     */
    private static final Logger log = LoggerFactory.getLogger(DecisionTree.class);
    /**
     * Impurity calculation method.
     */
    private final ImpurityCalculator impurityCalculator = new GiniIndexImpurityCalculation();
    /**
     * Max depth parameter. Growth of the tree is stopped once this depth is reached. Limiting depth of the tree can
     * help with overfitting, however if depth will be set too low tree will not be acurate.
     */
    private static final int maxDepth = 15;
    /**
     * Root node.
     */
    private Node<L> root;

    protected L label(String value, List<Value<L>> data) {
        return label(value, data, homogenityPercentage);
    }

    /**
     * Returns Label if data is homogeneous.
     */
    protected static <L> L label(String value, Collection<Value<L>> data, double homogenityPercentage) {
        // group by to map <Label, count>
        Map<L, Long> labelCount = data.stream().collect(groupingBy((x)->x.get(value), counting()));
        long totalCount = data.size();
        for (Map.Entry<L, Long> e : labelCount.entrySet()) {
            long nbOfLabels = e.getValue();
            if ((nbOfLabels / (double) totalCount) >= homogenityPercentage) {
                return e.getKey();
            }
        }
        return null;
    }

    /**
     * Get root.
     */
    public Node<L> root() {
        return root;
    }

    /**
     * Trains tree on training data for provided features.
     *
     * @param value        The value column being learned
     * @param trainingData List of training data samples.
     * @param features     List of possible features.
     */
    public void learn(String value, List<Value<L>> trainingData, List<Feature> features) {
        root = learn(value, trainingData, features, 1);
    }

    /**
     * Grow tree during training by splitting data recusively on best feature.
     *
     * @param trainingData List of training data samples.
     * @param features     List of possible features.
     * @return Node after split. For a first invocation it returns tree root node.
     */
    protected Node<L> learn(String value, List<Value<L>> trainingData, List<Feature> features, int currentDepth) {

        // if dataset already homogeneous enough (has label assigned) make this node a leaf
        L currentNodeLabel;
        if ((currentNodeLabel = label(value, trainingData, homogenityPercentage)) != null) {
            return leaf(currentNodeLabel); //log.debug("New leaf is created because data is homogeneous: {}", currentNodeLabel.name());
        }

        int fs = features.size();
        boolean stoppingCriteriaReached = (fs==0) || currentDepth >= maxDepth;
        if (stoppingCriteriaReached) {
            return leaf(majority(value, trainingData)); //log.debug("New leaf is created because stopping criteria reached: {}", majorityLabel.name());
        }

        Feature split = bestSplit(value, trainingData, features); // get best set of literals
        //log.debug("Best split found: {}", bestSplit.toString());

        // add children to current node according to split
        // if subset data is empty add a leaf with label calculated from initial data
        // else grow tree further recursively

        //log.debug("Data is split into sublists of sizes: {}", splitData.stream().map(List::size).collect(Collectors.toList()));
        return split.split(trainingData).stream().map(

                subsetTrainingData -> subsetTrainingData.isEmpty() ?

                    leaf(majority(value, trainingData))

                        :

                    learn(value, subsetTrainingData,
                            new FasterList<>(Iterables.filter(features, p -> !p.equals(split)), fs - 1),
                        currentDepth + 1))

                .collect(Collectors.toCollection(()->Node.feature(split)));
    }

    /**
     * Classify dataSample.
     *
     * @param value Data sample
     * @return Return label of class.
     */
    public L classify(Value value) {
        Node<L> node = root;
        while (!node.isLeaf()) { // go through tree until leaf is reached
            // only binary splits for now - has feature first child node(left branch), does not have feature second child node(right branch).
            node = node.get(value.has(node.feature) ? 0 : 1);
        }
        return node.label;
    }

    /**
     * Finds best feature to split on which is the one whose split results in lowest impurity measure.
     */
    protected Feature bestSplit(String value, Collection<Value<L>> data, Iterable<? extends Feature> features) {
        double currentImpurity = 1;
        Feature bestSplitFeature = null; // rename split to feature

        for (Feature feature : features) {

            // totalSplitImpurity = sum(singleLeafImpurities) / nbOfLeafs
            // in other words splitImpurity is average of leaf impurities
            double calculatedSplitImpurity =
                    feature.split(data).stream().filter(list -> !list.isEmpty()).mapToDouble(splitData -> impurityCalculator.calculateImpurity(value, splitData)).average().orElse(Double.POSITIVE_INFINITY);
            if (calculatedSplitImpurity < currentImpurity) {
                currentImpurity = calculatedSplitImpurity;
                bestSplitFeature = feature;
            }
        }

        return bestSplitFeature;
    }

    /**
     * Differs from getLabel() that it always return some label and does not look at homogenityPercentage parameter. It
     * is used when tree growth is stopped and everything what is left must be classified so it returns majority label for the data.
     */
    static <L> L majority(String value, Collection<Value<L>> data) {
        // group by to map <Label, count> like in getLabels() but return Label with most counts
        return data.stream().collect(groupingBy((x)->x.get(value), counting())).entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
    }

    // -------------------------------- TREE PRINTING ------------------------------------

    public void print() {
        print(System.out);
    }

    public void print(PrintStream o) {
        printSubtree(root, o);
    }

    private void printSubtree(Node<L> node, PrintStream o) {
        if (!node.isEmpty() && node.get(0) != null) {
            print(node.get(0), true, "", o);
        }
        print(node, o);
        if (node.size() > 1 && node.get(1) != null) {
            print(node.get(1), false, "", o);
        }
    }

    private static <L> void print(Node<L> node, PrintStream o) {
        o.print(node);
        o.println();
    }

    private static <L> void print(Node<L> node, boolean isRight, String indent, PrintStream o) {
        if (!node.isEmpty() && node.get(0) != null) {
            print(node.get(0), true, indent + (isRight ? "        " : " |      "), o);
        }
        o.print(indent);
        if (isRight) {
            o.print(" /");
        } else {
            o.print(" \\");
        }
        o.print("----- ");
        print(node, o);
        if (node.size() > 1 && node.get(1) != null) {
            print(node.get(1), false, indent + (isRight ? " |      " : "        "), o);
        }
    }

    static class Node<L> extends FasterList<Node<L>> {

        private static final String LEAF_NODE_NAME = "Leaf";

        /**
         * Node's feature used to split it further.
         */
        public final Feature feature;

        public final L label;

        Node(Feature feature) {
            super();
            this.feature = feature;
            this.label = null;
        }

        private Node(Feature feature, L label) {
            super();
            this.label = label;
            this.feature = feature;
        }

        public static <L> Node<L> feature(Feature feature) {
            return new Node<>(feature);
        }

        public static <L> Node<L> leaf(L label) {
            return new Node<>(null, label);
        }

        public boolean isLeaf() {
            return label != null;
        }

        public String toString() {
            return feature != null ? feature.toString() : LEAF_NODE_NAME;
        }

    }
}
