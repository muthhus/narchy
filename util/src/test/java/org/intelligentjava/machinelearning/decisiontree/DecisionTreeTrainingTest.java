package org.intelligentjava.machinelearning.decisiontree;

import org.intelligentjava.machinelearning.decisiontree.data.SimpleValue;
import org.intelligentjava.machinelearning.decisiontree.feature.Feature;
import org.intelligentjava.machinelearning.decisiontree.feature.P;
import org.intelligentjava.machinelearning.decisiontree.label.BooleanLabel;
import org.junit.Test;

import java.util.Arrays;

import static java.util.Arrays.asList;
import static org.intelligentjava.machinelearning.decisiontree.data.SimpleValue.classification;
import static org.intelligentjava.machinelearning.decisiontree.data.SimpleValue.data;
import static org.intelligentjava.machinelearning.decisiontree.feature.PredicateFeature.feature;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DecisionTreeTrainingTest {

    /**
     * Test if decision tree correctly learns simple AND function.
     * Should learn tree like this:
     * x1 = true
     * /       \
     * yes       No
     * /           \
     * x2 = true      LABEL_FALSE
     * /    \
     * yes     No
     * /         \
     * LABEL_TRUE    LABEL_FALSE
     */
    @Test
    public void testTrainingAndFunction() {
        DecisionTree<Object> tree = new DecisionTree();
        String[] header = {"x1", "x2", "answer"};

        SimpleValue data1 = data(header, Boolean.TRUE, Boolean.TRUE, BooleanLabel.TRUE_LABEL);
        SimpleValue data2 = data(header, Boolean.TRUE, Boolean.FALSE, BooleanLabel.FALSE_LABEL);
        SimpleValue data3 = data(header, Boolean.FALSE, Boolean.TRUE, BooleanLabel.FALSE_LABEL);
        SimpleValue data4 = data(header, Boolean.FALSE, Boolean.FALSE, BooleanLabel.FALSE_LABEL);

        Feature feature1 = feature("x1", Boolean.TRUE);
        Feature feature2 = feature("x1", Boolean.FALSE);
        Feature feature3 = feature("x2", Boolean.TRUE);
        Feature feature4 = feature("x2", Boolean.FALSE);

        tree.learn("answer", asList(data1, data2, data3, data4), asList(feature1, feature2, feature3, feature4));

        assertEquals("x1 = true", tree.root().toString()); // root node x1 = true split
        assertEquals(null, tree.root().label); // not leaf node

        assertEquals("x2 = true", tree.root().get(0).toString());
        assertEquals(null, tree.root().get(0).label); // not leaf node
        assertEquals("Leaf", tree.root().get(0).get(0).toString()); // leaf
        assertEquals(BooleanLabel.TRUE_LABEL, tree.root().get(0).get(0).label);
        assertEquals("Leaf", tree.root().get(0).get(1).toString()); // leaf
        assertEquals(BooleanLabel.FALSE_LABEL, tree.root().get(0).get(1).label);

        assertEquals("Leaf", tree.root().get(1).toString());
        assertEquals(BooleanLabel.FALSE_LABEL, tree.root().get(1).label);

    }


    /**
     * Test if decision tree correctly learns simple OR function.
     * Should learn tree like this:
     * x1 = true
     * /       \
     * yes       No
     * /           \
     * LABEL_TRUE     x2 = true
     * /    \
     * yes     No
     * /         \
     * LABEL_TRUE    LABEL_FALSE
     */
    @Test
    public void testTrainingORFunction() {
        DecisionTree<Object> tree = new DecisionTree();
        String[] header = {"x1", "x2", "answer"};

        SimpleValue data1 = data(header, Boolean.TRUE, Boolean.TRUE, BooleanLabel.TRUE_LABEL);
        SimpleValue data2 = data(header, Boolean.TRUE, Boolean.FALSE, BooleanLabel.TRUE_LABEL);
        SimpleValue data3 = data(header, Boolean.FALSE, Boolean.TRUE, BooleanLabel.TRUE_LABEL);
        SimpleValue data4 = data(header, Boolean.FALSE, Boolean.FALSE, BooleanLabel.FALSE_LABEL);

        Feature feature1 = feature("x1", Boolean.TRUE);
        Feature feature2 = feature("x1", Boolean.FALSE);
        Feature feature3 = feature("x2", Boolean.TRUE);
        Feature feature4 = feature("x2", Boolean.FALSE);

        tree.learn("answer", asList(data1, data2, data3, data4), asList(feature1, feature2, feature3, feature4));

        assertEquals("x1 = true", tree.root().toString()); // root node x1 = true split
        assertEquals(null, tree.root().label); // not leaf node

        assertEquals("Leaf", tree.root().get(0).toString());
        assertEquals(BooleanLabel.TRUE_LABEL, tree.root().get(0).label);

        assertEquals("x2 = true", tree.root().get(1).toString());
        assertEquals(null, tree.root().get(1).label);
        assertEquals("Leaf", tree.root().get(1).get(0).toString()); // leaf
        assertEquals(BooleanLabel.TRUE_LABEL, tree.root().get(1).get(0).label);
        assertEquals("Leaf", tree.root().get(1).get(1).toString()); // leaf
        assertEquals(BooleanLabel.FALSE_LABEL, tree.root().get(1).get(1).label);

    }


    /**
     * Test if decision tree correctly learns simple XOR function.
     * Should learn tree like this:
     * x1 = true
     * /       \
     * yes       No
     * /           \
     * x2 = true        x2 = true
     * /    \              /    \
     * yes     No          yes     No
     * /         \          /         \
     * LABEL_FALSE LABEL_TRUE  LABEL_TRUE LABEL_FALSE
     */
    @Test
    public void testTrainingXORFunction() {
        DecisionTree<Object> tree = new DecisionTree();
        String[] header = {"x1", "x2", "answer"};

        SimpleValue data1 = data(header, Boolean.TRUE, Boolean.TRUE, BooleanLabel.FALSE_LABEL);
        SimpleValue data2 = data(header, Boolean.TRUE, Boolean.FALSE, BooleanLabel.TRUE_LABEL);
        SimpleValue data3 = data(header, Boolean.FALSE, Boolean.TRUE, BooleanLabel.TRUE_LABEL);
        SimpleValue data4 = data(header, Boolean.FALSE, Boolean.FALSE, BooleanLabel.FALSE_LABEL);

        Feature feature1 = feature("x1", Boolean.TRUE);
        Feature feature2 = feature("x1", Boolean.FALSE);
        Feature feature3 = feature("x2", Boolean.TRUE);
        Feature feature4 = feature("x2", Boolean.FALSE);

        tree.learn("answer", asList(data1, data2, data3, data4), asList(feature1, feature2, feature3, feature4));
        tree.print();

        assertEquals("x1 = true", tree.root().toString()); // root node x1 = true split
        assertNull(tree.root().label); // not leaf node

        assertEquals("x2 = true", tree.root().get(0).toString());
        assertEquals(null, tree.root().get(0).label);
        assertEquals("Leaf", tree.root().get(1).get(0).toString()); // leaf
        assertEquals(BooleanLabel.FALSE_LABEL, tree.root().get(0).get(0).label);
        assertEquals("Leaf", tree.root().get(1).get(0).toString()); // leaf
        assertEquals(BooleanLabel.TRUE_LABEL, tree.root().get(0).get(1).label);

        assertEquals("x2 = true", tree.root().get(1).toString());
        assertEquals(null, tree.root().get(1).label);
        assertEquals("Leaf", tree.root().get(1).get(0).toString()); // leaf
        assertEquals(BooleanLabel.TRUE_LABEL, tree.root().get(1).get(0).label);
        assertEquals("Leaf", tree.root().get(1).get(1).toString()); // leaf
        assertEquals(BooleanLabel.FALSE_LABEL, tree.root().get(1).get(1).label);

    }

    @Test
    public void testLearnSimpleMoreLessFeature() {
        DecisionTree<Object> tree = new DecisionTree();
        String[] header = {"x1", "answer"};

        tree.learn(
                "answer",
                asList(
                        data(header, 1, BooleanLabel.FALSE_LABEL),
                        data(header, 2, BooleanLabel.FALSE_LABEL),
                        data(header, 3, BooleanLabel.TRUE_LABEL),
                        data(header, 4, BooleanLabel.TRUE_LABEL)),
                asList(
                        feature("x1", P.moreThan(0), "> 0"),
                        feature("x1", P.moreThan(1), "> 1"),
                        feature("x1", P.moreThan(2), "> 2"))
        );

        tree.print();

        assertEquals("x1 > 2", tree.root().toString()); // root node x1 = true split
        assertEquals(null, tree.root().label); // not leaf node

        assertEquals("Leaf", tree.root().get(0).toString()); // leaf
        assertEquals(BooleanLabel.TRUE_LABEL, tree.root().get(0).label);
        assertEquals("Leaf", tree.root().get(1).toString()); // leaf
        assertEquals(BooleanLabel.FALSE_LABEL, tree.root().get(1).label);


    }

    /**
     * Test classify function which finds path in decision tree to leaf node.
     *
     * @author Ignas
     */
    @Test
    public void testClassify() {

        // train AND function on decision tree
        DecisionTree tree = new DecisionTree();
        String[] header = {"x1", "x2", "answer"};

        SimpleValue data1 = data(header, Boolean.TRUE, Boolean.TRUE, BooleanLabel.TRUE_LABEL);
        SimpleValue data2 = data(header, Boolean.TRUE, Boolean.FALSE, BooleanLabel.FALSE_LABEL);
        SimpleValue data3 = data(header, Boolean.FALSE, Boolean.TRUE, BooleanLabel.FALSE_LABEL);
        SimpleValue data4 = data(header, Boolean.FALSE, Boolean.FALSE, BooleanLabel.FALSE_LABEL);

        Feature feature1 = feature("x1", Boolean.TRUE);
        Feature feature2 = feature("x1", Boolean.FALSE);
        Feature feature3 = feature("x2", Boolean.TRUE);
        Feature feature4 = feature("x2", Boolean.FALSE);

        tree.learn("answer",
                asList(data1, data2, data3, data4),
                asList(feature1, feature2, feature3, feature4));

        // now check classify
        String[] classificationHeader = {"x1", "x2"};
        assertEquals(BooleanLabel.TRUE_LABEL, tree.classify(classification(classificationHeader, Boolean.TRUE, Boolean.TRUE)));
        assertEquals(BooleanLabel.FALSE_LABEL, tree.classify(classification(classificationHeader, Boolean.TRUE, Boolean.FALSE)));
        assertEquals(BooleanLabel.FALSE_LABEL, tree.classify(classification(classificationHeader, Boolean.FALSE, Boolean.TRUE)));
        assertEquals(BooleanLabel.FALSE_LABEL, tree.classify(classification(classificationHeader, Boolean.FALSE, Boolean.FALSE)));
    }

//        @Test
//    public void testJSON() {
//        DecisionTree<Object> tree = new DecisionTree();
//        String[] header = {"x", "answer"};
//
//        String json = "[ { x: 1, answer: false }, { x: 2, answer: false }, { x: 3, answer: true }, { x: 4, answer: true }]";
//
//        tree.learn(
//                Util.jsonNode(json)
//                asList(
//                        data("answer", header, 1, BooleanLabel.FALSE_LABEL),
//                        data("answer", header, 2, BooleanLabel.FALSE_LABEL),
//                        data("answer", header, 3, BooleanLabel.TRUE_LABEL),
//                        data("answer", header, 4, BooleanLabel.TRUE_LABEL)),
//                asList(
//                        feature("x", P.moreThan(0), "> 0"),
//                        feature("x", P.moreThan(1), "> 1"),
//                        feature("x", P.moreThan(2), "> 2"))
//        );
//
//        tree.print();
//
//        assertEquals("x > 2", tree.root().toString()); // root node x = true split
//        assertEquals(null, tree.root().label); // not leaf node
//
//        assertEquals("Leaf", tree.root().get(0).toString()); // leaf
//        assertEquals(BooleanLabel.TRUE_LABEL, tree.root().get(0).label);
//        assertEquals("Leaf", tree.root().get(1).toString()); // leaf
//        assertEquals(BooleanLabel.FALSE_LABEL, tree.root().get(1).label);
//
//
//    }

}
