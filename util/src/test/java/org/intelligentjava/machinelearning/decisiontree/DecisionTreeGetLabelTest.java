package org.intelligentjava.machinelearning.decisiontree;

import com.google.common.collect.Lists;
import org.intelligentjava.machinelearning.decisiontree.data.Value;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static org.intelligentjava.machinelearning.decisiontree.label.BooleanLabel.FALSE_LABEL;
import static org.intelligentjava.machinelearning.decisiontree.label.BooleanLabel.TRUE_LABEL;

public class DecisionTreeGetLabelTest {

    @Test
    public void testGetLabelOnEmptyList() {
        DecisionTree tree = new DecisionTree();
        List<Value> data = Lists.newArrayList();
        Assert.assertNull(tree.label(null, data));
    }

    @Test
    public void testGetLabelOnSingleElement() {
        DecisionTree tree = new DecisionTree();
        List<Value> data = Lists.newArrayList();
        data.add(new TestValue(TRUE_LABEL));
        Assert.assertEquals("true", tree.label(null, data).toString());
    }

    @Test
    public void testGetLabelOnTwoDifferent() {
        DecisionTree tree = new DecisionTree();
        List<Value> data = Lists.newArrayList();
        data.add(new TestValue(TRUE_LABEL));
        data.add(new TestValue(FALSE_LABEL));
        Assert.assertNull(tree.label(null, data));
    }

    @Test
    public void testGetLabelOn95vs5() {
        DecisionTree tree = new DecisionTree();
        List<Value> data = Lists.newArrayList();
        for (int i = 0; i < 95; i++) {
            data.add(new TestValue(TRUE_LABEL));
        }
        for (int i = 0; i < 5; i++) {
            data.add(new TestValue(FALSE_LABEL));
        }
        Assert.assertEquals("true", tree.label(null, data).toString());
    }

    @Test
    public void testGetLabelOn94vs6() {
        DecisionTree tree = new DecisionTree();

        {
            List<Value> homogenous = buildSample(96, 4);
            Assert.assertNotNull(tree.label(null, homogenous));
        }


        {
            List<Value> nonhomogenous = buildSample(50, 50);
            Assert.assertNull(tree.label(null, nonhomogenous));
        }
    }

    static List<Value> buildSample(int a, int b) {
        List<Value> homogenous = Lists.newArrayList();
        for (int i = 0; i < a; i++)
            homogenous.add(new TestValue(TRUE_LABEL));
        for (int i = 0; i < b; i++)
            homogenous.add(new TestValue(FALSE_LABEL));
        return homogenous;
    }

}
