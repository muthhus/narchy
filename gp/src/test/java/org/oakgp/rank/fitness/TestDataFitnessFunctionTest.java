/*
 * Copyright 2015 S. Webber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.oakgp.rank.fitness;

import org.junit.jupiter.api.Test;
import org.oakgp.Assignments;
import org.oakgp.node.Node;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.oakgp.TestUtils.mockNode;

public class TestDataFitnessFunctionTest {
    @Test
    public void testDefaultRankingFunction() {
        // test data
        Map<Assignments, Integer> testData = new HashMap<>();
        Assignments assignments1 = new Assignments(1);
        testData.put(assignments1, 9);
        Assignments assignments2 = new Assignments(2);
        testData.put(assignments2, 2);
        Assignments assignments3 = new Assignments(3);
        testData.put(assignments3, 7);

        // mock
        Node mockNode = mockNode();
        given(mockNode.eval(assignments1)).willReturn(12);
        given(mockNode.eval(assignments2)).willReturn(-1);
        given(mockNode.eval(assignments3)).willReturn(5);

        // invoke evaluate method
        FitnessFunction fitnessFunction = TestDataFitnessFunction.createIntegerTestDataFitnessFunction(testData);
        double result = fitnessFunction.evaluate(mockNode);

        // assert result
        assertEquals(8d, result, 0.001d);
    }

    @Test
    public void testSpecifiedRankingFunction() {
        // test data
        Map<Assignments, String> testData = new HashMap<>();
        Assignments assignments1 = new Assignments(1);
        testData.put(assignments1, "abcdef");
        Assignments assignments2 = new Assignments(2);
        testData.put(assignments2, "asdfgh");
        Assignments assignments3 = new Assignments(3);
        testData.put(assignments3, "qwerty");

        // mock
        Node mockNode = mockNode();
        given(mockNode.eval(assignments1)).willReturn("abcdex");
        given(mockNode.eval(assignments2)).willReturn("asdxxx");
        given(mockNode.eval(assignments3)).willReturn("qwerty");

        // invoke evaluate method
        FitnessFunction fitnessFunction = new TestDataFitnessFunction<String>(testData, (e, a) -> {
            int ctr = 0;
            for (int i = 0; i < e.length(); i++) {
                if (e.charAt(i) != a.charAt(i)) {
                    ctr++;
                }
            }
            return ctr;
        });
        double result = fitnessFunction.evaluate(mockNode);

        // assert result
        assertEquals(4d, result, 0.001d);
    }
}
