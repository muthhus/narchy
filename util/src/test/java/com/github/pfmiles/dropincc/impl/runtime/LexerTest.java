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
package com.github.pfmiles.dropincc.impl.runtime;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.github.pfmiles.dropincc.CC;
import com.github.pfmiles.dropincc.Element;
import com.github.pfmiles.dropincc.Exe;
import com.github.pfmiles.dropincc.Grule;
import com.github.pfmiles.dropincc.Grammar;
import com.github.pfmiles.dropincc.TokenDef;
import com.github.pfmiles.dropincc.impl.AnalyzedLang;
import com.github.pfmiles.dropincc.impl.runtime.impl.Lexer;
import com.github.pfmiles.dropincc.testhelper.TestHelper;

/**
 * @author pf-miles
 * 
 */
public class LexerTest extends TestCase {

    public void testBasicLex1() {
        Grammar lang = new Grammar("Test");
        Element a = lang.the("a");
        Element b = lang.the("b");
        Element c = lang.the("c");
        Grule A = lang.rule();
        lang.when(A, CC.EOF);
        A.when(CC.ks(a), b).alt(CC.kc(a), c);
        Exe exe = lang.compile();
        // to test ignore whitespaces
        // System.out.println(exe.lexing("     abc   a\r\tbc\n   \r   "));
        // +1 for EOF
        assertTrue(exe.lexing("     abc   a\r\tbc\n   \r   ").size() == 7);
    }

    // public void testBasicInstantTokens()

    public void testJavaTokens() {
        Grammar lang = new Grammar("Test");
        // these keywords tokens must be defined before the identifier token in
        // the initial version of dropincc.java, because it could not do
        // 'longest' match, the later versions of dropincc.java should solve the
        // problem
        Element _public = lang.the("public");
        Element _void = lang.the("void");
        Element digit = lang.the("\\d+");
        Element _id = lang.the("[a-zA-Z_]\\w*");
        Element _new = lang.the("new");
        Element leftParen = lang.the("\\(");
        Element rightParen = lang.the("\\)");
        Element leftBrace = lang.the("\\{");
        Element rightBrace = lang.the("\\}");
        Element equal = lang.the("\\=\\=");
        Element assign = lang.the("\\=");
        Element dot = lang.the("\\.");
        Element semi = lang.the(";");
        Element comma = lang.the(",");
        Element str = lang.the("\"[^\"]*\"");

        lang.when(_public, _void, _new, digit, _id, leftParen, rightParen, leftBrace, rightBrace, equal, assign, dot, semi, comma, str, CC.EOF);

        Exe exe = lang.compile();
        List<Token> ts = exe.lexing("public void testBasicLex1() {" + "Lang lang = new Lang();" + "Element a = lang.newToken(\"a\");"
                + "Element b = lang.newToken(\"b\");" + "Element c = lang.newToken(\"c\");" + "Grule A = lang.newGrule();" + "lang.defineGrule(A, CC.EOF);"
                + "A.define(CC.ks(a), b).alt(CC.kc(a), c);" + "Exe exe = lang.compile();" + "assertTrue(exe.lexing(\"ab\\r\\t   c d\").size() == 7);" + "}");
        // for (Token t : ts)
        // System.out.println(t);
        // +1 for EOF
        assertTrue(ts.size() == 117);
    }

    public void testJavaRegexAnnoyingTokens() {
        Grammar lang = new Grammar("Test");
        Grule A = lang.rule();
        lang.when(A, CC.EOF);
        A.when("\\(", A, "\\)").alt("\\\\G");
        Exe exe = lang.compile();
        assertTrue(exe.lexing("(((\\G)))").size() == 8);
    }

    public void testWhitespaceSensitive() {
        Grammar lang = new Grammar("Test");
        Grule A = lang.rule();
        lang.when(A, CC.EOF);
        A.when(" \\(", A, "\\) ").alt(" \\\\G ");
        lang.setWhiteSpaceSensitive(true);
        Exe exe = lang.compile();
        // System.out.println(exe.lexing(" ( ( ( \\G ) ) ) "));
        assertTrue(exe.lexing(" ( ( ( \\G ) ) ) ").size() == 8);
    }

    public void testLaLt() {
        Grammar lang = new Grammar("Test");
        Grule A = lang.rule();
        lang.when(A, CC.EOF);
        A.when(" \\(", A, "\\) ").alt(" \\\\G ");
        lang.setWhiteSpaceSensitive(true);
        Exe exe = lang.compile();
        AnalyzedLang al = TestHelper.priField(exe, "al");
        Lexer l = al.newLexer(" ( ( ( \\G ) ) ) ");
        for (int i = 1; i <= 8; i++) {
            assertTrue(l.LA(i) != null);
        }
        assertTrue(l.LA(9) == null);
        List<Token> ts = new ArrayList<Token>();
        while (l.hasMoreElements())
            ts.add(l.nextElement());
        // System.out.println(ts);
        assertTrue(ts.size() == 8);
    }

    public void testLexerRuleContainsVerticalBar() {
        Grammar lang = new Grammar("Test");
        TokenDef a = lang.the("ab|bc");
        TokenDef b = lang.the("uv|wx");
        lang.when(CC.ks(a, b), CC.EOF);
        Exe exe = lang.compile();
        List<Token> ts = exe.lexing("bcabababbcbcabwxwxuvwx");
        // System.out.println(ts);
        assertTrue(ts.size() == 12);
    }
}
