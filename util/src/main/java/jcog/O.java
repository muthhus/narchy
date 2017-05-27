package jcog;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import jcog.list.FasterList;
import org.apache.commons.lang3.ClassUtils;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Consumer;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * objenome v3 prototype
 */
public class O {

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

    /**
     * constant property values, named according to the requiree
     */
    final Map<String, Class> is = new HashMap();


    final MutableGraph<Object> how = GraphBuilder.
            directed().allowsSelfLoops(false).build();

    public static O of(Object... xx) throws RuntimeException {
        O o = new O();
        for (Object x : xx)
            o.learn(x);
        return o;
    }

    /**
     * integrates information about 'x' into this
     */
    protected void learn(Object x) {
        assert (x != this);
        if (x instanceof Class) {
            Class xx = (Class) x;
            if (xx.isInterface() || xx.isPrimitive() || Modifier.isAbstract(xx.getModifiers()))
                throw new UnsupportedOperationException("a specified implementation must be concrete: " + xx);
            if (a.add(xx))
                learnClass(xx);
        } else if (x instanceof O) {
            learnAll(x);
        } else {
            learnInstance(x);
        }
    }

    private void learnInstance(Object x) {
        if (the.add(x)) {
            Class<?> c = x.getClass();
            ClassUtils.hierarchy(c, ClassUtils.Interfaces.INCLUDE).forEach(s -> {
                if (s!=c && s!=Object.class) {
                    how.putEdge(x, s); //superclasses assignable from instance 'x'
                }
            });

        }
    }

    private void learnAll(Object x) {
        O o = (O) x;
        how.nodes().addAll(o.how.nodes());
        how.edges().addAll(o.how.edges());
        the.addAll(o.the);
        a.addAll(o.a);
        is.putAll(o.is);
        the.add(x); //allow it as an instance too
    }

    private <X> void learnClass(Class<? extends X> c) {
        if (c.isPrimitive())
            return;

        if (!how.addNode(c))
            return;

        ClassUtils.hierarchy(c, ClassUtils.Interfaces.INCLUDE).forEach(s -> {
            if (s != c && s != Object.class) {
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
                    long specified = the.stream().filter(x -> i.isAssignableFrom(x.getClass())).peek(x -> {
                        how.putEdge(x, p);
                    }).count();

                    //2. find any specified implementations to assign, and then learn recursively
                    if (specified == 0) {
                        specified = a.stream().filter(i::isAssignableFrom).peek(x -> {
                            if (how.putEdge(x, p))
                                learnClass(x);
                        }).count();
                    }

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

    public <X> How<X> how(Class<X> c) {
        return new How(c, this);
    }

//    public static class Constructable<X> {
//
//        public final ImmutableGraph<Object> known;
//        public final Map<String,Object> unknown = new HashMap();
//        public final Class<X> target;
//
//        public Constructable(Class<X> x, ImmutableGraph<Object> known) {
//            this.target = x;
//            this.known = known;
//        }
//
//        @Override
//        public String toString() {
//            return "Constructable{" +
//                    "known=" + known +
//                    ", unknown=" + Joiner.on("\n\t\t").join(unknown.entrySet()) +
//                    '}';
//        }
//
//        public boolean get(Object disambiguationStrategy, Consumer<X> ifComplete) {
//            if (unknown.isEmpty() && !known.edges().isEmpty()) {
//                //follow plan
//            }
//            //return null;
//            return false;
//        }
//    }

    public static class How<X> { //implements Iterable<X> {

        public final HashMultimap<Constructor, Parameter> unknown = HashMultimap.create();
        public final HashMultimap<Object, Object> known = HashMultimap.create();

        private final Class<X> what;
        private final O o;

        @Override
        public String toString() {
            return "{" +
                    "\n  known=" + Joiner.on("\n\t\t").join(known.entries()) +
                    "\nunknown=" + Joiner.on("\n\t\t").join(unknown.entries()) +
                    '}';
        }

        public How(Class<X> what, O o) {
            this.what = what;
            this.o = o;

            solve(what);
        }

        protected boolean solve(Class<X> what) {

            if (known.containsKey(what))
                return true; //already solved


            boolean foundInstances = false;
            for (Object z : o.the) {
                //choose from the instances if available
                if (what.isAssignableFrom(z.getClass())) {
                    known.put(what, z);
                    foundInstances = true;
                }
            }
            if (foundInstances)
                return true;


            o.learnClass(what);
            Set<Object> next = o.how.predecessors(what); //first layer priors
            if (next.isEmpty())
                return false;

            boolean constructable = false;
            for (Object z : next) {
                if (z instanceof Constructor) {
                    if (what.isAssignableFrom(((Constructor) z).getDeclaringClass())) {
                        //choose by invoking any constructors, simplest and least exceptions first
                        Constructor cz = (Constructor) z;

                        boolean foundConstructor = true;
                        for (Parameter p : cz.getParameters()) {
                            Class pt = p.getType();
                            if (pt.isPrimitive()) {
                                unknown.put(cz, p);
                            } else if (!solve(pt)) {
                                foundConstructor = false;
                                break;
                            }
                        }
                        if (foundConstructor) {
                            known.put(what, cz);
                            constructable = true;
                        }
                    }
                } else if (z instanceof Class) {
                    //assert(z assignable from.. )
                    //recurse down the type hierarchy
                    return solve((Class)z);
                }
            }

            if (!constructable)
                return false;


//            if (!o.how.nodes().contains(what))
//                return false;


            return true;
        }




        /*@NotNull
        @Override
        public Iterator<X> iterator() {
            return null;
        }*/
    }


    @Override
    public String toString() {
        return "{" +
                "\n\tor=" + or +
                "\n\t, the=" + the +
                "\n\t, a=" + a +
                "\n\t, is=" + is +
                "\n\t, how=" + Joiner.on("\n\t\t").join(how.edges()) +
                '}';
    }

}
