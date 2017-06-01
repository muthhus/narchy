package org.intelligentjava.machinelearning.decisiontree;

import org.intelligentjava.machinelearning.decisiontree.data.DataSample;
import org.intelligentjava.machinelearning.decisiontree.feature.Feature;
import org.intelligentjava.machinelearning.decisiontree.impurity.GiniIndexImpurityCalculation;
import org.intelligentjava.machinelearning.decisiontree.impurity.ImpurityCalculationMethod;
import org.intelligentjava.machinelearning.decisiontree.label.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * Decision tree implementation.
 *
 * @author Ignas
 */
public class DecisionTree {

    /**
     * When data is considered homogeneous and node becomes leaf and is labeled. If it is equal 1.0 then absolutely all
     * data must be of the same label that node would be considered a leaf.
     */
    public static final double homogenityPercentage = 0.90;
    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(DecisionTree.class);
    /**
     * Impurity calculation method.
     */
    private final ImpurityCalculationMethod impurityCalculationMethod = new GiniIndexImpurityCalculation();
    /**
     * Max depth parameter. Growth of the tree is stopped once this depth is reached. Limiting depth of the tree can
     * help with overfitting, however if depth will be set too low tree will not be acurate.
     */
    private static final int maxDepth = 15;
    /**
     * Root node.
     */
    private Node root;

    protected static Label label(List<DataSample> data) {
        return label(data, homogenityPercentage);
    }

    /**
     * Returns Label if data is homogeneous.
     */
    protected static Label label(List<DataSample> data, double homogenityPercentage) {
        // group by to map <Label, count>
        Map<Label, Long> labelCount = data.stream().collect(groupingBy(DataSample::label, counting()));
        long totalCount = data.size();
        for (Map.Entry<Label, Long> labelLongEntry : labelCount.entrySet()) {
            long nbOfLabels = labelLongEntry.getValue();
            if ((nbOfLabels / (double) totalCount) >= homogenityPercentage) {
                return labelLongEntry.getKey();
            }
        }
        return null;
    }

    /**
     * Get root.
     */
    public Node getRoot() {
        return root;
    }

    /**
     * Trains tree on training data for provided features.
     *
     * @param trainingData List of training data samples.
     * @param features     List of possible features.
     */
    public void train(List<DataSample> trainingData, List<Feature> features) {
        root = growTree(trainingData, features, 1);
    }

    /**
     * Grow tree during training by splitting data recusively on best feature.
     *
     * @param trainingData List of training data samples.
     * @param features     List of possible features.
     * @return Node after split. For a first invocation it returns tree root node.
     */
    protected Node growTree(List<DataSample> trainingData, List<Feature> features, int currentDepth) {

        Label currentNodeLabel = null;
        // if dataset already homogeneous enough (has label assigned) make this node a leaf
        if ((currentNodeLabel = label(trainingData, homogenityPercentage)) != null) {
            log.debug("New leaf is created because data is homogeneous: {}", currentNodeLabel.name());
            return Node.newLeafNode(currentNodeLabel);
        }

        boolean stoppingCriteriaReached = features.isEmpty() || currentDepth >= maxDepth;
        if (stoppingCriteriaReached) {
            Label majorityLabel = getMajorityLabel(trainingData);
            log.debug("New leaf is created because stopping criteria reached: {}", majorityLabel.name());
            return Node.newLeafNode(majorityLabel);
        }

        Feature bestSplit = findBestSplitFeature(trainingData, features); // get best set of literals
        log.debug("Best split found: {}", bestSplit.toString());
        List<List<DataSample>> splitData = bestSplit.split(trainingData);
        log.debug("Data is split into sublists of sizes: {}", splitData.stream().map(List::size).collect(Collectors.toList()));

        // remove best split from features (TODO check if it is not slow)
        List<Feature> newFeatures = features.stream().filter(p -> !p.equals(bestSplit)).collect(toList());
        Node node = Node.newNode(bestSplit);
        for (List<DataSample> subsetTrainingData : splitData) { // add children to current node according to split
            if (subsetTrainingData.isEmpty()) {
                // if subset data is empty add a leaf with label calculated from initial data
                node.addChild(Node.newLeafNode(getMajorityLabel(trainingData)));
            } else {
                // grow tree further recursively
                node.addChild(growTree(subsetTrainingData, newFeatures, currentDepth + 1));
            }
        }

        return node;
    }

    /**
     * Classify dataSample.
     *
     * @param dataSample Data sample
     * @return Return label of class.
     */
    public Label classify(DataSample dataSample) {
        Node node = root;
        while (!node.isLeaf()) { // go through tree until leaf is reached
            // only binary splits for now - has feature first child node(left branch), does not have feature second child node(right branch).
            node = node.get(dataSample.has(node.getFeature()) ? 0 : 1);
        }
        return node.getLabel();
    }

    /**
     * Finds best feature to split on which is the one whose split results in lowest impurity measure.
     */
    protected Feature findBestSplitFeature(List<DataSample> data, List<Feature> features) {
        double currentImpurity = 1;
        Feature bestSplitFeature = null; // rename split to feature

        for (Feature feature : features) {
            List<List<DataSample>> splitData = feature.split(data);
            // totalSplitImpurity = sum(singleLeafImpurities) / nbOfLeafs
            // in other words splitImpurity is average of leaf impurities
            double calculatedSplitImpurity = splitData.stream().filter(list -> !list.isEmpty()).mapToDouble(impurityCalculationMethod::calculateImpurity).average().orElse(Double.POSITIVE_INFINITY);
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
    protected static Label getMajorityLabel(List<DataSample> data) {
        // group by to map <Label, count> like in getLabels() but return Label with most counts
        return data.stream().collect(groupingBy(DataSample::label, counting())).entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
    }

    // -------------------------------- TREE PRINTING ------------------------------------

    public void printTree() {
        printSubtree(root);
    }

    public void printSubtree(Node node) {
        if (!node.isEmpty() && node.get(0) != null) {
            printTree(node.get(0), true, "");
        }
        printNodeValue(node);
        if (node.size() > 1 && node.get(1) != null) {
            printTree(node.get(1), false, "");
        }
    }

    private static void printNodeValue(Node node) {
        if (node.isLeaf()) {
            System.out.print(node.getLabel());
        } else {
            System.out.print(node.getName());
        }
        System.out.println();
    }

    private static void printTree(Node node, boolean isRight, String indent) {
        if (!node.isEmpty() && node.get(0) != null) {
            printTree(node.get(0), true, indent + (isRight ? "        " : " |      "));
        }
        System.out.print(indent);
        if (isRight) {
            System.out.print(" /");
        } else {
            System.out.print(" \\");
        }
        System.out.print("----- ");
        printNodeValue(node);
        if (node.size() > 1 && node.get(1) != null) {
            printTree(node.get(1), false, indent + (isRight ? " |      " : "        "));
        }
    }
}
