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
import com.github.pfmiles.dropincc.impl.hotcompile.CompilationResult;
import com.github.pfmiles.dropincc.impl.hotcompile.HotCompileUtil;
import com.github.pfmiles.dropincc.impl.kleene.AbstractKleeneNode;
import com.github.pfmiles.dropincc.impl.kleene.KleeneCompiler;
import com.github.pfmiles.dropincc.impl.kleene.KleeneType;
import com.github.pfmiles.dropincc.impl.lexical.LexerCompiler;
import com.github.pfmiles.dropincc.impl.llstar.GenedKleeneGruleType;
import com.github.pfmiles.dropincc.impl.llstar.PredictingGrule;
import com.github.pfmiles.dropincc.impl.llstar.PredictingKleene;
import com.github.pfmiles.dropincc.impl.runtime.impl.*;
import com.github.pfmiles.dropincc.impl.syntactical.GenedGruleType;
import com.github.pfmiles.dropincc.impl.syntactical.ParserCompiler;
import com.github.pfmiles.dropincc.impl.syntactical.PredictingResult;
import com.github.pfmiles.dropincc.impl.syntactical.codegen.ParserCodeGenResult;
import com.github.pfmiles.dropincc.impl.util.Pair;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 
 * A analyzed language structure
 * 
 * @author pf-miles
 * 
 */
public class AnalyzedLang {

    private final String langName;
    private final List<TokenDef> tokens;
    private final Map<TokenDef, TokenType> tokenTypeMapping;
    // grule -> gruleType mapping, inited when AnalyzedLang obj creating,
    // completed after sub-rule rewriting
    private final Map<Grule, GruleType> gruleTypeMapping;
    private static final Map<Element, SpecialType> specialTypeMapping = new HashMap<>();
    static {
        // special type 1, 'nothing' represents a empty alternative.
        specialTypeMapping.put(CC.NOTHING, SpecialType.NOTHING);
    }
    // token group num -> token type
    private Map<Integer, TokenType> groupNumToType;
    // the token mathcing pattern
    private Pattern tokenPatterns;

    private final boolean whitespaceSensitive;

    // Grammar rule type -> alternatives with predicts, analysis & generated
    // from 'gruleTypeMapping' after sub-rule rewriting
    private Map<GruleType, List<CAlternative>> ruleTypeToAlts;

    // kleeneNode -> kleeneNodeType mapping, inited after building
    // 'gruleTypeMapping' immidiatelly
    private final Map<AbstractKleeneNode, KleeneType> kleeneTypeMapping;

    // kleene node Type -> match sequence mapping, built while
    // 'AnalyzedLang' compiling(resolveParserAst). For later analysis & code gen
    private Map<KleeneType, List<EleType>> kleeneTypeToNode;

    // the compiled lexer prototype
    private LexerPrototype lexerPrototype;
    // the compiled parser prototype
    private ParserPrototype parserPrototype;

    private String debugMsgs;
    private String warnings;

    public AnalyzedLang(String name, List<TokenDef> tokens, List<Grule> grules, boolean whitespaceSensitive) {
        this.langName = name;
        // build token -> tokenType mapping
        this.tokens = tokens;
        // Gathering instant tokenDefs...
        this.tokens.addAll(LexerCompiler.collectInstantTokenDefs(grules));

        this.whitespaceSensitive = whitespaceSensitive;

        this.tokenTypeMapping = LexerCompiler.buildTokenTypeMapping(tokens, whitespaceSensitive);

        // rewrite sub-rules
        List<Grule> genGrules = ParserCompiler.rewriteSubRules(grules);
        // build grule -> gruleType mapping for all grules(including generated
        // ones) in 'gruleTypeMapping'
        this.gruleTypeMapping = ParserCompiler.buildGruleTypeMapping(grules, genGrules);

        // traverse and register kleene nodes
        this.kleeneTypeMapping = KleeneCompiler.buildKleeneTypeMapping(this.gruleTypeMapping);
    }

    public void compile() {
        // 1.check & compile token rules
        Pair<Map<Integer, TokenType>, Pattern> compiledTokenUnit = LexerCompiler.checkAndCompileTokenRules(this.tokens, this.tokenTypeMapping);
        this.groupNumToType = compiledTokenUnit.getLeft();
        this.tokenPatterns = compiledTokenUnit.getRight();

        // 2.resolving the parser ast
        TypeMappingParam typeMappingParam = new TypeMappingParam(this.tokenTypeMapping, this.gruleTypeMapping, specialTypeMapping,
                this.kleeneTypeMapping);
        // at this point, 'gruleTypeMapping' contains all grule -> type
        // mappings, including generated grules
        this.ruleTypeToAlts = ParserCompiler.buildRuleTypeToAlts(typeMappingParam);

        // at this time, 'kleeneTypeMapping' should contain all KleeneNode ->
        // KleeneType mapping (built when traverse and register kleene nodes)
        this.kleeneTypeToNode = KleeneCompiler.buildKleeneTypeToNode(typeMappingParam);

        // resolve start grule
        GruleType startRuleType = resolveStartGruleType(ruleTypeToAlts, kleeneTypeToNode);

        // 3.check or simplify & compute grammar rules
        // detect and report left-recursion, LL parsing needed
        ParserCompiler.checkAndReportLeftRecursions(this.ruleTypeToAlts, this.kleeneTypeToNode);

        // 4.compute predicts, LL(*), detect and report rule conflicts
        PredictingResult predResults = ParserCompiler.computePredictingGrules(this.ruleTypeToAlts, this.kleeneTypeToNode);
        List<PredictingGrule> predGrules = predResults.getPgs();
        List<PredictingKleene> predKleenes = predResults.getPks();
        this.debugMsgs = predResults.getDebugMsgs();
        this.warnings = predResults.getWarnings();

        // 5.lexer code gen(TODO using pre-written template code currently,
        // should support stream tokenizing in the future)
        this.lexerPrototype = new PreWrittenStringLexerPrototype(this.groupNumToType, this.tokenPatterns, this.whitespaceSensitive);

        // 6.parser code gen
        ParserCodeGenResult parserCodeGenResult = ParserCompiler.genParserCode(this.langName, startRuleType, predGrules, predKleenes,
                tokenTypeMapping.values(), this.kleeneTypeToNode);
        String parserCode = parserCodeGenResult.getCode();

        // 7.compile and maintain the code in a separate classloader
        CompilationResult result = HotCompileUtil.compile("com.github.pfmiles.dropincc.impl.runtime.gen." + this.langName, parserCode);
        if (!result.isSucceed()) {
            throw new DropinccException("Parser code compilation failed. Reason: " + result.getErrMsg());
        }
        this.parserPrototype = new StatelessParserPrototype(result.getCls(), parserCodeGenResult);
    }

    // a grammar rule is the start rule if no other rules invokes it
    private static GruleType resolveStartGruleType(Map<GruleType, List<CAlternative>> ruleTypeToAlts, Map<KleeneType, List<EleType>> kleeneTypeToNode) {
        Set<GruleType> allGs = new HashSet<>();
        for (GruleType t : ruleTypeToAlts.keySet()) {
            if (!(t instanceof GenedGruleType) && !(t instanceof GenedKleeneGruleType))
                allGs.add(t);
        }
        Set<GruleType> enteredGrule = new HashSet<>();
        for (Map.Entry<GruleType, List<CAlternative>> e : ruleTypeToAlts.entrySet()) {
            GruleType g = e.getKey();
            List<CAlternative> alts = e.getValue();
            if (!enteredGrule.contains(g)) {
                enteredGrule.add(g);
                for (CAlternative alt : alts) {
                    filterOutInvokedGrule(alt.seq, allGs, ruleTypeToAlts, kleeneTypeToNode, enteredGrule);
                }
            }
        }
        if (allGs.isEmpty())
            throw new DropinccException("No start rule found, please check your grammar rules.");
        if (allGs.size() > 1)
            throw new DropinccException(
                    "More than one suspected start rule found in the grammar, dangling rules may exist, please check your grammar rules. These rules are not invoked by others: "
                            + allGs);
        return allGs.iterator().next();
    }

    private static void filterOutInvokedGrule(List<EleType> matchSequence, Set<GruleType> allGs, Map<GruleType, List<CAlternative>> ruleTypeToAlts,
                                              Map<KleeneType, List<EleType>> kleeneTypeToNode, Set<GruleType> enteredGrule) {
        for (EleType ele : matchSequence) {
            if (ele instanceof TokenType) {
                continue;
            } else if (ele instanceof GruleType) {
                allGs.remove(ele);
                if (!enteredGrule.contains(ele)) {
                    enteredGrule.add((GruleType) ele);
                    for (CAlternative alt : ruleTypeToAlts.get(ele)) {
                        filterOutInvokedGrule(alt.seq, allGs, ruleTypeToAlts, kleeneTypeToNode, enteredGrule);
                    }
                }
            } else if (ele instanceof KleeneType) {
                filterOutInvokedGrule(kleeneTypeToNode.get(ele), allGs, ruleTypeToAlts, kleeneTypeToNode, enteredGrule);
            } else {
                throw new DropinccException("Unhandled element type when finding start rule: " + ele);
            }
        }
    }

    /**
     * Create a new instance of the constructing language's lexer.
     * 
     * @return
     */
    public Lexer newLexer(String code) {
        return this.lexerPrototype.create(code);
    }

    /**
     * Create a new parser instance
     * 
     * @param lexer
     * @return
     */
    public Parser newParser(Lexer lexer) {
        return this.parserPrototype.create(lexer);
    }

    public Map<TokenDef, TokenType> getTokenTypeMapping() {
        return tokenTypeMapping;
    }

    public Map<Grule, GruleType> getGruleTypeMapping() {
        return gruleTypeMapping;
    }

    public Map<KleeneType, List<EleType>> getKleeneTypeToNode() {
        return kleeneTypeToNode;
    }

    public Map<Integer, TokenType> getGroupNumToType() {
        return groupNumToType;
    }

    public Map<GruleType, List<CAlternative>> getRuleTypeToAlts() {
        return ruleTypeToAlts;
    }

    public Pattern getTokenPatterns() {
        return tokenPatterns;
    }

    public String getDebugMsgs() {
        return debugMsgs;
    }

    public String getWarnings() {
        return warnings;
    }

}
