/*
 * Variable.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.term.var;


import jcog.Util;
import nars.$;
import nars.Op;
import nars.Param;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Normalized variable
 * "highly immutable" and re-used
 */
public abstract class AbstractVariable implements Variable {

    /** lowest 4 bits specify the type of variable */
    public final int id;

    static final byte DEP_ORD = Op.VAR_DEP.id;
    static {

        //test for the expected ordering of the variable op ordinals
        assert(Op.VAR_PATTERN.id - DEP_ORD == 3);
    }

    private final byte[] bytesCached;

    protected AbstractVariable(/*@NotNull*/ Op type, int num) {
        assert(num > 0);
        assert(num < Byte.MAX_VALUE);

        int id = num << 2 | (type.id-DEP_ORD);
        this.id = id;

        byte[] b = new byte[5];
        b[0] = type.id;
        Util.int2Bytes(num, b, 1);
        this.bytesCached = b;
    }


    @Override
    public final byte[] bytes() {
        return bytesCached;
    }

    @Override
    public final byte id() {
        return (byte) (id >> 2);
    }

    //@Override abstract public boolean equals(Object other);

    @Override
    public final boolean equals(Object obj) {
        return obj == this ||
                (obj instanceof AbstractVariable
                        && ((AbstractVariable) obj).id == id);
    }


    //    @Override
//    public boolean equals(Object that) {
//        boolean e = that == this ||
//                (that instanceof AbstractVariable && that.hashCode() == hash);
//
//        if (e) {
//            if (!toString().equals(that.toString()))
//                System.err.println("warning: " + this + " and " + that + " are not but considered equal");
//            return true;
//        } else {
//            if (toString().equals(that.toString()))
//                System.err.println("warning: " + this + " and " + that + " are but considered not equal");
//            return false;
//        }
//    }


    @Override
    public @Nullable Variable normalize(int vid) {
        if (id() == vid)
            return this;
        else
            return $.v(op(), vid);
    }


    @Override
    public final int hashCode() {
        return id;
    }


    @NotNull
    @Override
    public String toString() {
        return op().ch + Byte.toString(id()); //Integer.toString(id);;
    }

    /**
     * numerically-indexed variable instance cache; prevents duplicates and speeds comparisons
     */
    private static final AbstractVariable[][] varCache = new AbstractVariable[4][Param.MAX_VARIABLE_CACHED_PER_TYPE];

    @NotNull
    public static Op typeIndex(char c) {
        switch (c) {
            case '%':
                return Op.VAR_PATTERN;
            case '#':
                return Op.VAR_DEP;
            case '$':
                return Op.VAR_INDEP;
            case '?':
                return Op.VAR_QUERY;
        }
        throw new RuntimeException("invalid variable character");
    }

    //    @NotNull
//    static Op varCacheFor(Op c) {
//        switch (c) {
//            case Op.VAR_PATTERN:
//                return varCache[0]
//            case '#':
//                return varCacheFor
//            case '$':
//                return Op.VAR_INDEP;
//            case '?':
//                return Op.VAR_QUERY;
//        }
//        throw new RuntimeException(c + " not a variable");
//    }
    public static int opToVarIndex(/*@NotNull*/ Op o) {
        switch (o) {
            case VAR_PATTERN:
                return 0;
            case VAR_DEP:
                return 1;
            case VAR_INDEP:
                return 2;
            case VAR_QUERY:
                return 3;
            default:
                throw new UnsupportedOperationException();
        }
    }


//    @Override
//    abstract public int volume();

    static {
        //precompute cached variable instances
        for (Op o : new Op[]{Op.VAR_PATTERN, Op.VAR_QUERY, Op.VAR_DEP, Op.VAR_INDEP}) {
            int t = opToVarIndex(o);
            for (int i = 1; i < Param.MAX_VARIABLE_CACHED_PER_TYPE; i++) {
                varCache[t][i] = vNew(o, i);
            }
        }
    }

    /**
     * TODO move this to TermBuilder
     */
    @NotNull
    static AbstractVariable vNew(/*@NotNull*/ Op type, int id) {
        switch (type) {
            case VAR_PATTERN:
                return new VarPattern(id);
            case VAR_QUERY:
                return new VarQuery(id);
            case VAR_DEP:
                return new VarDep(id);
            case VAR_INDEP:
                return new VarIndep(id);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public static AbstractVariable the(/*@NotNull*/ Op type, int id) {
        assert(id > 0);
        if (id >= Param.MAX_VARIABLE_CACHED_PER_TYPE) {
            return AbstractVariable.vNew(type, id); //for special variables like ellipsis
        } else {
            return AbstractVariable.varCache[AbstractVariable.opToVarIndex(type)][id];
        }
    }




    //    //TODO replace this with a generic counting method of how many subterms there are present
//    public static int numPatternVariables(Term t) {
//        t.value(new TermToInt()) //..
////        final int[] has = {0};
////        t.recurseTerms((t1, superterm) -> {
////            if (t1.op() == Op.VAR_PATTERN)
////                has[0]++;
////        });
////        return has[0];
//    }


}
