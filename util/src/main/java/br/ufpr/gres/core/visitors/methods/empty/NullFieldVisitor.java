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
import org.objectweb.asm.FieldVisitor;

/**
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class NullFieldVisitor extends FieldVisitor {

    public NullFieldVisitor() {
        super(XGeneUtils.CURRENT_ASM_VERSION);
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
}
