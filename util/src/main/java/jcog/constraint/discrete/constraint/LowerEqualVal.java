/*
 * Copyright 2016, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jcog.constraint.discrete.constraint;

import jcog.constraint.discrete.IntVar;
import jcog.constraint.discrete.propagation.Propagator;

public class LowerEqualVal extends Propagator {

    private final IntVar x;
    private final int k;

    public LowerEqualVal(IntVar x, int k, boolean strict) {
        this.x = x;
        this.k = strict ? k + 1 : k;
    }

    @Override
    public boolean setup() {
        return x.updateMax(k);
    }

    @Override
    public boolean propagate() {
        return true;
    }
}