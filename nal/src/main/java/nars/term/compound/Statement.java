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
package nars.term.compound;

import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * A statement or relation is a compound term, consisting of a subject, a predicate, and a
 * relation symbol in between. It can be of either first-order or higher-order.
 */
public interface Statement {

//    /**
//     * Check the validity of a potential Statement. [To be refined]
//     * <p>
//     *
//     * @param subject   The first component
//     * @param predicate The second component
//     * @return Whether The Statement is invalid
//     */
//    static boolean invalidStatement(@NotNull Term subject, @NotNull Term predicate) {
//        return subject.equals(predicate) || invalidStatement2(subject, predicate);
//    }
//    static boolean coNegated(@NotNull Term subject, @NotNull Term predicate) {
//        return subject.op() == Op.NEG && ((Compound) subject).term(0).equals(predicate);
//    }

    /** @return
     *       -1 invalid for statement
     *       0 equivalent
     *      +1 valid for statement
     *
     */
    static int validStatement(@NotNull Term subject, @NotNull Term predicate) {


        Compound sc = subject instanceof Compound ? (Compound)subject : null;
        Compound pc = predicate instanceof Compound ? (Compound)predicate : null;

        if (sc!=null && pc!=null) {
            if (Terms.equalsAnonymous(sc, pc))
                return 0;
        } else {
            if (subject.equals(predicate))
                return 0;
        }

        //TODO its possible to disqualify invalid statement if there is no structural overlap here

        if (sc!=null &&
                sc.containsTermRecursively(predicate)
                //sc.containsTerm(predicate)
            ) {
            return -1;
        }

        if (pc!=null) {
            if (
                    pc.containsTermRecursively(subject)
                    //pc.containsTerm(subject)
               )
                return -1;

            if (sc!=null && subject.op().statement && predicate.op().statement) {
                return (!sc.term(0).equals(pc.term(1)) && !sc.term(1).equals(pc.term(0))) ? +1 : -1;
            }
        }

        return +1;
    }



    @Nullable
    static Term subj(@NotNull Termed t) {
        return ((TermContainer)t.term()).term(0);
    }
    @Nullable
    static Term pred(@NotNull Termed t) {
        return ((TermContainer)t.term()).term(1);
    }


//    public static boolean invalidPair(final Term s1, final Term s2) {
//        boolean s1Indep = s1.hasVarIndep();
//        boolean s2Indep = s2.hasVarIndep();
//        //return (s1Indep && !s2Indep || !s1Indep && s2Indep);
//        return s1Indep ^ s2Indep;
//    }

}
