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
package br.ufpr.gres.core;

import br.ufpr.gres.ClassContext;
import br.ufpr.gres.core.operators.IMutationOperator;
import br.ufpr.gres.core.operators.method_level.AOR2;
import br.ufpr.gres.core.operators.method_level.ROR;
import br.ufpr.gres.core.visitors.methods.MutatingClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;



/**
 * A fully generated mutant
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public final class Mutant {

    public static final ArrayList<IMutationOperator> DEFAULT_MUTATORS = new ArrayList<>() {{
        add(AOR2.AOR2);
        add(ROR.ROR);
    }};
;
    private final List<MutationDetails> details;
    private final byte[] bytes;
    public final String id;

    public Mutant(String uuid, final MutationDetails details, final byte[] bytes) {
        this.id = uuid;
        this.details = new ArrayList<>();
        this.details.add(details);
        this.bytes = bytes;
    }

    public Mutant(String uuid, final List<MutationDetails> details, final byte[] bytes) {
        this.id = uuid;
        this.details = details;
        this.bytes = bytes;
    }

    /**
     * Returns a data relating to the mutant
     *
     * @return A MutationDetails object
     */
    public List<MutationDetails> getDetails() {
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
        StringBuilder description = new StringBuilder();

        for (int i = 0; i < details.size(); i++) {
            description.append(i).append(1).append(") ").append(details.get(i)).append('\n');
        }

        return description.toString();
    }

    public Class compile(String name, DynamicClassLoader cl) {
        //public Class<?> loadClassFromFile (String fileName, String directory) throws ClassNotFoundException {
        try {

            byte[] byteBuffer = getBytes();
            return cl.load(name + id, byteBuffer);
        } catch (Exception e) {
            //logger.error("Error while loading class " + fileName);
            e.printStackTrace();
            return null;
        }

    }

    public static Mutant get(String uuid, final MutationIdentifier id, byte[] classToMutate) {
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
                if (value.startsWith(id.getClassName().toString().replace(".","/")))
                    value = value + uuid;
                return super.newClass( value );
            }
        };


        final MutatingClassVisitor mca = new MutatingClassVisitor(mutatorsFiltered, context, w);
        reader.accept(mca, ClassReader.EXPAND_FRAMES);


        final List<MutationDetails> details = context.getCollectedMutations();

        return new Mutant(uuid, details.stream().filter(p -> p.getId().equals(id)).findFirst().get(), w.toByteArray());
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

        List<MutationDetails> details = new ArrayList<>();

        for (MutationDetails detail : context.getCollectedMutations()) {
            if (ids.stream().anyMatch(p -> p.equals(detail.getId()))) {
                details.add(detail);
            }
        }

        return new Mutant(UUID.randomUUID().toString().replace("-", ""), details, w.toByteArray());
    }

}
