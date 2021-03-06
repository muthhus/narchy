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
package org.oakgp.select;

import org.junit.jupiter.api.Test;
import org.oakgp.rank.RankedCandidate;
import org.oakgp.rank.RankedCandidates;
import org.oakgp.util.DummyRandom;
import org.oakgp.util.Random;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.oakgp.TestUtils.integerConstant;

public class FitnessProportionateSelectionTest {
    @Test
    public void test() {
        RankedCandidate c1 = new RankedCandidate(integerConstant(1), 4);
        RankedCandidate c2 = new RankedCandidate(integerConstant(2), 2);
        RankedCandidate c3 = new RankedCandidate(integerConstant(3), 1);
        RankedCandidates candidates = new RankedCandidates(new RankedCandidate[]{c1, c2, c3}, Collections.reverseOrder());

        DummyRandom r = new DummyRandom(.0, .57, .58, .85, .86, .999, .25, .65, .93);
        NodeSelector s = createFitnessProportionateSelection(r, candidates);

        assertEquals(c1.getNode(), s.next());
        assertEquals(c1.getNode(), s.next());
        assertEquals(c2.getNode(), s.next());
        assertEquals(c2.getNode(), s.next());
        assertEquals(c3.getNode(), s.next());
        assertEquals(c3.getNode(), s.next());
        assertEquals(c1.getNode(), s.next());
        assertEquals(c2.getNode(), s.next());
        assertEquals(c3.getNode(), s.next());

        r.assertEmpty();
    }

    private FitnessProportionateSelection createFitnessProportionateSelection(Random random, RankedCandidates candidates) {
        FitnessProportionateSelectionFactory f = new FitnessProportionateSelectionFactory(random);
        return f.getSelector(candidates);
    }
}
