/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufpr.gres.core.visitors.methods.empty;

import br.ufpr.gres.util.XGeneUtils;
import org.objectweb.asm.*;

/**
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class NullVisitor extends ClassVisitor {

    public NullVisitor() {
        super(XGeneUtils.CURRENT_ASM_VERSION);
    }

    @Override
    public void visit(final int arg0, final int arg1, final String arg2, final String arg3, final String arg4, final String[] arg5) {
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String arg0, final boolean arg1) {
        return new NullAnnotationVisitor();
    }

    @Override
    public void visitAttribute(final Attribute arg0) {
    }

    @Override
    public void visitEnd() {
    }

    @Override
    public FieldVisitor visitField(final int arg0, final String arg1, final String arg2, final String arg3, final Object arg4) {
        return new NullFieldVisitor();
    }

    @Override
    public void visitInnerClass(final String arg0, final String arg1, final String arg2, final int arg3) {
    }

    @Override
    public MethodVisitor visitMethod(final int arg0, final String arg1, final String arg2, final String arg3, final String[] arg4) {
        return new NullMethodVisitor();
    }

    @Override
    public void visitOuterClass(final String arg0, final String arg1, final String arg2) {
    }

    @Override
    public void visitSource(final String arg0, final String arg1) {
    }
}
