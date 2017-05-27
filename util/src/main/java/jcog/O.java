package jcog;

import com.google.common.base.Joiner;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;
import jcog.list.FasterList;
import org.apache.commons.lang3.ClassUtils;

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

    /** integrates information about 'x' into this */
    protected void learn(Object x) {
        assert (x != this);
        if (x instanceof Class) {
            includeA((Class) x);
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
                includeA(x.getClass());
            }
        }
    }

    private void includeA(Class c) {
        if (c.isInterface() || c.isPrimitive())
            throw new UnsupportedOperationException("classes currently must be concrete implementations");

        if (a.contains(c))
            return; //already mapped

        nextConstructor:
        for (Constructor cc : c.getConstructors()) {
            if (!Modifier.isPublic(cc.getModifiers()))
                continue;


            Parameter[] cp = cc.getParameters();
            List<Parameter> primitives = new FasterList();
            if (cp.length > 0) {
                //determine if some permutation of the vocabulary fits
                for (Parameter p : cp) {
                    Class pt = p.getType();
                    if (pt.isPrimitive()) {
                        primitives.add(p);
                    } else if (!a.contains(pt))
                        break nextConstructor;
                }
            }

            how.addNode(cc);
            how.addNode(c);
            how.putEdge(cc, c);

            for (Parameter p : primitives) {
                how.addNode(p);
                how.putEdge(p, cc);
            }
        }
        //ConstructorUtils.getMatchingAccessibleConstructor(c)
        //ConstructorUtils.invokeConstructor()
        //ClassUtils.

        this.a.add(c);
        ClassUtils.hierarchy(c, ClassUtils.Interfaces.INCLUDE).forEach(this.a::add);
    }


    public <X> X a(Class<X> c) {

        Set<Object> what = Graphs.reachableNodes(how, c);
        MutableGraph<Object> can = Graphs.inducedSubgraph(how, what);

        System.out.println(can);

        return null;
    }


    @Override
    public String toString() {
        return "{" +
                "\n\tor=" + or +
                "\n\t, the=" + the +
                "\n\t, a=" + a +
                "\n\t, is=" + is +
                "\n\t, build=" + Joiner.on("\n\t\t").join(how.edges()) +
                '}';
    }

}
