//package nars.derive;
//
//import br.ufpr.gres.Mutant;
//import br.ufpr.gres.testcase.MutantUnit;
//import nars.$;
//import nars.Narsese;
//import nars.derive.time.Temporalize;
//import org.junit.jupiter.api.Disabled;
//import org.junit.jupiter.api.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.lang.reflect.Method;
//import java.util.Arrays;
//import java.util.stream.Stream;
//
//@Disabled
//public class TemporalizeMutatedTest {
//
//    static final Logger logger = LoggerFactory.getLogger(TemporalizeMutatedTest.class);
//
//    @Test
//    public void testTestCase() {
//        Mutant.mutate(Temporalize.class).forEach(d -> {
//
//            MutantUnit t = new MutantUnit(TemporalizeTest.class, d);
//            t.run();
//
//            System.out.println(d + "\n" + t.results);
//        });
//    }
//
//    @Test
//    public void testMutationDynamic() {
//        Mutant.mutate(Temporalize.class).forEach(d -> {
//            try {
//                System.out.println(d);
//                Object m = d.instance();
//
//
//                invoke(m, "knowTerm", $.$("(a)"), 0L);
//
//                String os = m.toString();
//
//                Object r = invoke(m, "solve", $.$("(a)"));
//
//                System.out.println(d.getDetails());
//                System.out.println("\t" + os + " " + r);
//                System.out.println();
//
//
//            } catch (Narsese.NarseseException | IllegalAccessException | InstantiationException e) {
//                e.printStackTrace();
//            }
//        });
//    }
//
//    public static Object invoke(Object inst, String method, Object... args) {
//        //System.err.println(inst);
//        try {
//
//            Method[] mm = Stream.of(inst.getClass().getMethods()).filter(m -> {
//                if (m.getParameterCount() == args.length && m.getName().equals(method)) {
//                    Class<?>[] parameterTypes = m.getParameterTypes();
//                    boolean fail = false;
//                    for (int i = 0; i < parameterTypes.length; i++) {
//                        Class p = parameterTypes[i];
//
//                        if (p == long.class && args[i].getClass() == Long.class) {
//                            continue;
//                        }
//                        if (!p.isAssignableFrom(args[i].getClass())) {
//                            fail = true;
//                            break;
//                        }
//                    }
//                    if (!fail) {
//                        m.trySetAccessible();
//                        return true;
//                    }
//                }
//                return false;
//            }).toArray(Method[]::new);
//            if (mm.length == 0) {
//                throw new RuntimeException("method not found applicable to args: " + Arrays.toString(args));
//            }
//
//
//            for (Method xm : mm) {
//                xm.trySetAccessible();
//                //System.out.println(xm);
//                try {
//                    return xm.invoke(inst, args);
//                } catch (Throwable e) {
//                    //e.printStackTrace();
//                    //logger.warn(e.getMessage());
//                    return null; //continue;
//                }
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        return null;
//    }
//
//}
