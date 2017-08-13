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
package br.ufpr.gres.core.operators.insn_components;

import br.ufpr.gres.core.MethodInfo;
import br.ufpr.gres.core.MethodMutationContext;
import br.ufpr.gres.core.MutationIdentifier;
import br.ufpr.gres.core.visitors.methods.MutatingMethodAdapter;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

/**
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public abstract class AbstractInsnMutator extends MutatingMethodAdapter {

    private final MethodMutationContext context;
    private final IMutationOperatorInsn mutationOperator;

    public AbstractInsnMutator(final MethodInfo methodInfo, final MethodMutationContext context, final MethodVisitor delegateMethodVisitor, final IMutationOperatorInsn mutationOperator) {
        super(delegateMethodVisitor);
        this.context = context;
        this.mutationOperator = mutationOperator;
    }

    @Override
    public void visitInsn(final int opcode) {        
        if (mutationOperator.shouldMutate(opcode)) {
            doMutation(opcode);
        } else {
            this.mv.visitInsn(opcode);
        }
    }

    private void doMutation(final int opcode) {

        List<IInsnSubstitution> mutations = mutationOperator.getMutation(opcode);

        boolean hasMutated = false;
        for (IInsnSubstitution mutation : mutations) {
            final MutationIdentifier newId = this.context.registerMutation(this.mutationOperator, mutation.decribe());
                        
            if (this.context.shouldMutate(newId)) {
                // Only one mutation in the "node"
                if (hasMutated) {
                    //throw new RuntimeException("Multiple mutation in the same opcode: " + opcode);
                    continue;
                } else {                    
                    mutation.apply(this.mv);
                    hasMutated = true;
                }
            }
        }

        if (!hasMutated) {
            this.mv.visitInsn(opcode);
        }
    }
}
