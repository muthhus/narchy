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
package com.github.pfmiles.dropincc.impl.syntactical.codegen;

import java.util.HashMap;
import java.util.Map;

import com.github.pfmiles.dropincc.Predicate;
import com.github.pfmiles.dropincc.impl.GruleType;
import com.github.pfmiles.dropincc.impl.TokenType;
import com.github.pfmiles.dropincc.impl.kleene.KleeneType;
import com.github.pfmiles.dropincc.impl.llstar.PredictingKleene;
import com.github.pfmiles.dropincc.impl.runtime.impl.RunningDfaState;
import com.github.pfmiles.dropincc.impl.util.SeqGen;

/**
 * Context during parser code generation. Put anything needed here.
 * 
 * @author pf-miles
 * 
 */
public class CodeGenContext {

    public CodeGenContext(Map<KleeneType, PredictingKleene> kleeneTypeToPredicting) {
        this.kleeneTypeToPredicting = kleeneTypeToPredicting;
    }

    // TODO could be removed by make token types static
    /**
     * generated parser class field's name to tokenType mapping
     */
    public final Map<String, TokenType> fieldTokenTypeMapping = new HashMap<>();

    /**
     * generated parser class field's name to alts action mapping
     */
    public final Map<String, Object> fieldAltsActionMapping = new HashMap<>();

    /**
     * generated parser class field's name to semantic predicate mapping
     */
    public final Map<String, Predicate<?>> fieldPredsMapping = new HashMap<>();

    /**
     * generated parser class field's name to rule dfa mapping
     */
    public final Map<String, RunningDfaState> fieldRuleDfaMapping = new HashMap<>();

    /**
     * generated parser class field's name to kleene nodes' look ahead dfa
     * mapping
     */
    public final Map<String, RunningDfaState> fieldKleeneDfaMapping = new HashMap<>();

    /**
     * variable sequence generator for method local variables
     */
    public SeqGen varSeq;

    /**
     * current grule type which is generating code for
     */
    public GruleType curGrule;

    /**
     * alts' action obj to generated parser class field's name mapping
     */
    public final Map<Object, String> actionFieldMapping = new HashMap<>();

    /**
     * kleeneType to its PredictingKleene mapping
     */
    public final Map<KleeneType, PredictingKleene> kleeneTypeToPredicting;
}
