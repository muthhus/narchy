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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Double class represents the double prolog data type
 *
 */
public class Double extends Number {
	private static final long serialVersionUID = 1L;
    private final double value;
    
    public Double(double v) {
        value = v;
    }
    
    /**
     *  Returns the value of the Double as int
     */
    @Override
    final public int intValue() {
        return (int) value;
    }
    
    /**
     *  Returns the value of the Double as float
     *
     */
    @Override
    final public float floatValue() {
        return (float) value;
    }
    
    /**
     *  Returns the value of the Double as double
     *
     */
    @Override
    final public double doubleValue() {
        return value;
    }
    
    /**
     *  Returns the value of the Double as long
     */
    @Override
    final public long longValue() {
        return (long) value;
    }
    
    
    /** is this term a prolog integer term? */
    @Override
    final public boolean isInteger() {
        return false;
    }
    
    /** is this term a prolog real term? */
    @Override
    final public boolean isReal() {
        return true;
    }

    /**
     * Returns true if this Double term is grater that the term provided.
     * For number term argument, the int value is considered.
     */
    @Override
    public boolean isGreater(Term t) {
        t = t.term();
        if (t instanceof Number) {
            return value>((Number)t).doubleValue();
        } else if (t instanceof Struct) {
            return false;
        } else return t instanceof Var;
    }
    
    @Override
    public boolean isGreaterRelink(Term t, ArrayList<String> vorder) {
        t = t.term();
        if (t instanceof Number) {
            return value>((Number)t).doubleValue();
        } else if (t instanceof Struct) {
            return false;
        } else return t instanceof Var;
    }
    
    /**
     * Returns true if this Double term is equal to the term provided.
     */
    @Override
    public boolean isEqual(Term t) {
        t = t.term();
        if (t instanceof Number) {
            Number n = (Number) t;
            if (!n.isReal())
                return false;
            return value == n.doubleValue();
        } else {
            return false;
        }
    }
    
    /**
     * Tries to unify a term with the provided term argument.
     * This service is to be used in demonstration context.
     */
    @Override
    boolean unify(List<Var> vl1, List<Var> vl2, Term t) {
        t = t.term();
        if (t instanceof Var) {
            return t.unify(vl2, vl1, this);
        } else if (t instanceof Number && ((Number) t).isReal()) {
            return value == ((Number) t).doubleValue();
        } else {
            return false;
        }
    }
    
    public String toString() {
        return java.lang.Double.toString(value);
    }
    
//    public int resolveVariables(int count) {
//        return count;
//    }

    /**
     * @author Paolo Contessi
     */
    @Override
    public final int compareTo(Number o) {
        return java.lang.Double.compare(value, o.doubleValue()); //(new java.lang.Double(value)).compareTo(o.doubleValue());
    }
    
}