package org.intelligentjava.machinelearning.decisiontree;

import com.google.common.collect.Lists;
import org.intelligentjava.machinelearning.decisiontree.data.DataSample;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static org.intelligentjava.machinelearning.decisiontree.label.BooleanLabel.FALSE_LABEL;
import static org.intelligentjava.machinelearning.decisiontree.label.BooleanLabel.TRUE_LABEL;

public class DecisionTreeGetLabelTest {

    @Test
    public void testGetLabelOnEmptyList() {
        DecisionTree tree = new DecisionTree();
        List<DataSample> data = Lists.newArrayList();
        Assert.assertNull(tree.label(data));
    }

    @Test
    public void testGetLabelOnSingleElement() {
        DecisionTree tree = new DecisionTree();
        List<DataSample> data = Lists.newArrayList();
        data.add(new TestDataSample(null, TRUE_LABEL));
        Assert.assertEquals("true", tree.label(data).name());
    }

    @Test
    public void testGetLabelOnTwoDifferent() {
        DecisionTree tree = new DecisionTree();
        List<DataSample> data = Lists.newArrayList();
        data.add(new TestDataSample(null, TRUE_LABEL));
        data.add(new TestDataSample(null, FALSE_LABEL));
        Assert.assertNull(tree.label(data));
    }

    @Test
    public void testGetLabelOn95vs5() {
        DecisionTree tree = new DecisionTree();
        List<DataSample> data = Lists.newArrayList();
        for (int i = 0; i < 95; i++) {
            data.add(new TestDataSample(null, TRUE_LABEL));
        }
        for (int i = 0; i < 5; i++) {
            data.add(new TestDataSample(null, FALSE_LABEL));
        }
        Assert.assertEquals("true", tree.label(data).name());
    }

    @Test
    public void testGetLabelOn94vs6() {
        DecisionTree tree = new DecisionTree();

        {
            List<DataSample> homogenous = buildSample(96, 4);
            Assert.assertNotNull(tree.label(homogenous));
        }


        {
            List<DataSample> nonhomogenous = buildSample(50, 50);
            Assert.assertNull(tree.label(nonhomogenous));
        }
    }

    static List<DataSample> buildSample(int a, int b) {
        List<DataSample> homogenous = Lists.newArrayList();
        for (int i = 0; i < a; i++)
            homogenous.add(new TestDataSample(null, TRUE_LABEL));
        for (int i = 0; i < b; i++)
            homogenous.add(new TestDataSample(null, FALSE_LABEL));
        return homogenous;
    }

}
