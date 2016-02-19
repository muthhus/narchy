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
import nars.nal.nal8.Execution;
import nars.task.Task;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

/**
 * Feeling happy value
 */
public class feelHappy extends feel {

    public static final Term happiness = $.the(feelHappy.class.getSimpleName());

    /**
     * To get the current value of an internal sensor
     * @return Immediate results as Tasks
     */
    @Override
    public void execute(@NotNull Task e) {
        NAR n = nar;
        Execution.feedback(e,
                feeling(n.memory.emotion.happy(),
                        n,
                        happiness), n
        );
    }
}
