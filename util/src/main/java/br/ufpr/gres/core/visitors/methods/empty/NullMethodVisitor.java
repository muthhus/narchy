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
package br.ufpr.gres.core.visitors.methods.empty;

import br.ufpr.gres.util.XGeneUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class NullMethodVisitor extends MethodVisitor {

    NullMethodVisitor() {
        super(XGeneUtils.CURRENT_ASM_VERSION);
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String arg0, final boolean arg1) {
        return new NullAnnotationVisitor();
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        return new NullAnnotationVisitor();
    }

    @Override
    public void visitAttribute(final Attribute arg0) {
    }

    @Override
    public void visitCode() {
    }

    @Override
    public void visitEnd() {
    }

    @Override
    public void visitFieldInsn(final int arg0, final String arg1, final String arg2, final String arg3) {
    }

    @Override
    public void visitFrame(final int arg0, final int arg1, final Object[] arg2, final int arg3, final Object[] arg4) {
    }

    @Override
    public void visitIincInsn(final int arg0, final int arg1) {
    }

    @Override
    public void visitInsn(final int arg0) {
    }

    @Override
    public void visitIntInsn(final int arg0, final int arg1) {
    }

    @Override
    public void visitJumpInsn(final int arg0, final Label arg1) {
    }

    @Override
    public void visitLabel(final Label arg0) {
    }

    @Override
    public void visitLdcInsn(final Object arg0) {
    }

    @Override
    public void visitLineNumber(final int arg0, final Label arg1) {
    }

    @Override
    public void visitLocalVariable(final String arg0, final String arg1, final String arg2, final Label arg3, final Label arg4, final int arg5) {
    }

    @Override
    public void visitLookupSwitchInsn(final Label arg0, final int[] arg1, final Label[] arg2) {
    }

    @Override
    public void visitMaxs(final int arg0, final int arg1) {
    }

    @Override
    public void visitMethodInsn(final int arg0, final String arg1, final String arg2, final String arg3) {
    }

    @Override
    public void visitMultiANewArrayInsn(final String arg0, final int arg1) {
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(final int arg0, final String arg1, final boolean arg2) {
        return new NullAnnotationVisitor();
    }

    @Override
    public void visitTableSwitchInsn(final int arg0, final int arg1, final Label arg2, final Label... labels) {
    }

    @Override
    public void visitTryCatchBlock(final Label arg0, final Label arg1, final Label arg2, final String arg3) {
    }

    @Override
    public void visitTypeInsn(final int arg0, final String arg1) {
    }

    @Override
    public void visitVarInsn(final int arg0, final int arg1) {
    }
}
