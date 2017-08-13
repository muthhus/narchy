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
package br.ufpr.gres.core.operators.jump_components;

import br.ufpr.gres.core.MethodInfo;
import br.ufpr.gres.core.MethodMutationContext;
import br.ufpr.gres.core.MutationIdentifier;
import br.ufpr.gres.core.visitors.methods.MutatingMethodAdapter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

/**
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class AbstractJumpMutator extends MutatingMethodAdapter {

    private final MethodMutationContext context;
    private final IMutationOperatorJump mutationOperator;

    public AbstractJumpMutator(final MethodInfo methodInfo, final MethodMutationContext context, final MethodVisitor delegateMethodVisitor, final IMutationOperatorJump mutationOperator) {
        super(delegateMethodVisitor);
        this.context = context;
        this.mutationOperator = mutationOperator;
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        if (mutationOperator.shouldMutate(opcode)) {
            doMutation(opcode, label);
        } else {
            this.mv.visitJumpInsn(opcode, label);
        }
    }
    
    private void doMutation(final int opcode, final Label label) {

        List<IJumpSubstitution> mutations = mutationOperator.getMutation(opcode);

        boolean hasMutated = false;
        
        for (IJumpSubstitution mutation : mutations) {
            final MutationIdentifier newId = this.context.registerMutation(this.mutationOperator, mutation.decribe());
                        
            if (this.context.shouldMutate(newId)) {
                        
                if (hasMutated) {
                    throw new RuntimeException("You cann't apply multiple mutations in a same local.");
                } else {                    
                    mutation.apply(this.mv, label);
                    hasMutated = true;
                }
            }
        }

        if (!hasMutated) {
            this.mv.visitJumpInsn(opcode, label);
        }
    }   
}
