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
package com.github.pfmiles.dropincc.impl.lexical;

import com.github.pfmiles.dropincc.Grammar;
import com.github.pfmiles.dropincc.TokenDef;
import com.github.pfmiles.dropincc.impl.EleType;
import com.github.pfmiles.dropincc.impl.TokenType;
import com.github.pfmiles.dropincc.impl.util.Pair;
import com.github.pfmiles.dropincc.testhelper.TestHelper;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author pf-miles
 * 
 */
@SuppressWarnings("unchecked")
public class LexerCompilerTest extends TestCase {
    public void testCheckAndCompileTokenRulesInvalidTokens() {
        Grammar dl = new Grammar("Test");
        List<TokenDef> tokens = new ArrayList<TokenDef>();
        // null token test
        tokens.add(dl.the(null));
        dl.when(dl.the("ok!"));
        Map<TokenDef, TokenType> tokenTypeMapping = LexerCompiler.buildTokenTypeMapping(tokens, false);
        try {
            LexerCompiler.checkAndCompileTokenRules(tokens, tokenTypeMapping);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue("Cannot create null token.".equals(e.getMessage()));
        }
        tokens.clear();
        // empty token test
        tokens.add(dl.the(""));
        try {
            LexerCompiler.checkAndCompileTokenRules(tokens, tokenTypeMapping);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue("Cannot create null token.".equals(e.getMessage()));
        }
        tokens.clear();
        // error pattern test
        tokens.add(dl.the("aaa"));
        tokens.add(dl.the("[[["));
        try {
            LexerCompiler.checkAndCompileTokenRules(tokens, tokenTypeMapping);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue("Invalid token rule: '[[['".equals(e.getMessage()));
        }
        tokens.clear();
    }

    public void testCombinedTokenRulesGroupNums() {
        Grammar dl = new Grammar("Test");
        List<TokenDef> tokens = new ArrayList<TokenDef>();
        tokens.add(dl.the("aaa"));
        tokens.add(dl.the("bb(c(d))"));
        tokens.add(dl.the("ee(f\\(g\\))"));
        tokens.add(dl.the("hh\\(i\\(j\\)k\\)l"));
        tokens.add(dl.the("zzz"));
        dl.when(dl.the("stubToken"));
        Map<TokenDef, TokenType> tokenTypeMapping = LexerCompiler.buildTokenTypeMapping(tokens, false);
        Pair<Map<Integer, TokenType>, Pattern> pair = LexerCompiler.checkAndCompileTokenRules(tokens, tokenTypeMapping);
        Map<Integer, TokenType> gnumToType = pair.getLeft();
        assertTrue(gnumToType.size() == 6);
        // Integer[] exps = new Integer[] { 1, 2, 5, 7, 8 , -2};
        Map<Integer, EleType> exps = new HashMap<Integer, EleType>();
        exps.put(1, new TokenType(0, "0"));
        exps.put(2, new TokenType(1, "1"));
        exps.put(5, new TokenType(2, "2"));
        exps.put(7, new TokenType(3, "3"));
        exps.put(8, new TokenType(4, "4"));
        exps.put(9, new TokenType(-2, "-2"));
        assertTrue(gnumToType.equals(exps));
    }

    public void testBuildTokenTypeMappingWhiteSpaceSensitive() {
        Grammar dl = new Grammar("Test");
        List<TokenDef> tokens = new ArrayList<TokenDef>();
        tokens.add(dl.the("aaa"));
        tokens.add(dl.the("bb(c(d))"));
        tokens.add(dl.the("ee(f\\(g\\))"));
        tokens.add(dl.the("hh\\(i\\(j\\)k\\)l"));
        tokens.add(dl.the("zzz"));
        dl.when(dl.the("stubToken"));
        Map<TokenDef, TokenType> tokenTypeMapping = LexerCompiler.buildTokenTypeMapping(tokens, true);
        Pair<Map<Integer, TokenType>, Pattern> pair = LexerCompiler.checkAndCompileTokenRules(tokens, tokenTypeMapping);
        Map<Integer, TokenType> gnumToType = pair.getLeft();
        assertTrue(gnumToType.size() == 5);
        // Integer[] exps = new Integer[] { 1, 2, 5, 7, 8};
        Map<Integer, EleType> exps = new HashMap<Integer, EleType>();
        exps.put(1, new TokenType(0, "0"));
        exps.put(2, new TokenType(1, "1"));
        exps.put(5, new TokenType(2, "2"));
        exps.put(7, new TokenType(3, "3"));
        exps.put(8, new TokenType(4, "4"));
        assertTrue(gnumToType.equals(exps));
    }

    public void testAddSameTokens() {
        Grammar l = new Grammar("Test");
        l.the("a");
        l.the("b");
        l.the("c");
        l.the("c");
        l.the("a");
        l.the("a");
        l.the("b");
        Map<TokenDef, TokenType> tokenTypeMapping = LexerCompiler.buildTokenTypeMapping((List<TokenDef>) TestHelper.priField(l, "tokens"), true);
        assertTrue(tokenTypeMapping.size() == 4);
    }
}
