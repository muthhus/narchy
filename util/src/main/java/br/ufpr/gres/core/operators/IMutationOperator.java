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
package br.ufpr.gres.core.operators;

import br.ufpr.gres.core.MethodInfo;
import br.ufpr.gres.core.MethodMutationContext;
import org.objectweb.asm.MethodVisitor;

/**
 * {@code IMutationOperator} is an interface for mutating JVM opcodes.
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 */
public interface IMutationOperator {

    /**
     * The method applies the mutation operator
     *
     * @param context
     * @param methodInfo
     * @param methodVisitor
     * @return
     */
    MethodVisitor apply(final MethodMutationContext context, final MethodInfo methodInfo, final MethodVisitor methodVisitor);
   
    /**
     * Get the operator name
     * @return 
     */
    String getName();
    
    /**
     * Check if the mutation operator is applicable
     * @return a boolean
     */
    //public boolean shouldMutate();    
}
