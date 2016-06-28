/*
 * Operator.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */

package nars.nal.nal8;

import nars.$;
import nars.NAR;
import nars.concept.OperationConcept;
import nars.term.Operator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * An operator implementation
 */
public abstract class AbstractOperator implements Consumer<OperationConcept> {


    public final @NotNull Operator atomicTerm;

    protected NAR nar; //TODO make private

    @NotNull
    @Override
    public String toString() {
        return atomicTerm.toString();
    }


    /**
     * use the class name as the operator name
     */
    public AbstractOperator() {
        this(null);
    }
    
    public AbstractOperator(@Nullable String operatorName) {
        if (operatorName == null) {
            operatorName = getClass().getSimpleName();
        }
        this.atomicTerm = $.operator(operatorName);
    }


    @Override
    public void accept(@NotNull OperationConcept exec) {

        //only proceed with execution if positively motivated
        if ((exec.goals().motivation(nar.time()) > 0))
            execute(exec);

//        if (async()) {
//            //asynch
//            NAR.runAsync(() -> execute(exec));
//        } else {
            //synchronous


    }

    /**
     * Required method for every operate, specifying the corresponding
     * operation
     *
     * @return The direct collectable results and feedback of the
     * reportExecution
     */

    public abstract void execute(OperationConcept exec);



    public final @Nullable Operator operator() {
        return atomicTerm;
    }

    /** this will be called prior to any execution */
    public void init(NAR nar) {
        this.nar = nar;
    }


    /*
    <patham9_> when a goal task is processed, the following happens: In order to decide on whether it is relevant for the current situation, at first it is projected to the current time, then it is revised with previous "desires", then it is checked to what extent this projected revised desire is already fullfilled (which revises its budget) , if its below satisfaction threshold then it is pursued, if its an operation it is additionally checked if
    <patham9_> executed
    <patham9_> the consequences of this, to give examples, are a lot:
    <patham9_> 1 the system wont execute something if it has a strong objection against it. (example: it wont jump down again 5 meters if it previously observed that this damages it, no matter if it thinks about that situation again or not)
    <patham9_> 2. the system wont lose time with thoughts about already satisfied goals (due to budget shrinking proportional to satisfaction)
    <patham9_> 3. the system wont execute and pursue what is already satisfied
    <patham9_> 4. the system wont try to execute and pursue things in the current moment which are "sheduled" to be in the future.
    <patham9_> 5. the system wont pursue a goal it already pursued for the same reason (due to revision, it is related to 1)
    */



}
