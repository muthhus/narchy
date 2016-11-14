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
package com.github.pfmiles.dropincc.impl;

import com.github.pfmiles.dropincc.Predicate;
import com.github.pfmiles.dropincc.impl.util.Util;

import java.util.List;

/**
 * Compiled rule alternative, with matching element sequence and look-aheads,
 * action
 * 
 * @author pf-miles
 * 
 */
public class CAlternative {

    /** match sequence */
    public final List<EleType> seq;

    public final Object action;

    public final Predicate<?> predicate;

    public CAlternative(List<EleType> ms, Object action, Predicate<?> pred) {
        this.seq = ms;
        this.action = action;
        this.predicate = pred;
    }


//    // same hashCode method as Object.class needed
//    public int hashCode() {
//        return super.hashCode();
//    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CAlternative(").append(this.seq.toString());
        if (this.action != null)
            sb.append(", ").append(Util.resolveActionName(this.action));
        sb.append(')');
        return sb.toString();
    }

}
