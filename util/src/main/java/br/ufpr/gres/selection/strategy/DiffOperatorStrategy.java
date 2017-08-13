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

import br.ufpr.gres.core.MutationInfo;
import br.ufpr.gres.selection.AbstractStrategy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Different Operators strategy
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class DiffOperatorStrategy extends AbstractStrategy {

    public DiffOperatorStrategy(ArrayList<MutationInfo> list) {
        super(list);
    }

    @Override
    public List<MutationInfo> get() {
        ArrayList<MutationInfo> result = new ArrayList<>();

        int numSelection = selection();

        Iterator<MutationInfo> it = new ArrayList(this.listStrategy).iterator();

        while (result.size() != numSelection) {
            if (!it.hasNext()) {
                ArrayList<MutationInfo> itemsAvailable = new ArrayList(this.originalList);

                itemsAvailable.removeAll(result);

                it = itemsAvailable.iterator();
            }
            
            MutationInfo mutationInfo = it.next();

            if (result.isEmpty()) {
                updateListStrategy(mutationInfo);
                result.add(mutationInfo);
                continue;
            }

            if (result.stream().map(MutationInfo::getMutator).noneMatch(mutationInfo.getMutator()::equals)) {
                updateListStrategy(mutationInfo);
                result.add(mutationInfo);
                break;
            }
        }

        result.sort(getComparator());

        return result;
    }

    @Override
    public String toString() {
        return "RandomStrategy";
    }
}
