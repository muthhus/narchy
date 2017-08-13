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
package br.ufpr.gres.core.visitors.methods;

import br.ufpr.gres.ClassContext;
import br.ufpr.gres.ClassInfo;
import br.ufpr.gres.core.Location;
import br.ufpr.gres.core.MethodInfo;
import br.ufpr.gres.core.MethodMutationContext;
import br.ufpr.gres.core.MethodName;
import br.ufpr.gres.core.classpath.ClassName;
import br.ufpr.gres.core.operators.IMutationOperator;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.Collection;

/**
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class MutatingClassVisitor extends MutatingClassAdapter {

    private final Collection<IMutationOperator> mutators;
    private final ClassContext context;

    public MutatingClassVisitor(Collection<IMutationOperator> mutators, ClassContext context, ClassVisitor cv) {
        super(cv);
        this.mutators = mutators;
        this.context = context;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.context.registerClass(new ClassInfo(version, access, name, signature, superName, interfaces));
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
        this.context.registerSourceFile(source);
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String methodDescriptor, String signature, String[] exceptions) {

        MethodMutationContext methodContext
                = new MethodMutationContext(this.context,
                        Location.location(ClassName.fromString(this.context.getClassInfo().getName()),
                                MethodName.fromString(methodName), methodDescriptor));

        final MethodVisitor methodVisitor = this.cv.visitMethod(access, methodName, methodDescriptor, signature, exceptions);

        final MethodInfo methodInfo = new MethodInfo()
                .withOwner(this.context.getClassInfo())
                .withAccess(access)
                .withMethodName(methodName)
                .withMethodDescriptor(methodDescriptor);

        if (methodVisitor != null && allowedToMutateMethod(access, methodName, methodDescriptor, signature, exceptions)) {
            return visitMethodForMutation(methodContext, methodInfo, methodVisitor);
        }
        return methodVisitor;
    }

    private MethodVisitor visitMethodForMutation(MethodMutationContext methodContext, final MethodInfo methodInfo, final MethodVisitor methodVisitor) {

        MethodVisitor next = methodVisitor;
        for (final IMutationOperator each : this.mutators) {
            next = each.apply(methodContext, methodInfo, next);
        }
        return new InstructionTrackingMethodVisitor(wrapWithLineTracker(methodContext, next), methodContext);
    }

    private static MethodVisitor wrapWithLineTracker(MethodMutationContext methodContext, final MethodVisitor mv) {
        return new LineTrackingMethodVisitor(methodContext, mv);
    }
}
