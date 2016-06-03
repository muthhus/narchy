/*
 * Copyright (C) 2014 peiwang
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.op.math;

import nars.$;
import nars.index.TermIndex;
import nars.nal.op.BinaryTermOperator;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.container.TermContainer;
import nars.util.Texts;
import org.jetbrains.annotations.NotNull;

/**
 * Count the number of elements in a set
 */
public class add extends IntIntTo<Integer> {

    @NotNull
    @Override protected Integer apply(int a, int b) {
        return a+b;
    }
}
//public class add extends BinaryTermOperator {
//
//    @Override public @NotNull Term apply(@NotNull Term a, Term b, @NotNull TermIndex i) {
//        if (!(a instanceof Atom) || !(b instanceof Atom))
//            return null;
//
//
//        try {
//            int ia = Texts.i(a.toString());
//            int ib = Texts.i(b.toString());
//            return $.the(ia + ib);
//        }
//        catch (Exception e) {
//            return null;
//        }
//
//
//    }
//
//}
