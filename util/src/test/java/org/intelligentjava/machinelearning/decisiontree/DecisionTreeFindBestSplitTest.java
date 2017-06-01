package org.intelligentjava.machinelearning.decisiontree;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;
import static org.intelligentjava.machinelearning.decisiontree.data.SimpleValue.data;
import static org.intelligentjava.machinelearning.decisiontree.feature.PredicateFeature.feature;
import static org.intelligentjava.machinelearning.decisiontree.label.BooleanLabel.FALSE_LABEL;
import static org.intelligentjava.machinelearning.decisiontree.label.BooleanLabel.TRUE_LABEL;

public class DecisionTreeFindBestSplitTest {
    
    @Test
    public void testBooleanSplit() {
        DecisionTree<String, Object> tree = new DecisionTree();
        String labelColumnName = "answer";
        
        String[] headers = {labelColumnName, "x1", "x2"};
        List<Function<String,Object>> dataSet = Lists.newArrayList();
        dataSet.add(data(headers, TRUE_LABEL, true, true));
        dataSet.add(data(headers, FALSE_LABEL, true, false));
        dataSet.add(data(headers, FALSE_LABEL, false, true));
        dataSet.add(data(headers, FALSE_LABEL, false, false));
        
        List<Predicate<Function<String,Object>>> features = Lists.newArrayList();
        features.add(feature("x1", true));
        features.add(feature("x2", true));
        features.add(feature("x1", false));
        features.add(feature("x2", false));
        
        // test finding split
        Predicate<Function<String,Object>> bestSplit = tree.bestSplit(labelColumnName,dataSet, features);
        Assert.assertEquals("x1 = true", bestSplit.toString());
        
        List<List<Function<String,Object>>> split = DecisionTree.split(bestSplit, dataSet).collect(toList());
        
        // test splitting data
        Assert.assertEquals(TRUE_LABEL, split.get(0).get(0).apply(labelColumnName));
        Assert.assertEquals(FALSE_LABEL, split.get(0).get(1).apply(labelColumnName));
        Assert.assertEquals(FALSE_LABEL, split.get(1).get(0).apply(labelColumnName));
        Assert.assertEquals(FALSE_LABEL, split.get(1).get(1).apply(labelColumnName));

        // next best split
        Predicate<Function<String,Object>> newBestSplit = tree.bestSplit(labelColumnName,split.get(0), features);
        Assert.assertEquals("x2 = true", newBestSplit.toString());

        List<List<Function<String,Object>>> newSplit = DecisionTree.split(newBestSplit, split.get(0)).collect(toList());
        Assert.assertEquals(TRUE_LABEL, newSplit.get(0).get(0).apply(labelColumnName));
        Assert.assertEquals(FALSE_LABEL, newSplit.get(1).get(0).apply(labelColumnName));
    }

}
