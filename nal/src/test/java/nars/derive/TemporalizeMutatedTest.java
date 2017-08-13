package nars.derive;

import br.ufpr.gres.ClassContext;
import br.ufpr.gres.core.DynamicClassLoader;
import br.ufpr.gres.core.Mutant;
import br.ufpr.gres.core.MutationDetails;
import br.ufpr.gres.core.classpath.ClassDetails;
import br.ufpr.gres.core.classpath.DynamicClassDetails;
import br.ufpr.gres.core.operators.IMutationOperator;
import br.ufpr.gres.core.visitors.methods.MutatingClassVisitor;
import br.ufpr.gres.core.visitors.methods.empty.NullVisitor;
import nars.$;
import nars.Narsese;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static br.ufpr.gres.core.Mutant.DEFAULT_MUTATORS;

public class TemporalizeMutatedTest {

    static final Logger logger = LoggerFactory.getLogger(TemporalizeMutatedTest.class);

    @Test
    public void testMutationDynamic() {
        mutate("nars/derive/time/Temporalize.class" /*Temporalize.class*/).forEach(o -> {
            try {

                invoke(o, "knowTerm", $.$("(a)"), 0L);

                String os = o.toString();

                Object r = invoke(o, "solve", $.$("(a)"));

                System.out.print(os + " ");
                System.out.println(r);


            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }
        });
    }

    public static Object invoke(Object inst, String method, Object... args) {
        //System.err.println(inst);
        try {

            Method[] mm = Stream.of(inst.getClass().getMethods()).filter(m -> {
                if (m.getParameterCount() == args.length && m.getName().equals(method)) {
                    Class<?>[] parameterTypes = m.getParameterTypes();
                    boolean fail = false;
                    for (int i = 0; i < parameterTypes.length; i++) {
                        Class p = parameterTypes[i];

                        if (p == long.class && args[i].getClass() == Long.class) {
                            continue;
                        }
                        if (!p.isAssignableFrom(args[i].getClass())) {
                            fail = true;
                            break;
                        }
                    }
                    if (!fail) {
                        m.trySetAccessible();
                        return true;
                    }
                }
                return false;
            }).toArray(Method[]::new);
            if (mm.length == 0) {
                throw new RuntimeException("method not found applicable to args: " + Arrays.toString(args));
            }


            for (Method xm : mm) {
                xm.trySetAccessible();
                //System.out.println(xm);
                try {
                    return xm.invoke(inst, args);
                } catch (Throwable e) {
                    //e.printStackTrace();
                    //logger.warn(e.getMessage());
                    return null; //continue;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static Stream mutate(String path) {
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

        List<Mutant> mutants = $.newArrayList();
        if (!context.getTargetMutation().isEmpty()) {
            final List<MutationDetails> details = context.getMutationDetails(context.getTargetMutation().get(0));
            //System.out.println(details);
        } else {
            ArrayList<MutationDetails> details = new ArrayList(context.getCollectedMutations());

            for (IMutationOperator operator : mutators) {
                for (MutationDetails detail : details.stream().filter(p -> p.getMutator().equals(operator.getName())).collect(Collectors.toList())) {
                    String uuid = UUID.randomUUID().toString().replace("-", "");
                    Mutant mutant = Mutant.get(uuid, detail.getId(), classToMutate);
                    //System.out.println(mutant.getDetails());
                    mutants.add(mutant);
                }
            }

        }

        return mutants.stream().map((Mutant mutant) -> {
//        ArrayList<MutationIdentifier> details = new ArrayList(context.getCollectedMutations().subList(0, 5).stream().map(MutationDetails::getId).collect(Collectors.toList()));
//        System.out.println("Creating a mutant with order " + details.size());
//
//        Mutant mutant = Mutant.get(details, classToMutate);
            //System.out.println("The new mutant");
            //System.out.println(mutant);


            DynamicClassLoader d = new DynamicClassLoader();

            //"Mutant" + (int)(Math.random()*10000) /* HACK */

            Class m = mutant.compile(context.getJavaClassName(), d);

            //System.out.println(Joiner.on("\n").join(m.getMethods()));

            //System.out.println(m + " " + m.getName());
            Object x;
            try {
                x = m.newInstance();
                return x;
            } catch (Exception e) {
                logger.warn("new instance: {}", e);
                return null;
            }

//        NashornScriptEngine JS = (NashornScriptEngine) new ScriptEngineManager().getEngineByName("nashorn");
//        Bindings b = JS.createBindings();
//        b.put("x", x);
//
//        Object result = null;
//        try {
//            result = JS.eval(js, b);
//            return (X) result;
//        } catch (ScriptException e) {
//            logger.warn("eval: {}", e.getMessage());
//            return null;
//        }

        });
    }

}
