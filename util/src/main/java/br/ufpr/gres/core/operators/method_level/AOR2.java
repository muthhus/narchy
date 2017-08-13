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
import br.ufpr.gres.core.operators.insn_components.IInsnSubstitution;
import br.ufpr.gres.core.operators.insn_components.IMutationOperatorInsn;
import br.ufpr.gres.core.operators.insn_components.InsnSubstitution;
import br.ufpr.gres.core.operators.insn_components.InsnVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.*;

/**
 * {@code AOR} (Arithmetic Operator Replacement) is a {@link IMutationOperator} implementation that
 * mutates {@code + - * / and %} into its all inverse operation {@code - + / * and *}.
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public enum AOR2 implements IMutationOperatorInsn {

    AOR2("Arithmetic Operator Replacement - Multiple Changes");

    private final String description;

    AOR2(String description) {
        this.description = description;
    }

    @Override
    public MethodVisitor apply(MethodMutationContext context, MethodInfo methodInfo, MethodVisitor methodVisitor) {
        return new InsnVisitor(methodInfo, context, methodVisitor, this);
    }

    @Override
    public String toString() {
        return this.description;
    }

    @Override
    public String getName() {
        return "AOR";
    }

    private static final Set<Integer> opcodesDouble = new HashSet<>();
    private static final Set<Integer> opcodesFloat = new HashSet<>();
    private static final Set<Integer> opcodesInt = new HashSet<>();
    private static final Set<Integer> opcodesLong = new HashSet<>();

    /**
     * Populate OPCODE_TO_MUTATION_MAP.
     */
    static {
        opcodesInt.addAll(Arrays.asList(Opcodes.IADD,
                Opcodes.ISUB,
                Opcodes.IMUL,
                Opcodes.IDIV,
                Opcodes.IREM));

        opcodesLong.addAll(Arrays.asList(Opcodes.LADD,
                Opcodes.LSUB,
                Opcodes.LMUL,
                Opcodes.LDIV,
                Opcodes.LREM));

        opcodesFloat.addAll(Arrays.asList(Opcodes.FADD,
                Opcodes.FSUB,
                Opcodes.FMUL,
                Opcodes.FDIV,
                Opcodes.FREM));

        opcodesDouble.addAll(Arrays.asList(Opcodes.DADD,
                Opcodes.DSUB,
                Opcodes.DMUL,
                Opcodes.DDIV,
                Opcodes.DREM));
    }

    private static Set<Integer> getMutations(int opcode) {
        Set<Integer> replacement = new HashSet<>();
        if (opcodesInt.contains(opcode)) {
            replacement.addAll(opcodesInt);
        } else if (opcodesLong.contains(opcode)) {
            replacement.addAll(opcodesLong);
        } else if (opcodesFloat.contains(opcode)) {
            replacement.addAll(opcodesFloat);
        } else if (opcodesDouble.contains(opcode)) {
            replacement.addAll(opcodesDouble);
        }

        replacement.remove(opcode);
        return replacement;
    }
    
    private static String getOp(int opcode) {
        switch (opcode) {
            case Opcodes.IADD:
            case Opcodes.LADD:
            case Opcodes.FADD:
            case Opcodes.DADD:
                return "+";
            case Opcodes.ISUB:
            case Opcodes.LSUB:
            case Opcodes.FSUB:
            case Opcodes.DSUB:
                return "-";
            case Opcodes.IMUL:
            case Opcodes.LMUL:
            case Opcodes.FMUL:
            case Opcodes.DMUL:
                return "*";
            case Opcodes.IDIV:
            case Opcodes.LDIV:
            case Opcodes.FDIV:
            case Opcodes.DDIV:
                return "/";
            case Opcodes.IREM:
            case Opcodes.LREM:
            case Opcodes.FREM:
            case Opcodes.DREM:
                return "%";
        }
        throw new RuntimeException("Unknown opcode: " + opcode);
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
        return opcodesInt.contains(opcode) || opcodesLong.contains(opcode) || opcodesFloat.contains(opcode) || opcodesDouble.contains(opcode);

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
    public List<IInsnSubstitution> getMutation(int opcode) {
        List<IInsnSubstitution> mutants = new ArrayList<>();

        for (int mutantOpcode : getMutations(opcode)) {
            mutants.add(new InsnSubstitution(mutantOpcode, "Replaced " + getOp(opcode) + " with " + getOp(mutantOpcode)));
        }
        
        return mutants;
    }
}
