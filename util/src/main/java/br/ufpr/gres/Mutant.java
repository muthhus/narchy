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
package br.ufpr.gres;

import br.ufpr.gres.core.DynamicClassLoader;
import br.ufpr.gres.core.MutationIdentifier;
import br.ufpr.gres.core.MutationInfo;
import br.ufpr.gres.core.classpath.ClassDetails;
import br.ufpr.gres.core.classpath.DynamicClassDetails;
import br.ufpr.gres.core.operators.IMutationOperator;
import br.ufpr.gres.core.operators.method_level.AOR2;
import br.ufpr.gres.core.operators.method_level.ROR;
import br.ufpr.gres.core.visitors.methods.MutatingClassVisitor;
import br.ufpr.gres.core.visitors.methods.empty.NullVisitor;
import jcog.list.FasterList;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * A fully generated mutant
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public final class Mutant {

    static final Logger logger = LoggerFactory.getLogger(Mutant.class);

    public static final ArrayList<IMutationOperator> DEFAULT_MUTATORS = new ArrayList<>() {{
        add(AOR2.AOR2);
        add(ROR.ROR);
    }};

    private final List<MutationInfo> details;
    private final byte[] bytes;

    /**
     * input class name
     */
    public final String className;

    /**
     * mutant unique id
     */
    public final String id;

    public Mutant(String className, String uuid, final MutationInfo details, final byte[] bytes) {
        this.className = className;
        this.id = uuid;
        this.details = new ArrayList<>();
        this.details.add(details);
        this.bytes = bytes;
    }

    public Mutant(String className, String uuid, final List<MutationInfo> details, final byte[] bytes) {
        this.className = className;
        this.id = uuid;
        this.details = details;
        this.bytes = bytes;
    }

    public static Stream<Mutant> mutate(Class x) {
        return mutate(ClassDetails.path(x));
    }

    public static Stream<Mutant> mutate(String path) {
        ClassDetails classes = DynamicClassDetails.get(path);

        final byte[] classToMutate = classes.getBytes();

//        try (DataOutputStream dout = new DataOutputStream(new FileOutputStream(new File(directory + File.separator + "mutants", "Original.class")))) {
//            dout.write(classToMutate);
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }


        //context.setTargetMutation();

        //final PremutationClassInfo classInfo = performPreScan(classToMutate);

        final ClassReader first = new ClassReader(classToMutate);
        final NullVisitor nv = new NullVisitor();
        Collection<IMutationOperator> mutators = (DEFAULT_MUTATORS);

        final ClassContext context = new ClassContext();
        final MutatingClassVisitor mca = new MutatingClassVisitor(mutators, context, nv);

        first.accept(mca, ClassReader.EXPAND_FRAMES);

        List<Mutant> mutants = new FasterList();
//        if (!context.getTargetMutation().isEmpty()) {
//            final List<MutationInfo> details = context.getMutationDetails(context.getTargetMutation().get(0));
//            //System.out.println(details);
//        } else {
        ArrayList<MutationInfo> details = new ArrayList(context.mutations);


        for (MutationInfo detail : details) {
            String uuid = UUID.randomUUID().toString().replace("-", "");
            Mutant mutant = mutate(uuid, detail.getId(), classToMutate);
            //System.out.println(mutant.getDetails());
            mutants.add(mutant);
        }


        DynamicClassLoader d = new DynamicClassLoader();

        return mutants.stream(); //.map((Mutant mutant) -> {
////        ArrayList<MutationIdentifier> details = new ArrayList(context.getCollectedMutations().subList(0, 5).stream().map(MutationDetails::getId).collect(Collectors.toList()));
////        System.out.println("Creating a mutant with order " + details.size());
////
////        Mutant mutant = Mutant.get(details, classToMutate);
//            //System.out.println("The new mutant");
//            //System.out.println(mutant);
//
//
//
//            //"Mutant" + (int)(Math.random()*10000) /* HACK */
//
//
//            //System.out.println(Joiner.on("\n").join(m.getMethods()));
//
//            //System.out.println(m + " " + m.getName());
//            Object x;
//            try {
//                return Tuples.pair(mutant, x);
//            } catch (Exception e) {
//                Mutant.logger.warn("new instance: {}", e);
//                return null;
//            }
//
////        NashornScriptEngine JS = (NashornScriptEngine) new ScriptEngineManager().getEngineByName("nashorn");
////        Bindings b = JS.createBindings();
////        b.put("x", x);
////
////        Object result = null;
////        try {
////            result = JS.eval(js, b);
////            return (X) result;
////        } catch (ScriptException e) {
////            logger.warn("eval: {}", e.getMessage());
////            return null;
////        }
//
//        });
    }

    public Object instance() throws IllegalAccessException, InstantiationException {
        return instance(new DynamicClassLoader());
    }

    public Object instance(DynamicClassLoader d) throws IllegalAccessException, InstantiationException {
        Class m = compile(d);
        return m.newInstance();
    }

    /**
     * Returns a data relating to the mutant
     *
     * @return A MutationDetails object
     */
    public List<MutationInfo> getDetails() {
        return this.details;
    }

    /**
     * Returns a byte array containing the mutant class
     *
     * @return A byte array
     */
    public byte[] getBytes() {
        return this.bytes;
    }

    /**
     * Return the mutant order (number of mutations present in the mutant)
     *
     * @return Mutant order
     */
    public int getOrder() {
        return this.details.size();
    }

    @Override
    public String toString() {
        return id + details;
    }

    public Class compile(DynamicClassLoader cl) {
        //public Class<?> loadClassFromFile (String fileName, String directory) throws ClassNotFoundException {
        try {

            byte[] byteBuffer = getBytes();
            return cl.load(className + id, byteBuffer);
        } catch (Exception e) {
            //logger.error("Error while loading class " + fileName);
            e.printStackTrace();
            return null;
        }

    }

    public static Mutant mutate(String uuid, final MutationIdentifier id, byte[] classToMutate) {
        Collection<IMutationOperator> mutators = new ArrayList<>(DEFAULT_MUTATORS);
        Collection<IMutationOperator> mutatorsFiltered = mutators.stream().filter(p -> id.getMutator().equals(p.getName())).collect(Collectors.toList());

        final ClassContext context = new ClassContext();
        context.setTargetMutation(id);

        // Lembrar de usar isso aqui (ClassPathByteArraySource - pitest)
        // GregorMutater... carregar os bytes
//        final Optional<byte[]> bytes = this.byteSource.getBytes(id.getClassName()
//                .asJavaName());
        //final PremutationClassInfo classInfo = performPreScan(classToMutate);
        final ClassReader reader = new ClassReader(classToMutate);
        final ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES) {
            @Override
            public int newClass(String value) {
                if (value.startsWith(id.getClassName().toString().replace(".", "/")))
                    value = value + uuid;
                return super.newClass(value);
            }
        };


        final MutatingClassVisitor mca = new MutatingClassVisitor(mutatorsFiltered, context, w);
        reader.accept(mca, ClassReader.EXPAND_FRAMES);


        final List<MutationInfo> details = context.mutations;

        return new Mutant(context.getJavaClassName(), uuid, details.stream().filter(p -> p.getId().equals(id)).findFirst().get(), w.toByteArray());
    }


    public static Mutant get(final ArrayList<MutationIdentifier> ids, byte[] classToMutate) {
        Collection<IMutationOperator> mutators = new ArrayList<>(DEFAULT_MUTATORS);
        //Collection<IMutationOperator> mutatorsFiltered = mutators.stream().filter(p ->.getMutator().equals(p.getName())).collect(Collectors.toList());

        final ClassContext context = new ClassContext();
        context.setTargetMutation(ids);

        // Lembrar de usar isso aqui (ClassPathByteArraySource - pitest)
        // GregorMutater... carregar os bytes
//        final Optional<byte[]> bytes = this.byteSource.getBytes(id.getClassName()
//                .asJavaName());
        //final PremutationClassInfo classInfo = performPreScan(classToMutate);
        final ClassReader reader = new ClassReader(classToMutate);

        final ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);


        final MutatingClassVisitor mca = new MutatingClassVisitor(mutators, context, w);

        reader.accept(mca, ClassReader.EXPAND_FRAMES);

        List<MutationInfo> details = new ArrayList<>();

        for (MutationInfo detail : context.mutations) {
            if (ids.stream().anyMatch(p -> p.equals(detail.getId()))) {
                details.add(detail);
            }
        }

        return new Mutant(context.getJavaClassName(), UUID.randomUUID().toString().replace("-", ""), details, w.toByteArray());
    }

}
