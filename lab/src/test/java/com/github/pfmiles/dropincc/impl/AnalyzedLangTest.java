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

import com.github.pfmiles.dropincc.*;
import com.github.pfmiles.dropincc.impl.kleene.KleeneStarNode;
import com.github.pfmiles.dropincc.impl.kleene.KleeneStarType;
import com.github.pfmiles.dropincc.impl.kleene.KleeneType;
import com.github.pfmiles.dropincc.testhelper.TestHelper;
import junit.framework.TestCase;

import java.util.List;
import java.util.Map;

/**
 * @author pf-miles
 * 
 */
public class AnalyzedLangTest extends TestCase {

    /**
     * <pre>
     * expr ::= addition EOF;
     * addition ::= addend ((AND | SUB) addend)*;
     * addend ::= factor ((MUL | DIV) factor)*;
     * factor ::= DIGIT
     *          | LEFTPAREN addition RIGHTPAREN;
     * </pre>
     */
    @SuppressWarnings({ "unused", "unchecked" })
    public void testSubRuleRewriteWithKleeneNodes() {
        Grammar calculator = new Grammar("Test");
        TokenDef DIGIT = calculator.the("\\d+");
        TokenDef ADD = calculator.the("\\+");
        TokenDef SUB = calculator.the("\\-");
        TokenDef MUL = calculator.the("\\*");
        TokenDef DIV = calculator.the("/");
        TokenDef LEFTPAREN = calculator.the("\\(");
        TokenDef RIGHTPAREN = calculator.the("\\)");
        // 2.define grammar rules and corresponding actions
        Grule addition = calculator.rule();
        Grule addend = calculator.rule();
        Grule factor = calculator.rule();
        Element expr = calculator.when(addition, CC.EOF);
        addition.when(addend, CC.ks(ADD.or(SUB), addend));
        addend.when(factor, CC.ks(MUL.or(DIV), factor));
        factor.when(DIGIT).orWhen(LEFTPAREN, addition, RIGHTPAREN);
        AnalyzedLang cl = new AnalyzedLang("test", (List<TokenDef>) TestHelper.priField(calculator, "tokens"), (List<Grule>) TestHelper.priField(
                calculator, "grules"), false);
        KleeneStarNode k1 = (KleeneStarNode) addition.getAlts().get(0).getElements().get(1);
        Object shouldBeRewritten = k1.getElements().get(0);
        assertTrue(shouldBeRewritten instanceof Grule);
        Grule r = (Grule) shouldBeRewritten;
        assertTrue(r.getAlts().size() == 2);
        assertTrue(r.getAlts().get(0).getElements().get(0).equals(ADD));
        assertTrue(r.getAlts().get(1).getElements().get(0).equals(SUB));
        KleeneStarNode k2 = (KleeneStarNode) addend.getAlts().get(0).getElements().get(1);
        shouldBeRewritten = k2.getElements().get(0);
        assertTrue(shouldBeRewritten instanceof Grule);
        r = (Grule) shouldBeRewritten;
        assertTrue(r.getAlts().size() == 2);
        assertTrue(r.getAlts().get(0).getElements().get(0).equals(MUL));
        assertTrue(r.getAlts().get(1).getElements().get(0).equals(DIV));

        assertTrue(cl.getGruleTypeMapping().size() == 6);
    }

    @SuppressWarnings("unused")
    public void testResolveParserAst() {
        Grammar calculator = new Grammar("Test");
        TokenDef DIGIT = calculator.the("\\d+");
        TokenDef ADD = calculator.the("\\+");
        TokenDef SUB = calculator.the("\\-");
        TokenDef MUL = calculator.the("\\*");
        TokenDef DIV = calculator.the("/");
        TokenDef LEFTPAREN = calculator.the("\\(");
        TokenDef RIGHTPAREN = calculator.the("\\)");
        Grule addition = calculator.rule();
        Grule addend = calculator.rule();
        Grule factor = calculator.rule();
        Element expr = calculator.when(addition, CC.EOF).then(new Action<Object>() {
            public Object apply(Object params) {
                return ((Object[]) params)[0];
            }
        });
        addition.when(addend, CC.ks((ADD.or(SUB)), addend)).then(new Action<Object>() {
            public Object apply(Object matched) {
                Object[] params = ((Object[]) matched);
                double leftMost = (Double) params[0];
                Object[] opAndOther = (Object[]) params[1];
                for (int i = 0; i < opAndOther.length; i++) {
                    Object[] opAndOne = (Object[]) opAndOther[i];
                    if ("+".equals(opAndOne[0])) {
                        leftMost += (Double) opAndOne[1];
                    } else if ("-".equals(opAndOne[1])) {
                        leftMost -= (Double) opAndOne[1];
                    } else {
                        throw new RuntimeException("Invalid operator: " + opAndOne[0]);
                    }
                }
                return leftMost;
            }
        });
        addend.when(factor, CC.ks(MUL.or(DIV), factor)).then(new Action<Object>() {
            public Object apply(Object matched) {
                Object[] params = (Object[]) matched;
                double leftMost = (Double) params[0];
                Object[] opAndOthers = (Object[]) params[1];
                for (int i = 0; i < opAndOthers.length; i++) {
                    Object[] opAndOther = (Object[]) opAndOthers[i];
                    if ("*".equals(opAndOthers[0])) {
                        leftMost *= (Double) opAndOthers[1];
                    } else if ("/".equals(opAndOthers[0])) {
                        leftMost /= (Double) opAndOthers[1];
                    } else {
                        throw new RuntimeException("Invalid operator: " + opAndOther[0]);
                    }
                }
                return leftMost;
            }
        });
        factor.when(DIGIT).then(new Action<Object>() {
            public Object apply(Object param) {
                return Double.parseDouble((String) param);
            }
        }).orWhen(LEFTPAREN, addition, RIGHTPAREN).then(new Action<Object>() {
            public Object apply(Object matched) {
                return (Double) ((Object[]) matched)[1];
            }
        });
        Exe exe = calculator.compile();
        AnalyzedLang alang = TestHelper.priField(exe, "al");
        Map<GruleType, List<CAlternative>> ruleTypeToAlts = alang.getRuleTypeToAlts();
        assertTrue(ruleTypeToAlts.size() == 6);
        for (Map.Entry<GruleType, List<CAlternative>> entry : ruleTypeToAlts.entrySet()) {
            List<CAlternative> calts = entry.getValue();
            switch (entry.getKey().getDefIndex()) {
            case 0:
                // addition
                assertTrue(calts.size() == 1);
                assertTrue(calts.get(0).seq.size() == 2);
                assertTrue(calts.get(0).seq.get(0) instanceof GruleType);
                assertTrue(calts.get(0).seq.get(1) instanceof KleeneStarType);
                break;
            case 1:
                // addend
                assertTrue(calts.size() == 1);
                assertTrue(calts.get(0).seq.size() == 2);
                assertTrue(calts.get(0).seq.get(0) instanceof GruleType);
                assertTrue(calts.get(0).seq.get(1) instanceof KleeneStarType);
                break;
            case 2:
                // factor
                assertTrue(calts.size() == 2);
                assertTrue(calts.get(0).seq.size() == 1);
                assertTrue(calts.get(0).seq.get(0) instanceof TokenType);

                assertTrue(calts.get(1).seq.size() == 3);
                assertTrue(calts.get(1).seq.get(0) instanceof TokenType);
                assertTrue(calts.get(1).seq.get(1) instanceof GruleType);
                assertTrue(calts.get(1).seq.get(2) instanceof TokenType);
                break;
            case 3:
                // expr
                assertTrue(calts.size() == 1);
                assertTrue(calts.get(0).seq.size() == 2);
                assertTrue(calts.get(0).seq.get(0) instanceof GruleType);
                assertTrue(calts.get(0).seq.get(1) instanceof TokenType);
                break;
            case 4:
                // sub rule in addend
                assertTrue(calts.size() == 2);
                assertTrue(calts.get(0).seq.size() == 1);
                assertTrue(calts.get(0).seq.get(0) instanceof TokenType);
                assertTrue(calts.get(0).seq.get(0).getDefIndex() == 3);

                assertTrue(calts.get(1).seq.size() == 1);
                assertTrue(calts.get(1).seq.get(0) instanceof TokenType);
                assertTrue(calts.get(1).seq.get(0).getDefIndex() == 4);
                break;
            case 5:
                // sub rule in addition
                assertTrue(calts.size() == 2);
                assertTrue(calts.get(0).seq.size() == 1);
                assertTrue(calts.get(0).seq.get(0) instanceof TokenType);
                assertTrue(calts.get(0).seq.get(0).getDefIndex() == 1);

                assertTrue(calts.get(1).seq.size() == 1);
                assertTrue(calts.get(1).seq.get(0) instanceof TokenType);
                assertTrue(calts.get(1).seq.get(0).getDefIndex() == 2);
                break;
            default:
                assertTrue(false);// error num of grules
            }
        }
        Map<KleeneType, List<EleType>> kleeneTypeToNode = alang.getKleeneTypeToNode();
        assertTrue(kleeneTypeToNode.size() == 2);
        for (Map.Entry<KleeneType, List<EleType>> entry : kleeneTypeToNode.entrySet()) {
            List<EleType> matchSeq = entry.getValue();
            switch (entry.getKey().getDefIndex()) {
            case 0:
                assertTrue(matchSeq.size() == 2);
                assertTrue(matchSeq.get(0) instanceof GruleType);
                assertTrue(matchSeq.get(0).getDefIndex() == 4);// mul or div

                assertTrue(matchSeq.get(1) instanceof GruleType);
                assertTrue(matchSeq.get(1).getDefIndex() == 2);// factor
                break;
            case 1:
                assertTrue(matchSeq.size() == 2);
                assertTrue(matchSeq.get(0) instanceof GruleType);
                assertTrue(matchSeq.get(0).getDefIndex() == 5);// add or sub

                assertTrue(matchSeq.get(1) instanceof GruleType);
                assertTrue(matchSeq.get(1).getDefIndex() == 1);// addend
                break;
            default:
                assertTrue(false);// error num of entries
            }
        }
    }
}
