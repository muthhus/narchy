package jcog;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.primitives.Primitives;
import jcog.list.FasterList;
import org.apache.commons.lang3.ClassUtils;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * objenome v3 prototype
 * <p>
 * container instance
 * <p>
 * --configured with:
 * >=0 specified implementation classes ('a')
 * <p>
 * constructor injection
 * <p>
 * >=0 specified singleton-ish instances ('the')
 * <p>
 * >=0 parent containers to inherit from (TODO)
 * <p>
 * --reflectively generates an object dataflow graph
 * for permutable instantiation that detects and allows dynamic non-deterministic construction choices
 * <p>
 * choice of available implementing classes to be instantiated (recursively)
 * <p>
 * choice of per-constructor parameter values (ie. primitives)
 * <p>
 * wrapper proxies (TODO)
 * <p>
 * state saving (TODO)
 * <p>
 * numerical and evolutionary optimization (TODO)
 * <p>
 * quantifiable metrics and unit tests
 * <p>
 * catalog parameter spaces and their performance
 */
public class O {

    public static O of(Object... xx) {
        return new O(xx);
    }

    @Nullable
    public <X> X a(@NotNull Class<X> c, @NotNull How<X> h) {
        Possible<X> p = possible(c);
        Map m = p.permute(h);
        return (m != null) ? p.build(c, h.finish(c, m)) : null;
    }

    /**
     * parents inherited
     */
    final Set<O> or = new HashSet();

    /**
     * singleton instances
     */
    final Set<Object> the = new HashSet();

    /**
     * specified implementations which may be constructed.
     * effectively the "vocabulary" of all types which may possibly be constructed by this
     * or an extension of it
     */
    final Set<Class> a = new HashSet<>();

//    /**
//     * constant property values, named according to the requiree
//     */
//    final Map<String, Class> is = new HashMap();


    final MutableGraph<Object> how = GraphBuilder.
            directed().allowsSelfLoops(false).build();

    protected O(Object... xx) {
        for (Object x : xx)
            learn(x);
    }


    /**
     * integrates information about 'x' into this
     */
    protected void learn(Object x) {
        assert (x != this);
        if (x instanceof Class) {
            Class xx = (Class) x;
            if (xx.isInterface() || xx.isPrimitive() || Modifier.isAbstract(xx.getModifiers()) || !nonSys(xx))
                throw new UnsupportedOperationException("a specified implementation must be concrete: " + xx);
            if (a.add(xx))
                learnClass(xx);
        } else if (x instanceof O) {
            learnAll((O) x);
        } else {
            learnInstance(x);
        }
    }

    private void learnInstance(Object x) {
        if (the.add(x)) {
            Class<?> c = x.getClass();
            ClassUtils.hierarchy(c, ClassUtils.Interfaces.INCLUDE).forEach(s -> {
                if (nonSys(s)) {
                    how.putEdge(x, s); //superclasses assignable from instance 'x'
                }
            });

        }
    }

    private static boolean nonSys(Class<?> s) {
         return s != Object.class && s!=Serializable.class; //ETC
    }

    private void learnAll(O o) {
        how.nodes().addAll(o.how.nodes());
        how.edges().addAll(o.how.edges());
        the.addAll(o.the);
        a.addAll(o.a);
        //is.putAll(o.is);
        the.add(o); //allow it as an instance too
    }

    private <X> void learnClass(Class<? extends X> c) {
        if (c.isPrimitive())
            return;

        how.addNode(c);

        ClassUtils.hierarchy(c, ClassUtils.Interfaces.INCLUDE).forEach(s -> {
            if (s != c && nonSys(s)) {
                how.putEdge(c, s); //superclasses assignable from instance 'x'
            }
        });


        nextConstructor:
        for (Constructor cc : c.getConstructors()) {
            if (!Modifier.isPublic(cc.getModifiers()))
                continue;

            Parameter[] cp = cc.getParameters();
            List<Parameter> primitives = new FasterList();
            List<Parameter> instances = new FasterList();

            for (Parameter p : cp) {
                Class pt = p.getType();
                if (pt.isPrimitive()) {
                    primitives.add(p);
                } else {
                    instances.add(p);
                }
            }

            if (how.addNode(cc)) {
                how.putEdge(cc, c);

                for (Parameter p : primitives) {
                    how.putEdge(p, cc);
                }

                for (Parameter p : instances) {
                    Class i = p.getType();
                    how.putEdge(i, p);
                    how.putEdge(p, cc);

                    //1. find any specified instances to assign
                    the.stream().filter(x -> i.isAssignableFrom(x.getClass())).forEach(x -> {
                        how.putEdge(x, i);
                    });

                    //2. find any specified implementations to assign, and then learn recursively
                    a.stream().filter(i::isAssignableFrom).forEach(x -> {
                        if (i!=x) {
                            how.putEdge(x, i);
                        }
                        learnClass(x);
                    });


                }

            }


        }
        //ConstructorUtils.getMatchingAccessibleConstructor(c)
        //ConstructorUtils.invokeConstructor()
        //ClassUtils.


    }


//    public <X> X a(Class<X> c) {
//        return a(c, null);
//    }

    <X> Possible<X> possible(Class<X> c) {
        return new Possible(c, this);
    }



    public interface How<X> {

        int impl(List choose);

        Object value(Parameter inConstructor);

        /** any final steps or filters can be applied here prior to instantiations */
        default Map finish(@NotNull Class<X> c, Map m) {
            return m;
        }
    }

    static class Possible<X> { //implements Iterable<X> {

        public final MutableGraph p = GraphBuilder.directed()
                //.nodeOrder(ElementOrder.<>natural())
                .allowsSelfLoops(false).build();

        private final Class<X> what;
        private final O o;

        @Override
        public String toString() {
            return "{" + "\n\t\t" + Joiner.on("\n\t\t").join(p.edges()) +
                    //"\n  known=" + Joiner.on("\n\t\t").join(known.entries()) +
                    //"\nunknown=" + Joiner.on("\n\t\t").join(unknown.entries()) +
                    '}';
        }

        Possible(Class<X> what, O o) {
            this.what = what;
            this.o = o;

            solve(what);
        }


        protected boolean solve(Class<X> what) {

            if (p.nodes().contains(what))
                return true; //already solved


            boolean constructable = false;
            for (Object provided : o.the) {
                //choose from the instances if available
                if (what.isAssignableFrom(provided.getClass())) {
                    p.putEdge(what, provided);//, "PROVIDED");
                    constructable = true;
                }
            }
//            if (foundInstances)
//                return true;


            o.learnClass(what);

            Collection<Object> next = //clone to avoid concurrent modification exception
                    Lists.newArrayList( o.how.predecessors(what) ); //first layer priors
            if (!next.isEmpty()) {

                for (Object z : next) {
                    if (z instanceof Constructor) {
                        if (what.isAssignableFrom(((Constructor) z).getDeclaringClass())) {
                            //choose by invoking any constructors, simplest and least exceptions first
                            Constructor constructor = (Constructor) z;

                            boolean foundConstructor = true;
                            for (Parameter parameter : constructor.getParameters()) {
                                Class pt = parameter.getType();
                                if (pt.isPrimitive()) {
                                    p.putEdge(constructor, parameter); //primitive parameter
                                } else if (!solve(pt)) {
                                    foundConstructor = false;
                                    break;
                                } else {
                                    //solved
                                    p.putEdge(parameter, pt);
                                    p.putEdge(constructor, parameter); //instance parameter
                                }
                            }
                            if (foundConstructor) {
                                p.putEdge(what, constructor); //, "CONSTRUCT");
                                constructable = true;
                            }
                        }
                    } else if (z instanceof Class) {
                        //assert(z assignable from.. )
                        //recurse down the type hierarchy
                        if (solve((Class) z)) {
                            p.putEdge(what, z);
                            constructable = true;
                        }
                    } else {
                        Class<?> zg = z.getClass();
                        if (what.isAssignableFrom(zg)) {
                            if (solve((Class)zg)) {
                                if (what!=zg)
                                    p.putEdge(what, zg);
                                constructable = true;
                            }
                        }
                    }
                }
            }

            return constructable;
        }

        public Map permute(@NotNull How h) {
            Map m = new HashMap();
            if (!get(what, h, m)) {
                return null;
            }
            return m;
        }

        protected boolean get(@NotNull Object current, How h, Map m) {

            Set<Object> next = p.successors(current);
            int s = next.size();
            switch (s) {
                case 0:
                    if (current instanceof Parameter) {
                        Parameter pc = (Parameter) current;
                        Class<?> pt = pc.getType();
                        Object v = h.value(pc);
                        if (v == null)
                            v = Void.class;
                        else if (pt.isPrimitive() /* or string etc */) {
                            if (Primitives.unwrap(v.getClass()) != pt)
                                v = Void.class;
                        } else {
                            if (!pc.getType().isAssignableFrom(v.getClass()))
                                throw new ClassCastException();
                        }
                        m.put(pc, v);
                    }
                    return true;
                case 1:
                    return get(next.iterator().next(), h, m);
                default:
                    //TODO argument types should be bound in argument,type pairs so they can each have unique assigned impl
                    if (current.getClass() == Class.class) {
                        //choose one implementation
                        List nn = Lists.newArrayList(next);
                        int n = h.impl(nn);
                        if (n < 0 || n >= s)
                            return false; //invalid choice

                        Object impl = nn.get(n);
                        m.put(current, impl);
                        return get(impl, h,  m);
                    } else {
                        return next.stream().allMatch(n -> get(n, h, m));
                    }
            }
        }

        public <X> X build(@NotNull Class<X> c, Map m) {
            m.forEach((k,v)->{
                System.out.println("\t" + k + "\t = " + v);
            });
            return null; //TODO
        }


    }


    @Override
    public String toString() {
        return "{" +
                "\n\tor=" + or +
                "\n\t, the=" + the +
                "\n\t, a=" + a +
                "\n\t, how=" + Joiner.on("\n\t\t").join(how.edges()) +
                '}';
    }

}
