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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code AOR} (Arithmetic Operator Replacement) is a {@link IMutationOperator} implementation that
 * mutates {@code + - * / and %} into its inverse operation {@code - + / * and *} respectively.
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public enum AOR implements IMutationOperatorInsn {

    AOR("Arithmetic Operator Replacement");

    private final String description;

    AOR(String description) {
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
        return this.name();
    }

    /**
     * Map of all the mutations by their original opcode.
     */
    private static final Map<Integer, IInsnSubstitution> MUTATIONS = new HashMap<>();

    /**
     * Populate OPCODE_TO_MUTATION_MAP.
     */
    static {
        MUTATIONS.put(Opcodes.IADD, new InsnSubstitution(Opcodes.ISUB, "Replaced integer addition with subtraction"));
        MUTATIONS.put(Opcodes.ISUB, new InsnSubstitution(Opcodes.IADD, "Replaced integer subtraction with addition"));
        MUTATIONS.put(Opcodes.IMUL, new InsnSubstitution(Opcodes.IDIV, "Replaced integer multiplication with division"));
        MUTATIONS.put(Opcodes.IDIV, new InsnSubstitution(Opcodes.IMUL, "Replaced integer division with multiplication"));
        MUTATIONS.put(Opcodes.IOR, new InsnSubstitution(Opcodes.IAND, "Replaced bitwise OR with AND"));
        MUTATIONS.put(Opcodes.IAND, new InsnSubstitution(Opcodes.IOR, "Replaced bitwise AND with OR"));
        MUTATIONS.put(Opcodes.IREM, new InsnSubstitution(Opcodes.IMUL, "Replaced integer modulus with multiplication"));
        MUTATIONS.put(Opcodes.IXOR, new InsnSubstitution(Opcodes.IAND, "Replaced XOR with AND"));
        MUTATIONS.put(Opcodes.ISHL, new InsnSubstitution(Opcodes.ISHR, "Replaced Shift Left with Shift Right"));
        MUTATIONS.put(Opcodes.ISHR, new InsnSubstitution(Opcodes.ISHL, "Replaced Shift Right with Shift Left"));
        MUTATIONS.put(Opcodes.IUSHR, new InsnSubstitution(Opcodes.ISHL, "Replaced Unsigned Shift Right with Shift Left"));

        // longs
        MUTATIONS.put(Opcodes.LADD, new InsnSubstitution(Opcodes.LSUB, "Replaced long addition with subtraction"));
        MUTATIONS.put(Opcodes.LSUB, new InsnSubstitution(Opcodes.LADD, "Replaced long subtraction with addition"));
        MUTATIONS.put(Opcodes.LMUL, new InsnSubstitution(Opcodes.LDIV, "Replaced long multiplication with division"));
        MUTATIONS.put(Opcodes.LDIV, new InsnSubstitution(Opcodes.LMUL, "Replaced long division with multiplication"));
        MUTATIONS.put(Opcodes.LOR, new InsnSubstitution(Opcodes.LAND, "Replaced bitwise OR with AND"));
        MUTATIONS.put(Opcodes.LAND, new InsnSubstitution(Opcodes.LOR, "Replaced bitwise AND with OR"));
        MUTATIONS.put(Opcodes.LREM, new InsnSubstitution(Opcodes.LMUL, "Replaced long modulus with multiplication"));
        MUTATIONS.put(Opcodes.LXOR, new InsnSubstitution(Opcodes.LAND, "Replaced XOR with AND"));
        MUTATIONS.put(Opcodes.LSHL, new InsnSubstitution(Opcodes.LSHR, "Replaced Shift Left with Shift Right"));
        MUTATIONS.put(Opcodes.LSHR, new InsnSubstitution(Opcodes.LSHL, "Replaced Shift Right with Shift Left"));
        MUTATIONS.put(Opcodes.LUSHR, new InsnSubstitution(Opcodes.LSHL, "Replaced Unsigned Shift Right with Shift Left"));

        // floats
        MUTATIONS.put(Opcodes.FADD, new InsnSubstitution(Opcodes.FSUB, "Replaced float addition with subtraction"));
        MUTATIONS.put(Opcodes.FSUB, new InsnSubstitution(Opcodes.FADD, "Replaced float subtraction with addition"));
        MUTATIONS.put(Opcodes.FMUL, new InsnSubstitution(Opcodes.FDIV, "Replaced float multiplication with division"));
        MUTATIONS.put(Opcodes.FDIV, new InsnSubstitution(Opcodes.FMUL, "Replaced float division with multiplication"));
        MUTATIONS.put(Opcodes.FREM, new InsnSubstitution(Opcodes.FMUL, "Replaced float modulus with multiplication"));

        // doubles
        MUTATIONS.put(Opcodes.DADD, new InsnSubstitution(Opcodes.DSUB, "Replaced double addition with subtraction"));
        MUTATIONS.put(Opcodes.DSUB, new InsnSubstitution(Opcodes.DADD, "Replaced double subtraction with addition"));
        MUTATIONS.put(Opcodes.DMUL, new InsnSubstitution(Opcodes.DDIV, "Replaced double multiplication with division"));
        MUTATIONS.put(Opcodes.DDIV, new InsnSubstitution(Opcodes.DMUL, "Replaced double division with multiplication"));
        MUTATIONS.put(Opcodes.DREM, new InsnSubstitution(Opcodes.DMUL, "Replaced double modulus with multiplication"));
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
    public List<IInsnSubstitution> getMutation(int opcode) {
        if (MUTATIONS.containsKey(opcode)) {
            return Collections.singletonList(MUTATIONS.get(opcode));
        }
        throw new IllegalArgumentException("can not mutate opcode " + opcode);
    }
}
