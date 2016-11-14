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
package com.github.pfmiles.dropincc.impl.llstar;

import com.github.pfmiles.dropincc.DropinccException;
import com.github.pfmiles.dropincc.Predicate;
import com.github.pfmiles.dropincc.impl.GruleType;
import com.github.pfmiles.dropincc.impl.TokenType;
import com.github.pfmiles.dropincc.impl.util.Pair;

import java.util.*;

/**
 * State of ATN
 * 
 * @author pf-miles
 * 
 */
public class AtnState {
    private final String name;
    // transitions to other states
    private final Map<Object, Set<AtnState>> transitions = new HashMap<>();
    // if this is a final state
    private final boolean _final;

    /**
     * Return all transitions as (edge, state) pairs
     * 
     * @return
     */
    public List<Pair<Object, AtnState>> getTransitionsAsPairs() {
        List<Pair<Object, AtnState>> ret = new ArrayList<>();
        for (Map.Entry<Object, Set<AtnState>> e : this.transitions.entrySet()) {
            Object edge = e.getKey();
            for (AtnState s : e.getValue()) {
                ret.add(new Pair<>(edge, s));
            }
        }
        return ret;
    }

    /**
     * Create a new AtnState
     * 
     * @param name
     *            name of this state
     * @param _final
     *            if this state final
     */
    public AtnState(String name, boolean _final) {
        super();
        this.name = 'p' + name;// name starts with 'p' by convention
        this._final = _final;
    }

    public AtnState(String name) {
        this(name, false);
    }

    public void addTransition(Object edge, AtnState otherState) {
        if (!edge.equals(Constants.epsilon) && !(edge instanceof TokenType) && !(edge instanceof GruleType) && !(edge instanceof Predicate))
            throw new DropinccException("Illegal ATN transition edge: " + edge);
        if (this.transitions.containsKey(edge)) {
            this.transitions.get(edge).add(otherState);
        } else {
            Set<AtnState> set = new HashSet<>();
            set.add(otherState);
            this.transitions.put(edge, set);
        }
    }

    public Set<AtnState> transit(Object edge) {
        if (this.transitions.containsKey(edge)) {
            return this.transitions.get(edge);
        } else {
            return Collections.emptySet();
        }
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AtnState other = (AtnState) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    public String toString() {
        return "AtnState(" + this.name + ')';
    }

    public String getName() {
        return name;
    }


    public Map<Object, Set<AtnState>> getTransitions() {
        return transitions;
    }

    public boolean isFinal() {
        return _final;
    }


    /**
     * Return num of transitions
     * 
     * @return
     */
    public int getTransitionCount() {
        int ret = 0;
        for (Set<AtnState> dests : this.transitions.values()) {
            ret += dests.size();
        }
        return ret;
    }

}
