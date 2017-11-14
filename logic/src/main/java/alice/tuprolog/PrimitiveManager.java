/*
 * tuProlog - Copyright (C) 2001-2002  aliCE team at deis.unibo.it
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package alice.tuprolog;

import alice.tuprolog.interfaces.IPrimitiveManager;
import jcog.list.FasterList;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMapUnsafe;

import java.lang.reflect.InvocationTargetException;
import java.util.*;


/**
 * Administration of primitive predicates
 *
 * @author Alex Benini
 */
public class PrimitiveManager /*Castagna 06/2011*/ implements IPrimitiveManager/**/ {

    private static final Set<String> PRIMITIVE_PREDICATES = Set.of(",", "':-'", ":-");
    private final Map<IPrimitives, List<PrimitiveInfo>> libHashMap;
    private final Map<String, PrimitiveInfo> directives;
    private final Map<String, PrimitiveInfo> predicates;
    private final Map<String, PrimitiveInfo> functors;

    public PrimitiveManager() {
        libHashMap = //Collections.synchronizedMap(new IdentityHashMap<IPrimitives, List<PrimitiveInfo>>());
                new IdentityHashMap();
        directives = //Collections.synchronizedMap
                new ConcurrentHashMapUnsafe();
        predicates = //Collections.synchronizedMap(
                new ConcurrentHashMapUnsafe();
        functors = //Collections.synchronizedMap(new HashMap<String,PrimitiveInfo>());
                new ConcurrentHashMapUnsafe();
    }

    /**
     * Config this Manager
     */
    void initialize(Prolog vm) {
        createPrimitiveInfo(new BuiltIn(vm));
    }

    void createPrimitiveInfo(IPrimitives src) {
        Map<Integer, List<PrimitiveInfo>> prims = src.getPrimitives();
        Iterator<PrimitiveInfo> it = prims.get(PrimitiveInfo.DIRECTIVE).iterator();
        while (it.hasNext()) {
            PrimitiveInfo p = it.next();
            directives.put(p.key, p);
        }
        it = prims.get(PrimitiveInfo.PREDICATE).iterator();
        while (it.hasNext()) {
            PrimitiveInfo p = it.next();
            predicates.put(p.key, p);
        }
        it = prims.get(PrimitiveInfo.FUNCTOR).iterator();
        while (it.hasNext()) {
            PrimitiveInfo p = it.next();
            functors.put(p.key, p);
        }
        List<PrimitiveInfo> primOfLib =
                //new LinkedList<>(prims.get(PrimitiveInfo.DIRECTIVE));
                new FasterList();
        primOfLib.addAll(prims.get(PrimitiveInfo.DIRECTIVE));
        primOfLib.addAll(prims.get(PrimitiveInfo.PREDICATE));
        primOfLib.addAll(prims.get(PrimitiveInfo.FUNCTOR));
        libHashMap.put(src, primOfLib);
    }


    void deletePrimitiveInfo(IPrimitives src) {
        for (PrimitiveInfo primitiveInfo : libHashMap.remove(src)) {
            String k = primitiveInfo.key;
            directives.remove(k);
            predicates.remove(k);
            functors.remove(k);
        }
    }


    /**
     * Identifies the term passed as argument.
     * <p>
     * This involves identifying structs representing builtin
     * predicates and functors, and setting up related structures and links
     *
     * @return term with the identified built-in directive
     * @parm term the term to be identified
     */
    public Term identifyDirective(Term term) {
        identify(term, PrimitiveInfo.DIRECTIVE);
        return term;
    }

    public boolean evalAsDirective(Struct d) throws Throwable {
        PrimitiveInfo pd = ((Struct) identifyDirective(d)).getPrimitive();
        if (pd != null) {
            try {
                pd.evalAsDirective(d);
                return true;
            } catch (InvocationTargetException ite) {
                throw ite.getTargetException();
            }
        } else
            return false;
    }

    void identifyPredicate(Term term) {
        identify(term, PrimitiveInfo.PREDICATE);
    }

    void identifyFunctor(Term term) {
        identify(term, PrimitiveInfo.FUNCTOR);
    }

    private void identify(Term term, int typeOfPrimitive) {
        if (term == null) {
            return;
        }
        term = term.term();
        if (!(term instanceof Struct)) {
            return;
        }
        Struct t = (Struct) term;

        int arity = t.subs();
        String name = t.name();
        //------------------------------------------
        int primType;

        primType = PRIMITIVE_PREDICATES.contains(name) ? PrimitiveInfo.PREDICATE : PrimitiveInfo.FUNCTOR;

        for (int c = 0; c < arity; c++) {
            identify(t.sub(c), primType);
        }

        //------------------------------------------
        //log.debug("Identification "+t);    

        Map<String, PrimitiveInfo> map = null;
        switch (typeOfPrimitive) {
            case PrimitiveInfo.DIRECTIVE:
                map = directives;
                break;
            case PrimitiveInfo.PREDICATE:
                map = predicates;
                break;
            case PrimitiveInfo.FUNCTOR:
                map = functors;
                break;
            default:
                throw new UnsupportedOperationException();
        }

        String key = name + '/' + arity;
        t.setPrimitive(map.get(key));
    }


    Library getLibraryDirective(String name, int nArgs) {
        try {
            return (Library) directives.get(name + '/' + nArgs).source;
        } catch (NullPointerException e) {
            return null;
        }
    }

    Library getLibraryPredicate(String name, int nArgs) {
        try {
            return (Library) predicates.get(name + '/' + nArgs).source;
        } catch (NullPointerException e) {
            return null;
        }
    }

    Library getLibraryFunctor(String name, int nArgs) {
        try {
            return (Library) functors.get(name + '/' + nArgs).source;
        } catch (NullPointerException e) {
            return null;
        }
    }

    /*Castagna 06/2011*/
    @Override
    public boolean containsTerm(String name, int nArgs) {
        return (functors.containsKey(name + '/' + nArgs) || predicates.containsKey(name + '/' + nArgs));
    }
    /**/
}