///*
// * Copyright (C) 2014 peiwang
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//package nars.op.mental;
//
//import nars.NAR;
//import nars.Symbols;
//import nars.concept.Concept;
//import nars.concept.OperationConcept;
//import nars.nal.nal8.AbstractOperator;
//import nars.table.BeliefTable;
//import org.jetbrains.annotations.NotNull;
//
///**
// * Operator that activates a concept
// */
//public class doubt extends AbstractOperator {
//
//    /**
//     * To activate a concept as if a question has been asked about it
//     *
//     * @return Immediate results as Tasks
//     */
//    @Override
//    public void execute(/*@NotNull*/ OperationConcept t) {
//
//
//
////        nar.runLater(()->{
////
////            Term term = Operator.argArray(t)[0];
////
////            Budget operation = UnitBudget.Zero; //execution.get(0); //TODO use the sum or average of the execution task budgets
////
////            Concept concept = nar.conceptualize(term, operation);
////            if (concept!=null) {
////                discountBeliefConfidence(concept, operation.punc(),
////                        //TODO use min/max parameters somehow
////                        0.5f + (1f - operation.motivation()),
////                        nar);
////            }
////
////        });
//
//    }
//
//    public static void discountBeliefConfidence(@NotNull Concept concept, byte punc, float confMultiplied /* < 1.0 */, @NotNull NAR nar) {
//        BeliefTable table;
//        switch (punc) {
//            case Symbols.BELIEF:
//                table = concept.beliefs(); break;
//            case Symbols.GOAL:
//                table = concept.goals(); break;
////            default:
////                return;
//        }
//
////        List<Task> tt = Lists.newArrayList(table);
////        table.clear();
////        tt.forEach(t-> {
////            t.setTruth(t.truth().confMult(confMultiplied));
////            table.add(t, null, new FasterList() /* HACK */, nar);
////        });
//
//    }
//
//}
