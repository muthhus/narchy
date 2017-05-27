package jcog;

import com.google.common.base.Joiner;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;
import jcog.list.FasterList;
import org.apache.commons.lang3.ClassUtils;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;
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
            this.learnClass((Class) x, null);
        } else if (x instanceof O) {
            O o = (O) x;
            how.nodes().addAll(o.how.nodes());
            how.edges().addAll(o.how.edges());
            the.addAll(o.the);
            a.addAll(o.a);
            is.putAll(o.is);
            the.add(x); //allow it as an instance too
        } else {
            if (the.add(x)) {
                this.learn(x.getClass());
            }
        }
    }

    private <X> void learnClass(Class<X> c, @Nullable Consumer<Pair<Constructor<X>, Parameter>> wonder) {
        if (c.isInterface() || c.isPrimitive() || Modifier.isAbstract(c.getModifiers()))
            return; //throw new UnsupportedOperationException("classes currently must be concrete implementations");

        boolean novel = a.add(c);

        if (!novel && wonder == null)
            return; //nothing to do with this already learned type at this time

        how.addNode(c);

        nextConstructor:
        for (Constructor cc : c.getConstructors()) {
            if (!Modifier.isPublic(cc.getModifiers()))
                continue;

            Parameter[] cp = cc.getParameters();
            List<Parameter> primitives = new FasterList();
            List<Class> instances = new FasterList();
            if (cp.length > 0) {
                //determine if some permutation of the vocabulary fits
                for (Parameter p : cp) {
                    Class pt = p.getType();
                    if (pt.isPrimitive()) {
                        primitives.add(p);
                    } else {
                        //if (!how.nodes().add(pt))
                        if (a.contains(pt))
                            instances.add(pt);
                        else {
                            if (!a.contains(pt)) {
                                if (wonder != null) {
                                    wonder.accept(pair(cc, p));
                                }
                                break nextConstructor;
                            }
                        }
                    }
                }
            }


            if (how.addNode(cc)) {
                how.putEdge(cc, c);

                for (Parameter p : primitives) {
                    how.addNode(p);
                    how.putEdge(p, cc);
                    if (wonder!=null && !is.containsKey(p))
                        wonder.accept(pair(cc,p));
                }
                for (Class i : instances) {
                    how.putEdge(i, cc);
                    learnClass(i, wonder);
                }
            }


        }
        //ConstructorUtils.getMatchingAccessibleConstructor(c)
        //ConstructorUtils.invokeConstructor()
        //ClassUtils.

        if (novel) {
            how.addNode(c);

//            ClassUtils.hierarchy(c, ClassUtils.Interfaces.EXCLUDE).forEach(s -> {
//                if (s!=c && s!=Object.class) {
//                    a.add(s);
//                    how.putEdge(c, s); //assignable from
//                }
//            });
        }
    }


//    public <X> X a(Class<X> c) {
//        return a(c, null);
//    }

    public <X> How<X> how(Class<X> c) {
        return new How(c, this);
    }

    public static class How<X> { //implements Iterable<X> {
        public final Set<Pair<Constructor<X>, Parameter>> wonder = new HashSet();
        public final MutableGraph<Object> plan;
        private final Class<X> what;

        public How(Class<X> what, O o) {
            this.what = what;

            o.learnClass(what, wonder::add);

            //((ConfigurableMutableGraph) o.how).predecessors(what)

            Set<Object> before = Graphs.reachableNodes(o.how, what);
            this.plan = Graphs.inducedSubgraph(o.how, before);
        }

        /** returns the first successfully constructed possibility */
        @Nullable public X get(/* strategy */) {
            return get(what);
        }

        @Nullable public <Y> Y get(Class<Y> y/* Strategy... */) {
            //first layer priors
            Set<Object> before = plan.successors(y);
            if (before.isEmpty())
                return null;

            //Strategy 1. choose from the instances if available
            List instances = before.stream()
                    .filter(p -> y.isAssignableFrom(p.getClass()))
                    .collect(toList());
            if (!instances.isEmpty())
                return (Y) instances.get(ThreadLocalRandom.current().nextInt(instances.size()));

            //Strategy 2. choose by invoking any constructors, simplest and least exceptions first
            List constructors = before.stream()
                    .filter(z -> z instanceof Constructor)
                    .filter(c -> y.isAssignableFrom( ((Constructor)c).getDeclaringClass() ) )
                    //.sorted(x -> x.) //TODO
                    .collect(toList());
            if (!constructors.isEmpty()) {
                Constructor c = (Constructor) constructors.get(0);
            }

            return null;

        }


        @Override
        public String toString() {
            return "{" +
                    "plan=" + Joiner.on("\n\t\t").join(plan.edges()) +
                    "\nwonder=" + Joiner.on("\n\t\t").join(wonder) +
                    '}';
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
