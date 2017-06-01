package org.intelligentjava.machinelearning.decisiontree;

import static org.intelligentjava.machinelearning.decisiontree.data.SimpleValue.data;
import static org.intelligentjava.machinelearning.decisiontree.feature.PredicateFeature.feature;
import static org.intelligentjava.machinelearning.decisiontree.label.BooleanLabel.FALSE_LABEL;
import static org.intelligentjava.machinelearning.decisiontree.label.BooleanLabel.TRUE_LABEL;

import java.util.List;

import org.intelligentjava.machinelearning.decisiontree.data.Value;
import org.intelligentjava.machinelearning.decisiontree.feature.Feature;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;

public class DecisionTreeFindBestSplitTest {
    
    @Test
    public void testBooleanSplit() {
        DecisionTree<Object> tree = new DecisionTree();
        String labelColumnName = "answer";
        
        String[] headers = {labelColumnName, "x1", "x2"};
        List<Value<Object>> dataSet = Lists.newArrayList();
        dataSet.add(data(headers, TRUE_LABEL, true, true));
        dataSet.add(data(headers, FALSE_LABEL, true, false));
        dataSet.add(data(headers, FALSE_LABEL, false, true));
        dataSet.add(data(headers, FALSE_LABEL, false, false));
        
        List<Feature> features = Lists.newArrayList();
        features.add(feature("x1", true));
        features.add(feature("x2", true));
        features.add(feature("x1", false));
        features.add(feature("x2", false));
        
        // test finding split
        Feature bestSplit = tree.bestSplit(labelColumnName,dataSet, features);
        Assert.assertEquals("x1 = true", bestSplit.toString());
        
        List<List<Value<Object>>> split = bestSplit.split(dataSet);
        
        // test splitting data
        Assert.assertEquals(TRUE_LABEL, split.get(0).get(0).get(labelColumnName));
        Assert.assertEquals(FALSE_LABEL, split.get(0).get(1).get(labelColumnName));
        Assert.assertEquals(FALSE_LABEL, split.get(1).get(0).get(labelColumnName));
        Assert.assertEquals(FALSE_LABEL, split.get(1).get(1).get(labelColumnName));

        // next best split
        Feature newBestSplit = tree.bestSplit(labelColumnName,split.get(0), features);
        Assert.assertEquals("x2 = true", newBestSplit.toString());

        List<List<Value<Object>>> newSplit = newBestSplit.split(split.get(0));
        Assert.assertEquals(TRUE_LABEL, newSplit.get(0).get(0).get(labelColumnName));
        Assert.assertEquals(FALSE_LABEL, newSplit.get(1).get(0).get(labelColumnName));
    }

}
