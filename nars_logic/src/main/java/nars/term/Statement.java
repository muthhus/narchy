/*
 * Statement.java
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
package nars.term;

import nars.term.compound.Compound;



/**
 * A statement or relation is a compound term, consisting of a subject, a predicate, and a
 * relation symbol in between. It can be of either first-order or higher-order.
 */
@Deprecated
public interface Statement {

    /**
     * Check the validity of a potential Statement. [To be refined]
     * <p>
     *
     * @param subject   The first component
     * @param predicate The second component
     * @return Whether The Statement is invalid
     */
    static boolean invalidStatement(Term subject, Term predicate) {
        if (subject == null || predicate == null)
            return true;

        if (subject.equals(predicate))
            return true;

        return invalidStatement2(subject, predicate);
    }

    /** skips the null and equality test */
    static boolean invalidStatement2(Term subject, Term predicate) {
        //TODO combine these mirrored invalidReflexive calls into one combined, unredundant operation
        if (invalidReflexive(subject, predicate))
            return true;

        if (invalidReflexive(predicate, subject))
            return true;


        if ((Statement.is(subject)) && (Statement.is(predicate))) {
            Termed s1 = subject;
            Termed s2 = predicate;

            Term t11 = Statement.subj(s1);
            Term t22 = Statement.pred(s2);
            if (!t11.equals(t22))
                return false;

            Term t12 = Statement.pred(s1);
            Term t21 = Statement.subj(s2);
            if (t12.equals(t21))
                return true;
        }
        return false;
    }


    static boolean is(Termed t) {
        return t.op().isStatement();
    }

    static Term subj(Termed t) {
        return ((TermContainer)t.term()).term(0);
    }
    static Term pred(Termed t) {
        return ((TermContainer)t.term()).term(1);
    }

    /**
     * Check if one term is identical to or included in another one, except in a
     * reflexive relation
     * <p>
     *
     * @param t1 The first term
     * @param t2 The second term
     * @return Whether they cannot be related in a statement
     */
    static boolean invalidReflexive(Term t1, Term t2) {
        if (!(t1 instanceof Compound)) {
            return false;
        }
        if ((t1.op().isImage()/*Ext) || (t1 instanceof ImageInt*/)) {
            return false;
        }
        return t1.containsTerm(t2);
    }


//    public static boolean invalidPair(final Term s1, final Term s2) {
//        boolean s1Indep = s1.hasVarIndep();
//        boolean s2Indep = s2.hasVarIndep();
//        //return (s1Indep && !s2Indep || !s1Indep && s2Indep);
//        return s1Indep ^ s2Indep;
//    }

}
