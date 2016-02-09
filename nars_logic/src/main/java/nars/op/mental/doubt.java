/*
 * Copyright (C) 2014 peiwang
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nars.op.mental;

import com.google.common.collect.Lists;
import nars.NAR;
import nars.Symbols;
import nars.budget.Budget;
import nars.concept.Concept;
import nars.concept.DefaultConcept;
import nars.concept.util.BeliefTable;
import nars.concept.util.TaskTable;
import nars.nal.nal8.Execution;
import nars.nal.nal8.operator.SyncOperator;
import nars.task.Task;
import nars.term.Term;

import java.util.List;

/**
 * Operator that activates a concept
 */
public class doubt extends SyncOperator {

    /**
     * To activate a concept as if a question has been asked about it
     *
     * @return Immediate results as Tasks
     */
    @Override
    public void execute(Execution execution) {

        execution.nar.runLater(()->{
            Task operation = execution.task;
            Term term = execution.argArray()[0];
            Budget b = operation.budget();

            Concept concept = execution.nar.conceptualize(term, b, 1f);
            if (concept!=null) {
                discountBeliefConfidence(concept, operation.punc(),
                        //TODO use min/max parameters somehow
                        0.5f + (1f - operation.motivation()),
                        execution.nar);
            }

        });

    }

    public static void discountBeliefConfidence(Concept concept, char punc, float confMultiplied /* < 1.0 */, NAR nar) {
        BeliefTable table;
        switch (punc) {
            case Symbols.BELIEF:
                table = concept.beliefs(); break;
            case Symbols.GOAL:
                table = concept.goals(); break;
            default:
                return;
        }

        List<Task> tt = Lists.newArrayList(table);
        table.clear();
        tt.forEach(t-> {
            t.setTruth(t.truth().withConfMult(confMultiplied));
            table.add(t, nar);
        });

    }

}
