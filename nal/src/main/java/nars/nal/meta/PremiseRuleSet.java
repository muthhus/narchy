package nars.nal.meta;

import nars.*;
import nars.nal.Deriver;
import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.index.PatternIndex;
import nars.term.transform.CompoundTransform;
import nars.term.variable.Variable;
import nars.util.data.Util;
import nars.util.data.list.FasterList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Holds an array of derivation rules
 */
public class PremiseRuleSet  {

    final static Term rule = $.the("rule");

    public void reifyTo(@NotNull NAR n) {
        rules.forEach(r-> reifyTo(n,r));
    }



    //TODO abstract
    protected static void reifyTo(@NotNull NAR n, @NotNull PremiseRule r) {

        PatternVarReifier pr = new PatternVarReifier(r.hashCode()); //HACK todo use not hashcode this unsafe way

        //a rule is fundamentally an implication associating precondition to postcondition
        n.input(
            new MutableTask(
                $.inh( $.p(
                    ruleComponent((Compound) r.term(0), pr),
                    ruleComponent((Compound) r.term(1), pr),
                    $.the(pr.id)
                ), rule),
            Symbols.BELIEF) //.truth(1f,1f)
        );
    }

    @Nullable
    private static Term ruleComponent(@NotNull Compound term, @NotNull PatternVarReifier r) {
        return Terms.terms.transform(term, r);
    }


    private static final Pattern twoSpacePattern = Pattern.compile("  ", Pattern.LITERAL);
    private static final Pattern equivOperatorPattern = Pattern.compile("<=>", Pattern.LITERAL);
    private static final Pattern implOperatorPattern = Pattern.compile("==>", Pattern.LITERAL);
    private static final Pattern conjOperatorPattern = Pattern.compile("&&", Pattern.LITERAL);
    public final List<PremiseRule> rules = new FasterList<>();


    public PremiseRuleSet() throws IOException, URISyntaxException {
        this(Paths.get(Deriver.class.getClassLoader().getResource("default.meta.nal").toURI()));
        //this(Deriver.class.getClassLoader().getResourceAsStream("default.meta.nal"));
    }

    public PremiseRuleSet(InputStream is) throws IOException {
        this(Util.inputToStrings(is));
    }

    public PremiseRuleSet(@NotNull Path path) throws IOException {
        this(Files.readAllLines(path));
    }
    public final PatternIndex patterns = new PatternIndex();


    private static final Logger logger = LoggerFactory.getLogger(PremiseRuleSet.class);


    public PremiseRuleSet(boolean normalize, @NotNull PremiseRule... rules) {
        for (PremiseRule p : rules) {
            if (normalize)
                p = p.normalizeRule(patterns);
            this.rules.add(p);
        }
    }

    public PremiseRuleSet(@NotNull Collection<String> ruleStrings) {
        int[] errors = {0};

        parse(load(ruleStrings), patterns).forEach(rules::add);


        logger.info("indexed " + rules.size() + " total rules, consisting of " + patterns.size() + " unique pattern components terms");
        if (errors[0] > 0) {
            logger.warn("\trule errors: " + errors[0]);
        }
    }


    @NotNull
    static List<String> load(@NotNull Iterable<String> lines) {

        List<String> unparsed_rules = Global.newArrayList(1024);

        StringBuilder current_rule = new StringBuilder();
        boolean single_rule_test = false;

        for (String s : lines) {
            s = s.trim(); //HACK write a better file loader
            if (s.startsWith("try:")) {
                single_rule_test = true;
                break;
            }
        }

        for (String s : lines) {
            boolean currentRuleEmpty = current_rule.length() == 0;

            s = s.trim(); //HACK write a better file loader

            if (s.startsWith("//") || spacePattern.matcher(s).replaceAll(Matcher.quoteReplacement("")).isEmpty()) {

                if (!currentRuleEmpty) {

                    if (!single_rule_test || single_rule_test && current_rule.toString().contains("try:")) {
                        unparsed_rules.add(current_rule.toString().trim().replace("try:", "")); //rule is finished, add it
                    }
                    current_rule.setLength(0); //start identifying a new rule
                }

            } else {
                //note, it can also be that the current_rule is not empty and this line contains |- which means
                //its already a new rule, in which case the old rule has to be added before we go on
                if (!currentRuleEmpty && s.contains("|-")) {

                    if (!single_rule_test || single_rule_test && current_rule.toString().contains("try:")) {
                        unparsed_rules.add(current_rule.toString().trim().replace("try:", "")); //rule is finished, add it
                    }
                    current_rule.setLength(0); //start identifying a new rule

                }
                current_rule.append(s).append('\n');
            }
        }

        if (current_rule.length() > 0) {
            if (!single_rule_test || single_rule_test && current_rule.toString().contains("try:")) {
                unparsed_rules.add(current_rule.toString());
            }
        }

        return unparsed_rules;
    }

    @Deprecated /* soon */ static String preprocess(String rule) //minor things like Truth.Comparison -> Truth_Comparison
    {

        String ret = '<' + rule + '>';

        while (ret.contains("  ")) {
            ret = twoSpacePattern.matcher(ret).replaceAll(Matcher.quoteReplacement(" "));
        }

        ret = ret.replace("A..", "%A.."); //add var pattern manually to ellipsis
        ret = ret.replace("%A..B=_", "%A..%B=_"); //add var pattern manually to ellipsis
        ret = ret.replace("B..", "%B.."); //add var pattern manually to ellipsis
        ret = ret.replace("%A.._=B", "%A.._=%B"); //add var pattern manually to ellipsis

        return ret.replace("\n", "");/*.replace("A_1..n","\"A_1..n\"")*/ //TODO: implement A_1...n notation, needs dynamic term construction before matching
    }


//    private static final String[] equFull = {"<=>"/*, "</>", "<|>"*/};
//    private static final String[] implFull = {"==>"/*, "=/>" , "=|>", "=\\>"*/};
//    private static final String[] conjFull = {"&&"/*, "&|", "&/"*/};
//    @Nullable
//    private static final String[] unchanged = {null};

//    /**
//     * //TODO do this on the parsed rule, because string contents could be unpredictable:
//     * permute(rule, Map<Op,Op[]> alternates)
//     *
//     * @param rules
//     * @param ruleString
//     */
//    static void permuteTenses(@NotNull Collection<String> rules /* results collection */,
//                              @NotNull String ruleString) {
//
//        //Original version which permutes in different tenses
//
//        if (!ruleString.contains("Order:ForAllSame")) {
//            rules.add(ruleString);
//            return;
//        }
//
//        String[] equs =
//                ruleString.contains("<=>") ?
//                        equFull :
//                        unchanged;
//
//
//        String[] impls =
//                ruleString.contains("==>") ?
//                        implFull :
//                        unchanged;
//
//        String[] conjs =
//                ruleString.contains("&&") ?
//                        conjFull :
//                        unchanged;
//
//
//        rules.add(ruleString);
//
//
//        for (String equ : equs) {
//
//            String p1 = equ != null ? equivOperatorPattern.matcher(ruleString).replaceAll(Matcher.quoteReplacement(equ)) : ruleString;
//
//            for (String imp : impls) {
//
//                String p2 = imp != null ? implOperatorPattern.matcher(p1).replaceAll(Matcher.quoteReplacement(imp)) : p1;
//
//                for (String conj : conjs) {
//
//                    String p3 = conj != null ? conjOperatorPattern.matcher(p2).replaceAll(Matcher.quoteReplacement(conj)) : p2;
//
//                    rules.add(p3);
//                }
//            }
//        }
//
//
//    }


    @NotNull
    static Set<PremiseRule> parse(@NotNull Collection<String> rawRules, @NotNull PatternIndex index) {


        Set<String> expanded = new HashSet(rawRules.size() * 4); //Global.newHashSet(1); //new ConcurrentSkipListSet<>();


        rawRules/*.parallelStream()*/.forEach(rule -> {

            String p = preprocess(rule);
            expanded.add(p);


            //there might be now be A_1..maxVarArgsToMatch in it, if this is the case we have to add up to maxVarArgsToMatch ur
            /*if (p.contains("A_1..n") || p.contains("A_1..A_i.substitute(_)..A_n")) {
                addUnrolledVarArgs(expanded, p, maxVarArgsToMatch);
            } else {*/
            //permuteTenses(expanded, p);



        });//.forEachOrdered(s -> expanded.addAll(s));


        Set<PremiseRule> ur = Global.newHashSet(rawRules.size()*4);
        //ListMultimap<TaskRule, TaskRule> ur = MultimapBuilder.linkedHashKeys().arrayListValues().build();


        //accumulate these in a set to eliminate duplicates
        expanded.forEach(src -> {
            try {


                Termed prt = Narsese.the().term(src, Terms.terms, false /* raw */);
                if (!(prt instanceof Compound))
                    throw new Narsese.NarseseException("rule parse error: " + src + " -> " + prt);

                PremiseRule preNorm = new PremiseRule((Compound) prt);

                permute(index, ur, src, preNorm);


            } catch (Exception ex) {
                logger.error("Invalid TaskRule: {}", ex);
            }
        });

        return ur;
    }

    @NotNull
    public static Set<PremiseRule> permute(PremiseRule preNorm) {
        return permute(new PatternIndex(), Global.newHashSet(1), "", preNorm);
    }

    @NotNull
    public static Set<PremiseRule> permute(@NotNull PatternIndex index, @NotNull Set<PremiseRule> ur, String src, PremiseRule preNorm) {
        PremiseRule r = add(ur, preNorm, src, index);


        if (forwardPermutes(r)) {
            permuteForward(index, ur, src, r, r.allowBackward);
        }

        if (r.allowBackward) {

            //System.err.println("r: " + r);

            r.backwardPermutation((q, reason) -> {

                //System.err.println("  q: " + q + " " + reason);
                PremiseRule b = add(ur, q, src + ":" + reason, index);

                if (forwardPermutes(b)) {
                    permuteForward(index, ur, src, b, r.allowBackward);
                }
            });
        }
        return ur;
    }

    public static void permuteForward(@NotNull PatternIndex index, @NotNull Set<PremiseRule> ur, String src, @NotNull PremiseRule b, boolean thenBackward) {
        PremiseRule f = add(ur, b.forwardPermutation(), src + ":forward", index);
        if (thenBackward) {
            f.backwardPermutation((s, reasonBF) -> {
                add(ur, s, src + ':' + reasonBF, index);
            });
        }
    }

    /** whether a rule will be forward permuted */
    static boolean forwardPermutes(@NotNull PremiseRule r) {
        boolean[] fwd = new boolean[] { true };
        r.recurseTerms((s,c) -> {

            if (!fwd[0])
                return; //already disqualified

            String x = s.toString();
            if ((x.contains("task(")) ||
                (x.contains("time(")) ||
                (x.contains("after(")) ||
                (x.contains("Punctuation")) ||
                (x.contains("Structural")) ||
                (x.contains("Identity")) ||
                //(x.contains("substitute")) || //TESTING THIS
                (x.contains("Negation"))) {

                fwd[0] = false;
            }
        });
        return fwd[0];
    }

//    private static void addQuestions(@NotNull Collection<PremiseRule> target, @NotNull PremiseRule r, String src, @NotNull PatternIndex patterns) {
//
//        r.forEachQuestionReversal((q,reason) ->
//                add(target, q, src + "//" + reason, patterns));
//
//     }

    @Nullable
    static PremiseRule add(@NotNull Collection<PremiseRule> target, @Nullable PremiseRule q, String src, @NotNull PatternIndex index) {
//        if (q == null)
//            throw new RuntimeException("null: " + q + ' ' + src);

        PremiseRule normalized = q.normalizeRule(index).setup(index);
        normalized.setSource(src);
        target.add(normalized);
        return normalized;
    }

    private static final Pattern spacePattern = Pattern.compile(" ", Pattern.LITERAL);


    private static class PatternVarReifier implements CompoundTransform<Compound,Term> {

        final int id;

        public PatternVarReifier(int ruleID) {
            this.id = ruleID;
        }

        @Override
        public boolean test(@NotNull Term superterm) {
            return (superterm.varPattern()>0);
            //return (o instanceof Compound) && ((Compound)o).varPattern() > 0;
        }

        @Nullable
        @Override
        public Termed apply(Compound parent, @NotNull Term subterm, int depth) {
            return unpatternify(subterm);
        }

        @Nullable
        public Term unpatternify(@NotNull Term subterm) {
            String ruleID = Integer.toString(id, 36);
            if (subterm.op() == Op.VAR_PATTERN) {
                return $.quote("%" + ((Variable) subterm).id() + "_" + ruleID);
            } else if (subterm instanceof Compound) {
                 return ruleComponent((Compound) subterm, this);
            }
            return subterm;
        }
    }
}

