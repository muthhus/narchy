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
package br.ufpr.gres.core.operators.method_level;

import br.ufpr.gres.core.MethodInfo;
import br.ufpr.gres.core.MethodMutationContext;
import br.ufpr.gres.core.operators.IMutationOperator;
import br.ufpr.gres.core.operators.jump_components.IJumpSubstitution;
import br.ufpr.gres.core.operators.jump_components.IMutationOperatorJump;
import br.ufpr.gres.core.operators.jump_components.JumpSubstitution;
import br.ufpr.gres.core.operators.jump_components.JumpVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code ROR} (Relational Operator Replacement) is a {@link IMutationOperator} implementation that
 * mutates {@code < <= > >= == != <= >= < and >} into its inverse operation
 * {@code <= < >= > != == > < >= and <=} respectively.
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public enum ROR implements IMutationOperatorJump {

    ROR("Relational Operator Replacement");

    private final String description;

    ROR(String description) {
        this.description = description;
    }

    @Override
    public MethodVisitor apply(MethodMutationContext context, MethodInfo methodInfo, MethodVisitor methodVisitor) {
        return new JumpVisitor(methodInfo, context, methodVisitor, this);
    }

    @Override
    public String toString() {
        return this.description;
    }

    @Override
    public String getName() {
        return this.name();
    }

    /**
     * Map of all the mutations by their original opcode.
     */
    private static final Map<Integer, IJumpSubstitution> MUTATIONS = new HashMap<>();

    private static final String DESCRIPTION_CONDITIONAL_BOUNDARY = "changed conditional boundary";
    private static final String DESCRIPTION_NEGATE_BOUNDARY = "negated conditional";

    /**
     * Populate OPCODE_TO_MUTATION_MAP.
     */
    static {
        //Conditionals Boundary Mutator
        MUTATIONS.put(Opcodes.IFLE, new JumpSubstitution(Opcodes.IFLT, DESCRIPTION_CONDITIONAL_BOUNDARY));
        MUTATIONS.put(Opcodes.IFGE, new JumpSubstitution(Opcodes.IFGT, DESCRIPTION_CONDITIONAL_BOUNDARY));
        MUTATIONS.put(Opcodes.IFGT, new JumpSubstitution(Opcodes.IFGE, DESCRIPTION_CONDITIONAL_BOUNDARY));
        MUTATIONS.put(Opcodes.IFLT, new JumpSubstitution(Opcodes.IFLE, DESCRIPTION_CONDITIONAL_BOUNDARY));
        MUTATIONS.put(Opcodes.IF_ICMPLE, new JumpSubstitution(Opcodes.IF_ICMPLT, DESCRIPTION_CONDITIONAL_BOUNDARY));
        MUTATIONS.put(Opcodes.IF_ICMPGE, new JumpSubstitution(Opcodes.IF_ICMPGT, DESCRIPTION_CONDITIONAL_BOUNDARY));
        MUTATIONS.put(Opcodes.IF_ICMPGT, new JumpSubstitution(Opcodes.IF_ICMPGE, DESCRIPTION_CONDITIONAL_BOUNDARY));
        MUTATIONS.put(Opcodes.IF_ICMPLT, new JumpSubstitution(Opcodes.IF_ICMPLE, DESCRIPTION_CONDITIONAL_BOUNDARY));

        //Negate Conditionals Mutator 
        MUTATIONS.put(Opcodes.IFEQ, new JumpSubstitution(Opcodes.IFNE, DESCRIPTION_NEGATE_BOUNDARY));
        MUTATIONS.put(Opcodes.IFNE, new JumpSubstitution(Opcodes.IFEQ, DESCRIPTION_NEGATE_BOUNDARY));
        MUTATIONS.put(Opcodes.IFLE, new JumpSubstitution(Opcodes.IFGT, DESCRIPTION_NEGATE_BOUNDARY));
        MUTATIONS.put(Opcodes.IFGE, new JumpSubstitution(Opcodes.IFLT, DESCRIPTION_NEGATE_BOUNDARY));
        MUTATIONS.put(Opcodes.IFGT, new JumpSubstitution(Opcodes.IFLE, DESCRIPTION_NEGATE_BOUNDARY));
        MUTATIONS.put(Opcodes.IFLT, new JumpSubstitution(Opcodes.IFGE, DESCRIPTION_NEGATE_BOUNDARY));
        MUTATIONS.put(Opcodes.IFNULL, new JumpSubstitution(Opcodes.IFNONNULL,DESCRIPTION_NEGATE_BOUNDARY));
        MUTATIONS.put(Opcodes.IFNONNULL, new JumpSubstitution(Opcodes.IFNULL,DESCRIPTION_NEGATE_BOUNDARY));
        MUTATIONS.put(Opcodes.IF_ICMPNE, new JumpSubstitution(Opcodes.IF_ICMPEQ,DESCRIPTION_NEGATE_BOUNDARY));
        MUTATIONS.put(Opcodes.IF_ICMPEQ, new JumpSubstitution(Opcodes.IF_ICMPNE,DESCRIPTION_NEGATE_BOUNDARY));
        MUTATIONS.put(Opcodes.IF_ICMPLE, new JumpSubstitution(Opcodes.IF_ICMPGT,DESCRIPTION_NEGATE_BOUNDARY));
        MUTATIONS.put(Opcodes.IF_ICMPGE, new JumpSubstitution(Opcodes.IF_ICMPLT,DESCRIPTION_NEGATE_BOUNDARY));
        MUTATIONS.put(Opcodes.IF_ICMPGT, new JumpSubstitution(Opcodes.IF_ICMPLE,DESCRIPTION_NEGATE_BOUNDARY));
        MUTATIONS.put(Opcodes.IF_ICMPLT, new JumpSubstitution(Opcodes.IF_ICMPGE,DESCRIPTION_NEGATE_BOUNDARY));
        MUTATIONS.put(Opcodes.IF_ACMPEQ, new JumpSubstitution(Opcodes.IF_ACMPNE,DESCRIPTION_NEGATE_BOUNDARY));
        MUTATIONS.put(Opcodes.IF_ACMPNE, new JumpSubstitution(Opcodes.IF_ACMPEQ,DESCRIPTION_NEGATE_BOUNDARY));
    }

    /**
     * Is the given opcode mutatable by {@link AOR}.
     *
     * @param opcode the opcode to be mutated.
     * @return {@code true} if {@link ArithmeticMutation} can mutate this opcode; {@code false}
     * otherwise.
     */
    @Override
    public boolean shouldMutate(int opcode) {
        return MUTATIONS.containsKey(opcode);
    }

    /**
     * Get the {@link AOR} instance that can mutate the given opcode.
     *
     * @param opcode the opcode to mutate.
     * @return a {@link AOR} instance; never null.
     * @throws IllegalArgumentException if opcode can not be mutated as defined by
     * {@link #isMutatable(int)}.
     * @see #isMutatable(int)
     */
    @Override
    public List<IJumpSubstitution> getMutation(int opcode) {
        if (MUTATIONS.containsKey(opcode)) {
            return Collections.singletonList(MUTATIONS.get(opcode));
        }
        throw new IllegalArgumentException("can not mutate opcode " + opcode);
    }
}
