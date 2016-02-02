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
package nars.op.mental;

import nars.$;
import nars.NAR;
import nars.Symbols;
import nars.nal.nal8.operator.SyncOperator;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Term;
import nars.truth.DefaultTruth;
import org.jetbrains.annotations.NotNull;

/**
 * Feeling common operations
 */
public abstract class feel extends SyncOperator {

    /**
     * To get the current value of an internal sensor
     *
     * @param value The value to be checked, in [0, 1]
     * @return Immediate results as Tasks
     */
    protected static Task feeling(float value, @NotNull NAR nar, Term feeling) {

        Term content = $.instprop(nar.self, feeling);

        return
            new MutableTask(content, Symbols.JUDGMENT)
                .judgment()
                .truth(new DefaultTruth(value, 0.99f))
                .present(nar.time())
        ;
    }

}
