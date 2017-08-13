package br.ufpr.gres.core.classpath;///*
// * Copyright 2016 Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>.
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
//package br.ufpr.gres.core.classpath;
//
//import br.ufpr.gres.ClassContext;
//import br.ufpr.gres.core.visitors.methods.RegisterInformationsClassVisitor;
//import br.ufpr.gres.core.visitors.methods.empty.NullVisitor;
//import java.io.File;
//import java.io.IOException;
//import java.util.LinkedList;
//import java.util.List;
//
//import br.ufpr.gres.testcase.classloader.ForeingClassLoader;
//import org.apache.commons.io.FileUtils;
//import org.objectweb.asm.ClassReader;
//
///**
// *
// * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
// * @version 1.0
// */
//public class Resources {
//
//    /*
//    * Root folder (for example: src path, test path)
//     */
//    private final File root;
//
//    public Resources(final File root) {
//        this.root = root;
//    }
//
//    public Resources(final String root) {
//        this(new File(root));
//    }
//
//    public List<ClassDetails> getJavaFiles() throws IOException, ClassNotFoundException {
//        return getClasses(this.root, ".java");
//    }
//
//    public List<ClassDetails> getClasses() throws IOException, ClassNotFoundException {
//        return getClasses(this.root, ".class");
//    }
//
//    private List<ClassDetails> getClasses(final File file, String filter) throws IOException, ClassNotFoundException {
//        final List<ClassDetails> classNames = new LinkedList<>();
//
//        if (file.exists()) {
//            for (final File f : file.listFiles()) {
//                if (f.isDirectory()) {
//                    classNames.addAll(getClasses(f, filter));
//                } else if (f.getName().endsWith(filter)) {
//                    final ClassContext context = new ClassContext();
//                    final ClassReader first = new ClassReader(FileUtils.readFileToByteArray(f));
//                    final NullVisitor nv = new NullVisitor();
//                    final RegisterInformationsClassVisitor mca = new RegisterInformationsClassVisitor(context, nv);
//
//                    first.accept(mca, ClassReader.EXPAND_FRAMES);
//
//                    classNames.add(new FileClassDetails(context.getClassInfo(), this.root,
//                            new ForeingClassLoader(root).getLoader().loadClass(context.getClassInfo().className.asJavaName()
//                            ));
//                }
//            }
//        }
//
//        return classNames;
//    }
//}
