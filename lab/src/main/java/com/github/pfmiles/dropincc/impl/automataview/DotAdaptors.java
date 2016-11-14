/*******************************************************************************
 * Copyright (c) 2012 pf_miles.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     pf_miles - initial API and implementation
 ******************************************************************************/
package com.github.pfmiles.dropincc.impl.automataview;

import com.github.pfmiles.dropincc.impl.llstar.AtnState;
import com.github.pfmiles.dropincc.impl.llstar.DfaState;
import com.github.pfmiles.dropincc.impl.util.Pair;

import java.util.*;

/**
 * Some adapting methods to convert ATN or DFA states to GeneratingStates which
 * is required by DotGenerator.
 * 
 * @author pf-miles
 * 
 */
public final class DotAdaptors {
    private DotAdaptors() {
    }

    /**
     * Adapt ATN states to GeneratingStates, for later dot generation use.
     * 
     * @param states
     * @return
     */
    public static Collection<GeneratingState> adaptAtnStates(Set<AtnState> states) {
        List<GeneratingState> ret = new ArrayList<>();
        for (AtnState state : states) {
            ret.add(new DotAtnState(state));
        }
        return ret;
    }

    private static final class DotAtnState implements GeneratingState {
        private final AtnState state;

        public DotAtnState(AtnState state) {
            this.state = state;
        }

        @Override
        public String getId() {
            return this.state.getName();
        }

        @Override
        public List<Pair<String, GeneratingState>> getTransitions() {
            List<Pair<String, GeneratingState>> ret = new ArrayList<>();
            for (Pair<Object, AtnState> p : this.state.getTransitionsAsPairs()) {
                ret.add(new Pair<>(p.getLeft().toString(), new DotAtnState(p.getRight())));
            }
            return ret;
        }

        @Override
        public boolean isFinal() {
            return this.state.isFinal();
        }

    }

    /**
     * Adapting look ahead dfa states to GeneratingStates
     * 
     * @param states
     * @return
     */
    public static Collection<GeneratingState> adaptLookAheadDfaStates(Set<DfaState> states) {
        List<GeneratingState> ret = new ArrayList<>();
        for (DfaState state : states) {
            ret.add(new DotDfaState(state));
        }
        return ret;
    }

    private static final class DotDfaState implements GeneratingState {
        private final DfaState state;

        public DotDfaState(DfaState state) {
            this.state = state;
        }

        @Override
        public String getId() {
            if (this.state.isFinal()) {
                return this.state.getName() + '_' + this.state.getAlt();
            } else {
                return this.state.getName();
            }
        }

        @Override
        public List<Pair<String, GeneratingState>> getTransitions() {
            List<Pair<String, GeneratingState>> ret = new ArrayList<>();
            for (Map.Entry<Object, DfaState> e : this.state.getTransitions().entrySet()) {
                ret.add(new Pair<>(e.getKey().toString(), new DotDfaState(e.getValue())));
            }
            return ret;
        }

        @Override
        public boolean isFinal() {
            return this.state.isFinal();
        }
    }
}
