package nars.op.java;

import com.google.common.collect.ImmutableSet;
import nars.$;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.var.Variable;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.impl.bimap.mutable.HashBiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Created by me on 8/19/15.
 */
public class DefaultTermizer implements Termizer {


    public static final Atomic PACKAGE = Atomic.the("package");
    public static final Atomic PRIMITIVE = Atomic.the("primitive");
    public static final Variable INSTANCE_VAR = $.varDep("instance");

    final Map<Package, Term> packages = new HashMap();
    final Map<Class, Term> classes = new HashMap();


    final HashBiMap<Term,Object> instances = new HashBiMap();

    /*final HashMap<Term, Object> instances = new HashMap();
    final HashMap<Object, Term> objects = new HashMap();*/

    static final Set<Class> classInPackageExclusions = ImmutableSet.of(
        Class.class,
        Object.class,

        //since autoboxing can be managed, the distinction between boxed and unboxed values should not be seen by reasoner
        Float.class,
        Double.class,
        Boolean.class,
        Character.class,
        Long.class,
        Integer.class,
        Short.class,
        Byte.class,
        Class.class
    );

    public DefaultTermizer() {
        map(NULL, null);
        map(TRUE, true);
        map(FALSE, false);
    }

    public void map(Term x, Object y) {
        instances.put(x, y);
    }

    /** dereference a term to an object (but do not un-termize) */
    @Nullable
    @Override public Object object(Term t) {

        if (t == NULL) return null;

        Object x = instances.get(t);
        if (x == null)
            return t; /** return the term intance itself */

        return x;
    }


    @Nullable
    Term obj2term(@Nullable Object o) {

        if (o == null)
            return NULL;


        if (o instanceof Term) return (Term)o;

        if (o instanceof String)
            return $.quote(o);

        if (o instanceof Boolean)
            return ((Boolean) o) ? TRUE : FALSE;

        if (o instanceof Character)
            return $.quote(String.valueOf(o));

        if (o instanceof Number)
            return number((Number)o);

        if (o instanceof Class) {
            Class oc = (Class) o;
            return classTerm(oc);

//            if (metadata) {
//                Package p = oc.getPackage();
//                if (p != null) {
//
//                    Term cterm = termClassInPackage(oc);
//
//                    if (reportClassInPackage(oc)) { //TODO use a method for other class exclusions
//                        Term pkg = packages.get(p);
//                        if (pkg == null) {
//                            pkg = termPackage(p);
//                            packages.put(p, pkg);
//                            termClassInPackage(cterm, PACKAGE);
//                        }
//
//                        //TODO add recursive superclass ancestry?
//                    }
//
//                    return cterm;
//                }
//            }



            //return PRIMITIVE;
        }

        if (o instanceof int[]) {
            List<Term> arg = Arrays.stream((int[]) o)
                    .mapToObj($::the).collect(Collectors.toList());
            if (arg.isEmpty()) return EMPTY;
            return $.p( arg );
        }

        //noinspection IfStatementWithTooManyBranches
        if (o instanceof Object[]) {
            List<Term> arg = Arrays.stream((Object[]) o).map(this::term).collect(Collectors.toList());
            if (arg.isEmpty()) return EMPTY;
            return $.p( arg );
        }

        if (o instanceof List) {
            if (((List)o).isEmpty()) return EMPTY;

            //TODO can this be done with an array to avoid duplicate collection allocation


            Collection c = (Collection) o;
            List<Term> arg = $.newArrayList(c.size());
            for (Object x : c) {
                Term y = term(x);
                arg.add(y);
            }

            if (arg.isEmpty()) return EMPTY;

            return $.p(arg);

        /*} else if (o instanceof Stream) {
            return Atom.quote(o.toString().substring(17));
        }*/
        }

        if (o instanceof Set) {
            Collection<Term> arg = (Collection<Term>) ((Collection) o).stream().map(this::term).collect(Collectors.toList());
            if (arg.isEmpty()) return EMPTY;
            return $.sete(arg);
        } else if (o instanceof Map) {

            Map mapo = (Map) o;
            Set<Term> components = $.newHashSet(mapo.size());
            mapo.forEach((k, v) -> {

                Term tv = obj2term(v);
                Term tk = obj2term(k);

                if ((tv != null) && (tk!=null)) {
                    components.add(
                        $.inh(tv, tk)
                    );
                }
            });
            if (components.isEmpty()) return EMPTY;
            return $.sete(components);
        }

//        else if (o instanceof Method) {
//            //translate the method to an operation term
//            Method m = (Method)o;
//            return getOperation(m, getMethodArgVariables(m));
//        }

        return instanceTerm(o);


//        //ensure package is term'ed
//        String pname = p.getName();
//        int period = pname.length()-1;
//        int last = period;
//        Term child = cterm;
//        while (( period = pname.lastIndexOf('.', period)) != -1) {
//            String parname = pname.substring(0, last);
//            Term parent = packages.get(parname);
//            if (parent == null) {
//                parent = Atom.the(parname);
//                nar.believe( Inheritance.make(child, parent) );
//                packages.put()
//                last = period;
//                child = parent;
//            }
//            else {
//                break;
//            }
//        }


    }

    protected static Term number(Number o) {
        return $.the(o);
    }

//    @NotNull
//    public Compound getOperation(@NotNull Method m, Term[] args) {
//        return getMethodOperator(m, args);
//    }

    private boolean reportClassInPackage(@NotNull Class oc) {
        if (classInPackageExclusions.contains(oc)) return false;

        if (Term.class.isAssignableFrom(oc)) return false;
        return !oc.isPrimitive();


    }


    /** (#arg1, #arg2, ...), #returnVar */
    @NotNull
    private Term[] getMethodArgVariables(@NotNull Method m) {

        //TODO handle static methods which will not receive first variable instance

        String varPrefix = m.getName() + '_';
        int n = m.getParameterCount();
        Compound args = $.p(getArgVariables(varPrefix, n));

        return m.getReturnType() == void.class ? new Term[]{
                INSTANCE_VAR,
                args
        } : new Term[]{
                INSTANCE_VAR,
                args,
                $.varDep(varPrefix + "_return") //return var
        };
    }

    @NotNull
    private static Term[] getArgVariables(String prefix, int numParams) {
        Term[] x = new Term[numParams];
        for (int i = 0; i < numParams; i++) {
            x[i] = $.varDep(prefix + i);
        }
        return x;
    }

    public static Term classTerm(@NotNull Class c) {
        //return Atom.the(Utf8.toUtf8(name));

        return Atomic.the(c.getSimpleName());

//        int olen = name.length();
//        switch (olen) {
//            case 0:
//                throw new RuntimeException("empty atom name: " + name);
//
////            //re-use short term names
////            case 1:
////            case 2:
////                return theCached(name);
//
//            default:
//                if (olen > Short.MAX_VALUE/2)
//                    throw new RuntimeException("atom name too long");

        //  }
    }

    public static Term termClassInPackage(@NotNull Class c) {
        return $.p(termPackage(c.getPackage()), classTerm(c));
    }

    @NotNull
    public static Term termPackage(@NotNull Package p) {
        //TODO cache?
        if (p == null) return Atomic.the("package");
        String n = p.getName();
        //if (n == null) return $.the(p.toString());
        String[] path = n.split("\\.");
        return $.p(path);

        //return Atom.the(p.getName());
    }

    /** generic instance term representation */
    public static Term instanceTerm(@NotNull Object o) {
        //return o.getClass().getName() + '@' + Integer.toHexString(o.hashCode());
        //return o.getClass() + "_" + System.identityHashCode(o)

        //                Term clas = classes.computeIfAbsent(o.getClass(), this::obj2term);
//
//                Term finalClas = clas;
//                post[0] = () -> onInstanceOfClass(o, oterm, finalClas);

        //instances.put(oterm, o); //reverse

        return $.p(
                    termPackage(o.getClass().getPackage()),
                    termClassInPackage(o.getClass()),
                    $.the(System.identityHashCode(o), 36)
                );
    }

    @Nullable
    protected Term termClassInPackage(Term classs, @Deprecated Term packagge) {
        //TODO ??
        return null;
    }

    @Override
    @Nullable
    public Term term(@Nullable Object o) {
        if (o == null) return NULL;

        //        String cname = o.getClass().toString().substring(6) /* "class " */;
//        int slice = cname.length();
//
        Runnable[] post = new Runnable[1];


        Term result = obj2termCached(o, post);

        if (result!=null)
            if (post[0]!=null)
                post[0].run();

        return result;


        //TODO decide to use toString or System object id
        //String instanceName = o.toString();
//        if (instanceName.length() > slice)
//            instanceName = instanceName.substring(slice);

        //final Term oterm = Atom.quote(instanceName);

//        Term prevOterm = objects.put(o, oterm);
        //if (prevOterm == null) {


        //}
//        else {
//            if (!oterm.equals(prevOterm)) {
//                //toString value has changed, create similarity to associate
//                onInstanceChange(oterm, prevOterm);
//            }
//        }

        //return oterm;
    }

    @Nullable
    public Term obj2termCached(@Nullable Object o, Runnable[] post) {

        if (o == null) return NULL;
        if (o instanceof Term)
            return ((Term)o);

        Term oe;
        if (cacheableInstance(o)) {

            MutableBiMap<Object, Term> iii = instances.inverse();
            oe = iii.get(o); //computeifAbsent crashes because it can go recursive
            if (oe == null) {


                oe = iii.computeIfAbsent(o, this::obj2term);
            }
        } else {
            oe = obj2term(o);
        }

        return oe;
    }

    private boolean cacheableInstance(Object o) {
//        if (o instanceof Float)
//            return false;
        return true;
    }


    protected void onInstanceChange(Term oterm, Term prevOterm) {

    }

    protected void onInstanceOfClass(Object o, Term oterm, Term clas) {

    }

    @NotNull
    public static <T extends Term> Map<Atomic,T> mapStaticClassFields(@NotNull Class c, @NotNull Function<Field, T> each) {
        Field[] ff = c.getFields();
        Map<Atomic,T> t = $.newHashMap(ff.length);
        for (Field f : ff) {
            if (Modifier.isStatic(f.getModifiers())) {
                T xx = each.apply(f);
                if (xx!=null) {
                    t.put(Atomic.the(f.getName()), xx);
                }
            }
        }
        return t;
    }


}
