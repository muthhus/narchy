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
package com.github.pfmiles.dropincc.impl.syntactical;

import com.github.pfmiles.dropincc.*;
import com.github.pfmiles.dropincc.impl.Alternative;
import com.github.pfmiles.dropincc.impl.GruleType;
import com.github.pfmiles.dropincc.impl.llstar.PredictingGrule;
import com.github.pfmiles.dropincc.testhelper.AnalyzedLangForTest;
import com.github.pfmiles.dropincc.testhelper.TestHelper;
import junit.framework.TestCase;

import java.util.List;
import java.util.Map;

/**
 * @author pf-miles
 * 
 */
public class ParserCompilerTest extends TestCase {

    @SuppressWarnings("unchecked")
    public void testOrSubRuleRewrite() {
        Grammar calculator = new Grammar("Test");
        TokenDef DIGIT = calculator.the("\\d+");
        TokenDef ADD = calculator.the("\\+");
        TokenDef SUB = calculator.the("\\-");
        TokenDef MUL = calculator.the("\\*");
        TokenDef DIV = calculator.the("/");
        TokenDef LEFTPAREN = calculator.the("\\(");
        TokenDef RIGHTPAREN = calculator.the("\\)");
        Grule expr = calculator.rule();
        Grule term = calculator.rule();
        Element mulTail = calculator.when(MUL.or(DIV), term);
        term.when(DIGIT, mulTail).orWhen(LEFTPAREN, expr, RIGHTPAREN).orWhen(DIGIT);
        Element addendTail = calculator.when(ADD.or(SUB), term);
        expr.when(term, addendTail, CC.EOF);

        List<Grule> grules = (List<Grule>) TestHelper.priField(calculator, "grules");
        List<Grule> genGrules = ParserCompiler.rewriteSubRules(grules);
        assertTrue(genGrules.size() == 2);
        for (Grule gg : genGrules) {
            assertTrue(gg.getAlts().size() == 2);
            for (Alternative alt : gg.getAlts()) {
                assertTrue(alt.getElements().size() == 1);
                assertTrue(alt.getElements().get(0) instanceof TokenDef);
            }
        }
        Map<Grule, GruleType> gruleTypeMapping = ParserCompiler.buildGruleTypeMapping(grules, genGrules);
        assertTrue(gruleTypeMapping.size() == 6);
    }

    /**
     * Intended to test sub rule rewrite
     * 
     * <pre>
     * term ::= DIGIT ((MUL | DIV) term) 
     *        | LEFTPAREN expr RIGHTPAREN
     *        | DIGIT ;
     * expr ::= term ((ADD | SUB) term) EOF;
     * </pre>
     */
    @SuppressWarnings("unchecked")
    public void testSubRuleRewriteOrCascadingAnd() {
        Grammar calculator = new Grammar("Test");
        TokenDef DIGIT = calculator.the("\\d+");
        TokenDef ADD = calculator.the("\\+");
        TokenDef SUB = calculator.the("\\-");
        TokenDef MUL = calculator.the("\\*");
        TokenDef DIV = calculator.the("/");
        TokenDef LEFTPAREN = calculator.the("\\(");
        TokenDef RIGHTPAREN = calculator.the("\\)");

        Grule term = calculator.rule();
        Grule expr = calculator.rule();
        term.when(DIGIT, MUL.or(DIV).and(term)).orWhen(LEFTPAREN, expr, RIGHTPAREN).orWhen(DIGIT);
        expr.when(term, ADD.or(SUB).and(term), CC.EOF);

        List<Grule> grules = (List<Grule>) TestHelper.priField(calculator, "grules");
        List<Grule> genGrules = ParserCompiler.rewriteSubRules(grules);
        assertTrue(genGrules.size() == 4);
        Grule g1 = genGrules.get(0);
        assertTrue(g1.getAlts().size() == 1);
        assertTrue(g1.getAlts().get(0).getElements().size() == 2);
        assertTrue(g1.getAlts().get(0).getElements().get(0) instanceof Grule);
        assertTrue(g1.getAlts().get(0).getElements().get(1) instanceof Grule);

        Grule g2 = genGrules.get(1);
        assertTrue(g2.getAlts().size() == 2);
        assertTrue(g2.getAlts().get(0).getElements().size() == 1);
        assertTrue(g2.getAlts().get(0).getElements().get(0) instanceof TokenDef);
        assertTrue(g2.getAlts().get(1).getElements().size() == 1);
        assertTrue(g2.getAlts().get(1).getElements().get(0) instanceof TokenDef);

        Grule g3 = genGrules.get(2);
        assertTrue(g3.getAlts().size() == 1);
        assertTrue(g3.getAlts().get(0).getElements().size() == 2);
        assertTrue(g3.getAlts().get(0).getElements().get(0) instanceof Grule);
        assertTrue(g3.getAlts().get(0).getElements().get(1) instanceof Grule);

        Grule g4 = genGrules.get(3);
        assertTrue(g4.getAlts().size() == 2);
        assertTrue(g4.getAlts().get(0).getElements().size() == 1);
        assertTrue(g4.getAlts().get(0).getElements().get(0) instanceof TokenDef);
        assertTrue(g4.getAlts().get(1).getElements().size() == 1);
        assertTrue(g4.getAlts().get(1).getElements().get(0) instanceof TokenDef);

        Map<Grule, GruleType> gruleTypeMapping = ParserCompiler.buildGruleTypeMapping(grules, genGrules);
        assertTrue(gruleTypeMapping.size() == 6);
    }

    // TODO add a test which rewrites 'and' invocation cascading 'or'

    /**
     * direct left recursion:
     * 
     * <pre>
     * L ::= L '>' '0'
     * </pre>
     * 
     * chained left recursion:
     * 
     * <pre>
     * L ::= '(' A ')'
     *     | B ']'
     *     | '0'
     * A ::= '{' B '}'
     * B ::= L '&gt;'
     * </pre>
     * 
     * left recursion with kleene nodes:
     * 
     * <pre>
     * L ::= '(' A ')'
     *     | (B ']')*
     *     | '0'
     * A ::= '{' B '}'
     *     | (L)?
     * B ::= (A)+ '&gt;'
     * </pre>
     */
    public void testCheckAndReportLeftRecursions() {
        // direct left recursion
        Grammar testLang = new Grammar("Test");
        TokenDef gt = testLang.the("\\>");
        TokenDef zero = testLang.the("0");
        Grule L = testLang.rule();
        L.when(L, gt, zero);
        AnalyzedLangForTest a = TestHelper.resolveAnalyzedLangForTest(testLang);
        try {
            ParserCompiler.checkAndReportLeftRecursions(a.ruleTypeToAlts, a.kleeneTypeToNode);
            assertTrue(false);
        } catch (DropinccException e) {
            // System.out.println(e.getMessage());
            assertTrue(true);
        }

        // chained left recursion
        testLang = new Grammar("Test");
        TokenDef leftParen = testLang.the("\\(");
        TokenDef rightParen = testLang.the("\\)");
        TokenDef rightBracket = testLang.the("\\]");
        zero = testLang.the("0");
        TokenDef leftBrace = testLang.the("\\{");
        TokenDef rightBrace = testLang.the("\\}");
        gt = testLang.the("\\>");
        Grule A = testLang.rule();
        Grule B = testLang.rule();
        Element l = testLang.when(leftParen, A, rightParen).orWhen(B, rightBracket).orWhen(zero);
        A.when(leftBrace, B, rightBrace);
        B.when(l, gt);
        a = TestHelper.resolveAnalyzedLangForTest(testLang);
        try {
            ParserCompiler.checkAndReportLeftRecursions(a.ruleTypeToAlts, a.kleeneTypeToNode);
            assertTrue(false);
        } catch (DropinccException e) {
            // System.out.println(e.getMessage());
            assertTrue(true);
        }

        // left recursion in kleene nodes
        testLang = new Grammar("Test");
        leftParen = testLang.the("\\(");
        rightParen = testLang.the("\\)");
        rightBracket = testLang.the("\\]");
        zero = testLang.the("0");
        leftBrace = testLang.the("\\{");
        rightBrace = testLang.the("\\}");
        gt = testLang.the("\\>");
        A = testLang.rule();
        B = testLang.rule();
        l = testLang.when(leftParen, A, rightParen).orWhen(CC.ks(B, rightBracket)).orWhen(zero);
        A.when(leftBrace, B, rightBrace).orWhen(CC.op(l));
        B.when(CC.kc(A), gt);
        a = TestHelper.resolveAnalyzedLangForTest(testLang);
        try {
            ParserCompiler.checkAndReportLeftRecursions(a.ruleTypeToAlts, a.kleeneTypeToNode);
            assertTrue(false);
        } catch (DropinccException e) {
            // System.out.println(e.getMessage());
            assertTrue(true);
        }
    }

    /**
     * Test compute predicting grule, basic LL(1) grammar
     * 
     * <pre>
     *  S ::= A $
     *  A ::= a c*
     *      | b c*
     * </pre>
     */
    public void testComputePredictingGrules() {
        Grammar ll1 = new Grammar("Test");
        Element a = ll1.the("a");
        Element b = ll1.the("b");
        Element c = ll1.the("c");
        Grule A = ll1.rule();
        ll1.when(A, CC.EOF);
        A.when(a, CC.ks(c)).orWhen(b, CC.ks(c));
        AnalyzedLangForTest al = TestHelper.resolveAnalyzedLangForTest(ll1);
        List<PredictingGrule> ps = ParserCompiler.computePredictingGrules(al.ruleTypeToAlts, al.kleeneTypeToNode).getPgs();
        // System.out.println(ps);
        assertTrue(ps.size() == 2);
    }

    public void testComputePredictingGrulesWithInstantTokens() {
        Grammar ll3 = new Grammar("Test");
        Grule A = ll3.rule();

        ll3.when(A, CC.EOF);

        Grule B = ll3.rule();
        Grule C = ll3.rule();
        Grule D = ll3.rule();

        A.when(B, CC.ks("a")).orWhen(C, CC.kc("a")).orWhen(D, CC.op("a"));

        B.when("a", "b", "c", C).orWhen("a", "b", "c", D).orWhen("d");

        C.when("e", "f", "g", D).orWhen("e", "f", "g", "h");

        D.when("i", "j", "k", "l").orWhen("i", "j", "k", "m");

        AnalyzedLangForTest al = TestHelper.resolveAnalyzedLangForTest(ll3);
        List<PredictingGrule> ps = ParserCompiler.computePredictingGrules(al.ruleTypeToAlts, al.kleeneTypeToNode).getPgs();
        // System.out.println(ps);
        assertTrue(ps.size() == 5);
    }

    public void testParserCodeGen() {
        Grammar lang = new Grammar("Calculator");
        Grule L = lang.rule();
        TokenDef a = lang.the("\\+");
        lang.when(L, CC.EOF).then(new Action<Object>() {
            public Object apply(Object matched) {
                Object[] ms = (Object[]) matched;
                System.out.println("Total result, length(2 exp): " + ms.length);
                return ms;
            }
        });
        Grule A = lang.rule();
        L.when(A, CC.ks(a.or("\\-"), A)).then(new Action<Object>() {
            public Object apply(Object matched) {
                Object[] ms = (Object[]) matched;
                System.out.println("L result, length(2 exp): " + ms.length);
                return ms;
            }
        });
        TokenDef m = lang.the("\\*");
        Grule F = lang.rule();
        A.when(F, CC.ks(m.or("/"), F)).then(new Action<Object>() {
            public Object apply(Object matched) {
                Object[] ms = (Object[]) matched;
                System.out.println("A result, length(2 exp): " + ms.length);
                return ms;
            }
        });
        F.when("\\(", L, "\\)").then(new Action<Object>() {
            public Object apply(Object matched) {
                Object[] ms = (Object[]) matched;
                System.out.println("F result, length(3 exp): " + ms.length);
                return ms;
            }
        }).orWhen("[0-9]+").then(new ParamedAction<Object, Object>() {
            public Object apply(Object arg, Object matched) {
                String m = (String) matched;
                System.out.println("F result, single value: " + m + ", arg: " + arg);
                return m;
            }
        });

        Exe exe = lang.compile();
        assertTrue(exe.eval("1+2", "hello") != null);
    }

    /**
     * <pre>
     * S ::= A $
     * A ::= a b c
     *     | a b c
     * </pre>
     */
    public void testDebugAndWarningsMsgs() {
        Grammar lang = new Grammar("Test");
        Grule A = lang.rule();
        lang.when(A, CC.EOF);
        A.when("a", "b", "c").orWhen("a", "b", "c");
        lang.compile();
        // System.out.println(lang.getDebugMsgs());
        assertTrue(lang.getDebugMsgs() != null);
        // System.out.println(lang.getWarnings());
        assertTrue(lang.getWarnings() != null);
    }
}
