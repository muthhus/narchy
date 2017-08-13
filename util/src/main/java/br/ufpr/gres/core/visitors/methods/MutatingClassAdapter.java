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

import br.ufpr.gres.util.XGeneUtils;
import org.objectweb.asm.ClassVisitor;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

/**
 * A wrapper for the class visitor
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class MutatingClassAdapter extends ClassVisitor {

    public MutatingClassAdapter(ClassVisitor cv) {
        super(XGeneUtils.CURRENT_ASM_VERSION, cv);
    }

    /**
     * The mutation is allowed if the method is not "equals", "hash code" or "toString" method
     *
     * @param access
     * @param methodName
     * @param methodDescriptor
     * @param signature
     * @param exceptions
     * @return
     */
    protected static boolean allowedToMutateMethod(int access, String methodName, String methodDescriptor, String signature, String[] exceptions) {
        return !equalsMethod(access, methodName, methodDescriptor, signature, exceptions)
                && !hashCodeMethod(access, methodName, methodDescriptor, signature, exceptions)
                && !toStringMethod(access, methodName, methodDescriptor, signature, exceptions);
    }

    /**
     * check if it is the "equals method"
     *
     * @param methodName
     * @param name
     * @param methodDescriptor
     * @param signature
     * @param exceptions
     * @return
     */
    private static boolean equalsMethod(int access, String methodName, String methodDescriptor, String signature, String[] exceptions) {
        return "equals".equals(methodName) && (access & ACC_PUBLIC) != 0 && "(Ljava/lang/Object;)Z".equals(methodDescriptor);
    }

    /**
     * check if it is the "hash code method"
     *
     * @param access
     * @param methodName
     * @param methodDescriptor
     * @param signature
     * @param exceptions
     * @return
     */
    private static boolean hashCodeMethod(int access, String methodName, String methodDescriptor, String signature, String[] exceptions) {
        return "hashCode".equals(methodName) && (access & ACC_PUBLIC) != 0 && "()I".equals(methodDescriptor);
    }

    /**
     * check if it is the "toString method"
     *
     * @param access
     * @param methodName
     * @param methodDescriptor
     * @param signature
     * @param exceptions
     * @return
     */
    private static boolean toStringMethod(int access, String methodName, String methodDescriptor, String signature, String[] exceptions) {
        return "toString".equals(methodName) && (access & ACC_PUBLIC) != 0 && "()Ljava/lang/String;".equals(methodDescriptor);
    }
}
