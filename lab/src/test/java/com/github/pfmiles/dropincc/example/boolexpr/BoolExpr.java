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
package com.github.pfmiles.dropincc.example.boolexpr;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import com.github.pfmiles.dropincc.Action;
import com.github.pfmiles.dropincc.CC;
import com.github.pfmiles.dropincc.Exe;
import com.github.pfmiles.dropincc.Grule;
import com.github.pfmiles.dropincc.Grammar;
import com.github.pfmiles.dropincc.ParamedAction;
import com.github.pfmiles.dropincc.TokenDef;
import com.github.pfmiles.dropincc.example.boolexpr.operator.Between;
import com.github.pfmiles.dropincc.example.boolexpr.operator.Equals;
import com.github.pfmiles.dropincc.example.boolexpr.operator.GreaterThan;
import com.github.pfmiles.dropincc.example.boolexpr.operator.Include;
import com.github.pfmiles.dropincc.example.boolexpr.operator.LessThan;
import com.github.pfmiles.dropincc.example.boolexpr.operator.NotBetween;
import com.github.pfmiles.dropincc.example.boolexpr.operator.NotEquals;
import com.github.pfmiles.dropincc.example.boolexpr.operator.NotInclude;

/**
 * A simple bool expression DSL example.
 * 
 * @author pf-miles Aug 20, 2012 10:10:54 AM
 */
public class BoolExpr {

    private static final Exe exe;

    private static final Map<String, Operator> opMapping = new HashMap<>();
    static {
        opMapping.put(">", new GreaterThan());
        opMapping.put("<", new LessThan());
        opMapping.put("=", new Equals());
        opMapping.put("!=", new NotEquals());
        opMapping.put("between", new Between());
        opMapping.put("!between", new NotBetween());
        opMapping.put("include", new Include());
        opMapping.put("!include", new NotInclude());
    }

    /**
     * <pre>
     * 
     * OR ::= '||'
     * AND ::= '&&'
     * NOT ::= '!'
     * TRUE ::= 'true'
     * FALSE ::= 'false'
     * OP ::= '\!?[a-zA-Z\>\<\=]+'
     * LEFT_PAREN ::= '('
     * RIGHT_PAREN ::= ')'
     * STRING ::= '\'[^\']*\''
     * NUMBER ::= '\-?[0-9]+(\.[0-9]+)?'
     * DATE ::= '#[0-9][0-9][0-9][0-9]\-[0-9][0-9]\-[0-9][0-9]( [0-9][0-9]:[0-9][0-9]:[0-9][0-9])?#'
     * REF ::= '\$[a-zA-Z][a-zA-Z0-9_]*'
     * LEFT_BRACE ::= '{'
     * RIGHT_BRACE ::= '}'
     * COMMA ::= ','
     * 
     * S ::= boolExpr $
     * boolExpr ::= orExpr (OR orExpr)*
     * orExpr ::= andExpr (AND andExpr)*
     * andExpr ::= TRUE
     *           | FALSE
     *           | value OP value
     *           | NOT* LEFT_PAREN boolExpr RIGHT_PAREN
     *           | value
     * value ::= STRING
     *         | NUMBER
     *         | DATE
     *         | REF
     *         | LEFT_PAREN|LEFT_BRACKET value COMMA value RIGHT_BRACKET|RIGHT_PAREN
     *         | LEFT_BRACE value (COMMA value)* RIGHT_BRACE
     * 
     * </pre>
     */
    static {
        Grammar g = new Grammar("BoolExpr");

        TokenDef OR = g.the("\\|\\|");
        TokenDef AND = g.the("&&");
        TokenDef TRUE = g.the("true");
        TokenDef FALSE = g.the("false");
        TokenDef OP = g.the("\\!?[a-zA-Z\\>\\<\\=]+");
        TokenDef NOT = g.the("\\!");
        TokenDef LEFT_PAREN = g.the("\\(");
        TokenDef RIGHT_PAREN = g.the("\\)");
        TokenDef STRING = g.the("'[^']*'");
        TokenDef NUMBER = g.the("\\-?[0-9]+(\\.[0-9]+)?");
        TokenDef DATE = g.the("#[0-9][0-9][0-9][0-9]\\-[0-9][0-9]\\-[0-9][0-9]( [0-9][0-9]:[0-9][0-9]:[0-9][0-9])?#");
        TokenDef REF = g.the("\\$[a-zA-Z][a-zA-Z0-9_]*");
        TokenDef LEFT_BRACKET = g.the("\\[");
        TokenDef RIGHT_BRACKET = g.the("\\]");
        TokenDef LEFT_BRACE = g.the("\\{");
        TokenDef RIGHT_BRACE = g.the("\\}");
        TokenDef COMMA = g.the(",");

        Grule orExpr = g.rule();
        Grule andExpr = g.rule();
        Grule value = g.rule();

        Grule boolExpr = g.rule();
        boolExpr.when(orExpr, CC.ks(OR, orExpr)).then((Action<Object[]>) matched -> Util.reduceOrExprs((Boolean) matched[0], (Object[]) matched[1]));

        orExpr.when(andExpr, CC.ks(AND, andExpr)).then((Action<Object[]>) matched -> Util.reduceAndExprs((Boolean) matched[0], (Object[]) matched[1]));

        andExpr.when(TRUE).then((Action<String>) matched -> Boolean.TRUE).orWhen(FALSE).then((Action<String>) matched -> Boolean.FALSE).orWhen(value, OP, value).then((Object context, Object[] matched) -> {
            Object left = matched[0];
            Object right = matched[2];
            if (!opMapping.containsKey(matched[1]))
                throw new RuntimeException("Illegal operator: " + matched[1]);
            return opMapping.get(matched[1]).compute(left, right);
        }).orWhen(CC.ks(NOT), LEFT_PAREN, boolExpr, RIGHT_PAREN).then((Action<Object[]>) matched -> {
            int notCount = ((Object[]) matched[0]).length;
            return (notCount % 2 == 0) == (Boolean) matched[2];
        }).orWhen(value).then((Action<Object>) matched -> {
            if (!(matched instanceof Boolean))
                throw new RuntimeException("");
            return (Boolean) matched;
        });
        value
            .when(STRING).then((Action<String>) matched -> matched.substring(1, matched.length() - 1))
            .orWhen(NUMBER).then((Action<String>) Double::parseDouble)
            .orWhen(DATE).then((Action<String>) matched -> {
                SimpleDateFormat fmt = new SimpleDateFormat(matched.contains(" ") ? "#yyyy-MM-dd HH:mm:ss#" : "#yyyy-MM-dd#");
                try {
                    return fmt.parse(matched);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            })
            .orWhen(REF).then((ParamedAction<Map<String, Object>, String>) Map::get)
            .orWhen(LEFT_PAREN.or(LEFT_BRACKET), value, COMMA, value, RIGHT_BRACKET.or(RIGHT_PAREN)).then((Action<Object[]>) matched -> Util.createInterval("(".equals(matched[0]), matched[1], matched[3], ")".equals(matched[4]))).orWhen(LEFT_BRACE, value, CC.ks(COMMA, value), RIGHT_BRACE)
                .then((Action<Object[]>) matched -> Util.buildCollection(matched[1], (Object[]) matched[2]));

        exe = g.compile(boolExpr);
    }

    public static Boolean exe(Map<String, Object> context, String code) {
        return exe.eval(code, context);
    }

    public static Boolean exe(String code) {
        return exe.eval(code, new HashMap<String, Object>());
    }

}
