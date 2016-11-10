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
package com.github.pfmiles.dropincc.example.calculator;

import com.github.pfmiles.dropincc.Action;
import com.github.pfmiles.dropincc.CC;
import com.github.pfmiles.dropincc.Exe;
import com.github.pfmiles.dropincc.Grule;
import com.github.pfmiles.dropincc.Grammar;
import com.github.pfmiles.dropincc.TokenDef;

/**
 * A simple calculator implementation. It could do non-negative floating point
 * calculations.
 * 
 * @author pf-miles
 * 
 */
public class Calculator {

    // the runnable calculator instance
    private static Exe calc = null;

    static {
        /**
         * <pre>
         * calc ::= expr $
         * expr ::= addend (('+'|'-') addend)*
         * addend ::= factor (('*'|'/') factor)*
         * factor ::= '(' expr ')'
         *          | '\\d+(\\.\\d+)?'
         * </pre>
         */
        Grammar c = new Grammar("Calculator");
        Grule expr = c.rule();
        c.when(expr, CC.EOF).then((Action<Object>) matched -> (Double) ((Object[]) matched)[0]);
        TokenDef a = c.the("\\+");
        Grule addend = c.rule();
        expr.when(addend, CC.ks(a.or("\\-"), addend)).then((Action<Object>) matched -> {
            Object[] ms = (Object[]) matched;
            Double a0 = (Double) ms[0];
            Object[] aPairs = (Object[]) ms[1];
            for (Object p : aPairs) {
                String op = (String) ((Object[]) p)[0];
                Double a1 = (Double) ((Object[]) p)[1];
                if ("+".equals(op)) {
                    a0 += a1;
                } else {
                    a0 -= a1;
                }
            }
            return a0;
        });
        TokenDef m = c.the("\\*");
        Grule factor = c.rule();
        addend.when(factor, CC.ks(m.or("/"), factor)).then((Action<Object>) matched -> {
            Object[] ms = (Object[]) matched;
            Double f0 = (Double) ms[0];
            Object[] fPairs = (Object[]) ms[1];
            for (Object p : fPairs) {
                String op = (String) ((Object[]) p)[0];
                Double f = (Double) ((Object[]) p)[1];
                if ("*".equals(op)) {
                    f0 *= f;
                } else {
                    f0 /= f;
                }
            }
            return f0;
        });
        factor.when("\\(", expr, "\\)").then((Action<Object>) matched -> (Double) ((Object[]) matched)[1]).alt("\\d+(\\.\\d+)?").then((Action<Object>) matched -> Double.parseDouble((String) matched));
        calc = c.compile();
    }

    /**
     * Compute and return the result of the specified expression. It could do
     * non-negative floating point calculations.
     * 
     * @param expr
     * @return
     */
    public static Double compute(String expr) {
        return calc.eval(expr);
    }
}
