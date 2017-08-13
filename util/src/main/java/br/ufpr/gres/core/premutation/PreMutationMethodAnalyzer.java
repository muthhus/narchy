/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufpr.gres.core.premutation;

import br.ufpr.gres.core.visitors.methods.TryWithResourcesMethodVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class PreMutationMethodAnalyzer extends MethodVisitor {

    public static final Collection<String> LOGGING_CLASSES = Arrays
            .asList(
                    "java.util.logging",
                    "org.apache.log4j",
                    "org.slf4j",
                    "org.apache.commons.logging");

    private final Set<String> loggingClasses;

    private int currentLineNumber;
    private final PremutationClassInfo classInfo;

    public PreMutationMethodAnalyzer(final PremutationClassInfo classInfo) {
        super(Opcodes.ASM6, new TryWithResourcesMethodVisitor(classInfo));
        this.classInfo = classInfo;
        this.loggingClasses = new HashSet<>(LOGGING_CLASSES);
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc, boolean itf) {

        if (this.loggingClasses.stream().anyMatch(owner::startsWith)) {
            this.classInfo.registerLineToAvoid(this.currentLineNumber);
        }

        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitLineNumber(final int line, final Label start) {
        this.currentLineNumber = line;
        super.visitLineNumber(line, start);
    }
}
