/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufpr.gres.core.premutation;

import org.objectweb.asm.*;

/**
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class PreMutationAnalyser extends ClassVisitor {

    private final PremutationClassInfo classInfo = new PremutationClassInfo();

    public PreMutationAnalyser() {
        super(Opcodes.ASM6);
    }

    @Override
    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {

    }

    @Override
    public void visitSource(final String source, final String debug) {

    }

    @Override
    public void visitOuterClass(final String owner, final String name, final String desc) {

    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
        return null;
    }

    @Override
    public void visitAttribute(final Attribute attr) {

    }

    @Override
    public void visitInnerClass(final String name, final String outerName, final String innerName, final int access) {

    }

    @Override
    public FieldVisitor visitField(final int access, final String name, final String desc, final String signature, final Object value) {
        return null;

    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        return new PreMutationMethodAnalyzer(this.classInfo);
    }

    @Override
    public void visitEnd() {

    }

    public PremutationClassInfo getClassInfo() {
        return this.classInfo;
    }
}
