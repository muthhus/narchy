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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.github.pfmiles.dropincc.Action;
import com.github.pfmiles.dropincc.CC;
import com.github.pfmiles.dropincc.Exe;
import com.github.pfmiles.dropincc.Grule;
import com.github.pfmiles.dropincc.Lang;
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

    private static Exe exe;

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
        Lang lang = new Lang("BoolExpr");
        TokenDef OR = lang.newToken("\\|\\|");
        TokenDef AND = lang.newToken("&&");
        TokenDef TRUE = lang.newToken("true");
        TokenDef FALSE = lang.newToken("false");
        TokenDef OP = lang.newToken("\\!?[a-zA-Z\\>\\<\\=]+");
        TokenDef NOT = lang.newToken("\\!");
        TokenDef LEFT_PAREN = lang.newToken("\\(");
        TokenDef RIGHT_PAREN = lang.newToken("\\)");
        TokenDef STRING = lang.newToken("'[^']*'");
        TokenDef NUMBER = lang.newToken("\\-?[0-9]+(\\.[0-9]+)?");
        TokenDef DATE = lang.newToken("#[0-9][0-9][0-9][0-9]\\-[0-9][0-9]\\-[0-9][0-9]( [0-9][0-9]:[0-9][0-9]:[0-9][0-9])?#");
        TokenDef REF = lang.newToken("\\$[a-zA-Z][a-zA-Z0-9_]*");
        TokenDef LEFT_BRACKET = lang.newToken("\\[");
        TokenDef RIGHT_BRACKET = lang.newToken("\\]");
        TokenDef LEFT_BRACE = lang.newToken("\\{");
        TokenDef RIGHT_BRACE = lang.newToken("\\}");
        TokenDef COMMA = lang.newToken(",");

        Grule boolExpr = lang.newGrule();
        Grule orExpr = lang.newGrule();
        Grule andExpr = lang.newGrule();
        Grule value = lang.newGrule();

        lang.defineGrule(boolExpr, CC.EOF).action((Action<Object[]>) matched -> (Boolean) matched[0]);
        boolExpr.define(orExpr, CC.ks(OR, orExpr)).action((Action<Object[]>) matched -> Util.reduceOrExprs((Boolean) matched[0], (Object[]) matched[1]));
        orExpr.define(andExpr, CC.ks(AND, andExpr)).action((Action<Object[]>) matched -> Util.reduceAndExprs((Boolean) matched[0], (Object[]) matched[1]));
        andExpr.define(TRUE).action((Action<String>) matched -> Boolean.TRUE).alt(FALSE).action((Action<String>) matched -> Boolean.FALSE).alt(value, OP, value).action((ParamedAction<Map<String, Object>, Object[]>) (context, matched) -> {
            Object left = matched[0];
            Object right = matched[2];
            if (!opMapping.containsKey(matched[1]))
                throw new RuntimeException("Illegal operator: " + matched[1]);
            return opMapping.get(matched[1]).compute(left, right);
        }).alt(CC.ks(NOT), LEFT_PAREN, boolExpr, RIGHT_PAREN).action((Action<Object[]>) matched -> {
            int numOfNots = ((Object[]) matched[0]).length;
            Boolean ret = (Boolean) matched[2];
            if (numOfNots % 2 == 0) {
                return ret;
            } else {
                return !ret;
            }
        }).alt(value).action((Action<Object>) matched -> {
            if (!(matched instanceof Boolean))
                throw new RuntimeException("");
            return (Boolean) matched;
        });
        value.define(STRING).action((Action<String>) matched -> matched.substring(1, matched.length() - 1)).alt(NUMBER).action((Action<String>) Double::parseDouble).alt(DATE).action((Action<String>) matched -> {
            SimpleDateFormat fmt = null;
            fmt = matched.contains(" ") ? new SimpleDateFormat("#yyyy-MM-dd HH:mm:ss#") : new SimpleDateFormat("#yyyy-MM-dd#");
            try {
                return fmt.parse(matched);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }).alt(REF).action((ParamedAction<Map<String, Object>, String>) Map::get).alt(LEFT_PAREN.or(LEFT_BRACKET), value, COMMA, value, RIGHT_BRACKET.or(RIGHT_PAREN)).action((Action<Object[]>) matched -> Util.createInterval("(".equals(matched[0]), matched[1], matched[3], ")".equals(matched[4]))).alt(LEFT_BRACE, value, CC.ks(COMMA, value), RIGHT_BRACE).action((Action<Object[]>) matched -> Util.buildCollection(matched[1], (Object[]) matched[2]));
        exe = lang.compile();
    }

    public static Boolean exe(Map<String, Object> context, String code) {
        return exe.eval(code, context);
    }

    public static Boolean exe(String code) {
        return exe.eval(code, new HashMap<String, Object>());
    }

}
