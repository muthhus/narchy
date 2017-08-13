/*
 * Copyright 2016 Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>.
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
package br.ufpr.gres.core;

import br.ufpr.gres.ClassContext;
import br.ufpr.gres.ClassInfo;
import br.ufpr.gres.core.operators.IMutationOperator;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class MethodMutationContext {

    private final ClassContext classContext;
    private final Location location;

    private int instructionIndex;

    private int lastLineNumber;
    private final Set<String> mutationFindingDisabledReasons = new HashSet<>();

    public MethodMutationContext(final ClassContext classContext, final Location location) {
        this.classContext = classContext;
        this.location = location;
    }

    public MutationIdentifier registerMutation(final IMutationOperator mutationOperator, final String description) {

        final MutationIdentifier newId = getNextMutationIdentifer(mutationOperator, this.classContext.getJavaClassName());

        final MutationDetails details = new MutationDetails(newId,
                this.classContext.getFileName(), description, this.lastLineNumber,
                this.classContext.getCurrentBlock(),
                this.classContext.isWithinFinallyBlock(), false);
        
        registerMutation(details);      
        
        return newId;
    }

    private MutationIdentifier getNextMutationIdentifer(final IMutationOperator mutationOperator, final String className) {
        return new MutationIdentifier(this.location, this.instructionIndex, mutationOperator.getName());
    }

    private void registerMutation(final MutationDetails details) {
        if (!isMutationFindingDisabled()) {            
            this.classContext.addMutation(details);
        }
    }

    private boolean isMutationFindingDisabled() {
        return !this.mutationFindingDisabledReasons.isEmpty();
    }

    public void registerCurrentLine(final int line) {
        this.lastLineNumber = line;
    }

    public void registerNewBlock() {
        this.classContext.registerNewBlock();
    }

    public void registerFinallyBlockStart() {
        this.classContext.registerFinallyBlockStart();
    }

    public void registerFinallyBlockEnd() {
        this.classContext.registerFinallyBlockEnd();
    }

    public ClassInfo getClassInfo() {
        return this.classContext.getClassInfo();
    }

    public boolean shouldMutate(final MutationIdentifier newId) {
        return this.classContext.shouldMutate(newId);
    }

    public void disableMutations(final String reason) {
        this.mutationFindingDisabledReasons.add(reason);
    }

    public void enableMutatations(final String reason) {
        this.mutationFindingDisabledReasons.remove(reason);
    }

    public void increment() {
        this.instructionIndex = this.instructionIndex + 1;
    }

    public int currentInstructionCount() {
        return this.instructionIndex;
    }
}
