///*
// * Copyright 2017 Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package br.ufpr.gres.core;
//
//import br.ufpr.gres.ClassContext;
//import br.ufpr.gres.core.operators.IMutationOperator;
//import br.ufpr.gres.core.visitors.methods.MutatingClassVisitor;
//import br.ufpr.gres.core.visitors.methods.empty.NullVisitor;
//import org.objectweb.asm.ClassReader;
//import org.objectweb.asm.ClassWriter;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//
///**
// *
// * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
// * @version 1.0
// */
//public class Mutator {
//
//    public static Mutant getMutation(Collection<IMutationOperator> mutators, ArrayList<MutationIdentifier> ids, byte[] classToMutate) {
//        final ClassContext context = new ClassContext();
//        context.setTargetMutation(ids);
//        final ClassReader reader = new ClassReader(classToMutate);
//        final ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
//
//        final MutatingClassVisitor mca = new MutatingClassVisitor(mutators, context, w);
//        reader.accept(mca, ClassReader.EXPAND_FRAMES);
//
//        List<MutationDetails> details = new ArrayList<>();
//
//        for (MutationDetails detail : context.getCollectedMutations()) {
//            if (ids.stream().anyMatch(p -> p.equals(detail.getId()))) {
//                details.add(detail);
//            }
//        }
//
//        return new Mutant(details, w.toByteArray());
//    }
//}
