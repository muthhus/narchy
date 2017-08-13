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

import org.objectweb.asm.MethodVisitor;

/**
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class InsnSubstitution implements IInsnSubstitution {

    private final int replacementOpcode;
    private final String message;

    public InsnSubstitution(final int replacementOpcode, final String message) {
        this.replacementOpcode = replacementOpcode;
        this.message = message;
    }

    @Override
    public void apply(MethodVisitor mv) {
        mv.visitInsn(this.replacementOpcode);
    }

    @Override
    public String decribe() {
        return this.message;
    }

}
