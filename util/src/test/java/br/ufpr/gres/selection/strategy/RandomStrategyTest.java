/*
 * Copyright 2017 Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.ufpr.gres.selection.strategy;

import br.ufpr.gres.core.MutationDetails;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 */
public class RandomStrategyTest {
    
    @Test
    public void testSomeMethod() throws IOException, ClassNotFoundException {
        List<MutationDetails> mutations = StrategyTest.getMutations();
        
        System.out.println("All mutations");
        for (MutationDetails mutationDetails : mutations) {
            System.out.println(mutationDetails);
        }

        RandomStrategy strategy = new RandomStrategy(new ArrayList<>(mutations));

        System.out.println("\n\nInitiating the selection for the strategy " + strategy + " \n\n");
        int i = 0;
        while (strategy.allItemsSelected()) {
            i++;

            List<MutationDetails> selection = strategy.get();

            System.out.println("===Selection " + i + "===");
            for (MutationDetails mutationDetails : selection) {
                System.out.println(mutationDetails);
            }
        }
    }
}
