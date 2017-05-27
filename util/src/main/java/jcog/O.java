package jcog;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.primitives.Primitives;
import jcog.list.FasterList;
import org.apache.commons.lang3.ClassUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.type.PrimitiveType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;

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
                if (s != c && s != Object.class) {
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
                    the.stream().filter(x -> i.isAssignableFrom(x.getClass())).forEach(x -> {
                        how.putEdge(x, i);
                    });

                    //2. find any specified implementations to assign, and then learn recursively
                    a.stream().filter(i::isAssignableFrom).forEach(x -> {
                        how.putEdge(x, i);
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

    public <X> Possible<X> possible(Class<X> c) {
        return new Possible(c, this);
    }

    @Nullable
    public <X> X get(@NotNull Class<X> c, @NotNull How h) {
        Possible<X> p = possible(c);
        if (p == null)
            return null;

        Map m = new HashMap();
        if (get(c, h, p, m)) {
            //use 'm' to construct return value
            System.out.println(m);
        }
        return null;
    }

    private boolean get(@NotNull Object current, How h, Possible p, Map m) {

        Set<Object> next = p.p.successors(current);
        int s = next.size();
        switch (s) {
            case 0:
                if (current instanceof Parameter) {
                    Parameter pc = (Parameter) current;
                    Class<?> pt = pc.getType();
                    Object v = h.value(pc);
                    if (v == null)
                        return false;
                    if (pt.isPrimitive() /* or string etc */) {
                        if (Primitives.unwrap(v.getClass()) != pt)
                            return false;
                    } else {
                        if (!pc.getType().isAssignableFrom(v.getClass()))
                            throw new ClassCastException();
                    }
                    m.put(pc, v);
                }
                return true;
            case 1:
                return get(next.iterator().next(), h, p, m);
            default:
                if (current.getClass() == Class.class) {
                    //choose one implementation
                    List nn = Lists.newArrayList(next);
                    int n = h.which(nn);
                    if (n < 0 || n >= s)
                        return false; //invalid choice
                    return get(nn.get(n), h, p, m);
                } else {
                    return next.stream().allMatch(n -> get(n, h, p, m));
                }
        }
    }


    public interface How {
        int which(List options);

        Object value(Parameter inConstructor);
    }

    public static class Possible<X> { //implements Iterable<X> {

        public final MutableGraph<Object> p = GraphBuilder.directed().allowsSelfLoops(false).build();

        private final Class<X> what;
        private final O o;

        @Override
        public String toString() {
            return "{" + "\n\t\t" + Joiner.on("\n\t\t").join(p.edges()) +
                    //"\n  known=" + Joiner.on("\n\t\t").join(known.entries()) +
                    //"\nunknown=" + Joiner.on("\n\t\t").join(unknown.entries()) +
                    '}';
        }

        public Possible(Class<X> what, O o) {
            this.what = what;
            this.o = o;

            solve(what);
        }


        protected boolean solve(Class<X> what) {

            if (p.nodes().contains(what))
                return true; //already solved


            boolean foundInstances = false;
            for (Object provided : o.the) {
                //choose from the instances if available
                if (what.isAssignableFrom(provided.getClass())) {
                    p.putEdge(what, provided);//, "PROVIDED");
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
