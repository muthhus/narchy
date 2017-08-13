package br.ufpr.gres.util;


import br.ufpr.gres.core.DynamicClassLoader;
import br.ufpr.gres.core.classpath.ClassDetails;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.ASM6;

/**
 * Provides functionality to rename references to generated classes, including fully-qualified
 * type names in String constants.
 */
public class ClassRenamer {

    //TODO
    //public static byte[] renameClassRefs(Class input, Map<Class,Class> mapping) {

//        Map<String, String> re = new HashMap();
//        mapping.forEach((x,y)->{
//            re.put(x.getCanonicalName(), y.getCanonicalName());
//        });

    //}

    public static Class renameClassRefs(Class input, String from, String to, DynamicClassLoader c) {
        return c.load(input.getName(), renameClassRefs(input, from, to));
    }

    public static byte[] renameClassRefs(Class input, String from, String to) {
        ClassReader creader = new ClassReader(ClassDetails.bytes(input));
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        Map<String, String> m = Map.of(from, to);
//         Map<String, String> m = new HashMap<String, String>() {
//            @Override
//            public String get(Object key) {
//                return "Foo";
//            }
//        };

        ClassRemapper r = new ClassRemapper(writer, new SimpleRemapper(m));
        creader.accept(r, 0);
        return writer.toByteArray();
    }

//    /**
//     * Exception that is thrown if the from/to map contains no entry for the generated class
//     * with name <code>typeName</code>.
//     */
//    @SuppressWarnings("serial")
//    public static class NoHashedNameException extends RuntimeException {
//        public NoHashedNameException(String typeName) {
//            super(typeName);
//        }
//    }
//
//    /**
//     * Renames references to generated classes according to the mapping <code>fromTo</code>
//     * in the bytecode <code>classBytes</code>.
//     *
//     * @param fromTo     This map must contain, for every generated class c an entry that maps c to
//     *                   some other valid class name. Generated classes are such classes whose name is matched by
//     *                   {@link Hasher#containsGeneratedClassName(String)}.
//     * @param classBytes The bytecode in which the renaming should take place. This array wil remain
//     *                   unmodified.
//     * @return The bytecode containing the renamed references.
//     */
//    public static byte[] replaceClassNamesInBytes(final Map<String, String> fromTo, byte[] classBytes) {
//        ClassReader creader = new ClassReader(classBytes);
//        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
//        RemappingClassAdapter visitor = new RemappingClassAdapter(writer, new Remapper() {
//            //rename a type reference
//            @Override
//            public String map(String typeName) {
//                String newName = fromTo.get(typeName);
////                if (Hasher.containsGeneratedClassName(typeName) && newName == null) {
////                    throw new NoHashedNameException(typeName);
////                }
//                if (newName != null) typeName = newName;
//                return super.map(typeName);
//            }
//        }) {
//            //visit the body of the method
//            @Override
//            public MethodVisitor visitMethod(int access, String name,
//                                             String desc, String signature, String[] exceptions) {
//                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
//                mv = new RemappingStringConstantAdapter(mv, new StringRemapper() {
//                    //rename any string constants
//                    @Override
//                    public String remapStringConstant(String constant) {
//                        //string constants will refer to the type using a dotted name; replace dots by slashes...
//                        String slashed = slashed(constant);
//                        String to = fromTo.get(slashed);
////                        if (Hasher.containsGeneratedClassName(slashed) && to == null) {
////                            throw new NoHashedNameException(slashed);
////                        }
//                        if (to != null) constant = dotted(to);
//                        return super.remapStringConstant(constant);
//                    }
//                });
//                return mv;
//            }
//        };
//        creader.accept(visitor, 0);
//        return writer.toByteArray();
//    }
//
//    public static String dotted(String className) {
//        return className.replace('/', '.');
//    }
//
//    public static String slashed(String className) {
//        return className.replace('.', '/');
//    }
//
//    /**
//     * A {@link Remapper} that not only re-maps type names but also string
//     * constants.
//     */
//    public static class StringRemapper extends Remapper {
//
//        public String remapStringConstant(String constant) {
//            //by default, don't re-map anything
//            return constant;
//        }
//
//    }
//
//    /**
//     * A {@link MethodAdapter} that calls the provided {@link StringRemapper} to re-map
//     * string constants.
//     */
//    public static class RemappingStringConstantAdapter extends MethodVisitor {
//
//        protected final StringRemapper rm;
//
//        public RemappingStringConstantAdapter(MethodVisitor mv, StringRemapper rm) {
//            super(mv);
//            this.rm = rm;
//        }
//
//        @Override
//        public void visitLdcInsn(Object cst) {
//            if (cst instanceof String) {
//                cst = rm.remapStringConstant((String) cst);
//            }
//            super.visitLdcInsn(cst);
//        }
//
//    }

}
