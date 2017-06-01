package org.intelligentjava.machinelearning.decisiontree;

import jcog.list.FasterList;
import org.intelligentjava.machinelearning.decisiontree.feature.Feature;
import org.intelligentjava.machinelearning.decisiontree.label.Label;

public class Node extends FasterList<Node> {

    private static final String LEAF_NODE_NAME = "Leaf";

    /**
     * Node's feature used to split it further.
     */
    private final Feature feature;

    private Label label;

    private Node(Feature feature) {
        super();
        this.feature = feature;
    }

    private Node(Feature feature, Label label) {
        super();
        this.label = label;
        this.feature = feature;
    }

    public static Node newNode(Feature feature) {
        return new Node(feature);
    }

    public static Node newLeafNode(Label label) {
        return new Node(null, label);
    }

    public void addChild(Node child) {
        this.add(child);
    }


    public Label getLabel() {
        return label;
    }

    public boolean isLeaf() {
        return label != null;
    }

    public Feature getFeature() {
        return feature;
    }

    public String getName() {
        return feature != null ? feature.toString() : LEAF_NODE_NAME;
    }

}
