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

import com.github.pfmiles.dropincc.Action;
import com.github.pfmiles.dropincc.DropinccException;
import com.github.pfmiles.dropincc.ParamedAction;
import com.github.pfmiles.dropincc.impl.GruleType;

import java.text.MessageFormat;
import java.util.List;

/**
 * @author pf-miles
 * 
 */
public class AltsActionsGen extends CodeGen {

    // [actionCls, ruleName, altIndex]
    private static final String fmt = "public {0} {1}Action{2};// {1} rule alt {2} action";

    // list([grule, altIndex, actionObj])
    private final List<Object[]> actionInfos;

    public AltsActionsGen(List<Object[]> actionInfos) {
        this.actionInfos = actionInfos;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String render(CodeGenContext context) {
        StringBuilder sb = new StringBuilder();
        // [grule, altIndex, actionObj]
        for (Object[] actionInfo : actionInfos) {
            Object action = actionInfo[2];
            String actionCls = null;
            if (action instanceof Action) {
                actionCls = "Action";
            } else if (action instanceof ParamedAction) {
                actionCls = "ParamedAction";
            } else {
                throw new DropinccException("Invalid action object: " + action);
            }
            String ruleName = ((GruleType) actionInfo[0]).toCodeGenStr();
            String altIndex = String.valueOf(actionInfo[1]);
            String fname = ruleName + "Action" + altIndex;
            context.fieldAltsActionMapping.put(fname, action);
            context.actionFieldMapping.put(action, fname);
            sb.append(MessageFormat.format(fmt, actionCls, ruleName, altIndex)).append('\n');
        }
        return sb.toString();
    }
}
