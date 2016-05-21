package nars.nal.meta;

import nars.$;
import nars.Global;
import nars.Narsese;
import nars.nal.Deriver;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.index.PatternIndex;
import nars.util.data.Util;
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
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;


/**
 * Holds an set of derivation rules and a pattern index of their components
 */
public class PremiseRuleSet  {

    final static Term rule = $.the("rule");



    private static final Pattern twoSpacePattern = Pattern.compile("  ", Pattern.LITERAL);
    //private static final Pattern equivOperatorPattern = Pattern.compile("<=>", Pattern.LITERAL);
    //private static final Pattern implOperatorPattern = Pattern.compile("==>", Pattern.LITERAL);
    //private static final Pattern conjOperatorPattern = Pattern.compile("&&", Pattern.LITERAL);
    public final List<PremiseRule> rules;


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
        this.rules = Global.newArrayList(rules.length);
        for (PremiseRule p : rules) {
            this.rules.add(normalize ? p.normalizeRule(patterns) : p);
        }
    }

    final int[] errors = {0};


    public PremiseRuleSet(@NotNull List<String> ruleStrings) {

        this.rules = parse(load(ruleStrings), patterns).distinct().collect(toList());


        logger.info("indexed " + rules.size() + " total rules, consisting of " + patterns.size() + " unique pattern components terms");
        if (errors[0] > 0) {
            logger.warn("\trule errors: " + errors[0]);
        }
    }


    @NotNull
    static Stream<CharSequence> load(@NotNull List<String> lines) {

        List<CharSequence> unparsed_rules = Global.newArrayList(1024);

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

        return unparsed_rules
                //.parallelStream();
                .stream();
    }

    @Deprecated /* soon */ static String preprocess(@NotNull CharSequence rule) //minor things like Truth.Comparison -> Truth_Comparison
    {
        String ret = '<' + rule.toString() + '>';

        //while (ret.contains("  ")) {
            //ret = twoSpacePattern.matcher(ret).replaceAll(Matcher.quoteReplacement(" "));
        //}

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
    static Stream<PremiseRule> parse(@NotNull Stream<CharSequence> rawRules, @NotNull PatternIndex index) {
        return rawRules/*.parallelStream()*/
                .distinct()
                .map(PremiseRuleSet::preprocess)
                .map(src-> {

            List<PremiseRule> ur = Global.newArrayList(4);
            try {


                Termed prt = Narsese.the().term(src, Terms.terms, false /* raw */);
                if (!(prt instanceof Compound))
                    throw new Narsese.NarseseException("rule parse error: " + src + " -> " + prt);

                PremiseRule preNorm = new PremiseRule((Compound) prt);

                permute(index, ur, src, preNorm);

            } catch (Exception ex) {
                logger.error("Invalid TaskRule: {} {}", src, ex.getMessage());
            }

            return ur;
        }).flatMap(Collection::stream);
    }

    @NotNull
    public static Set<PremiseRule> permute(PremiseRule preNorm) {
        Set<PremiseRule> ur;
        permute(new PatternIndex(), ur = Global.newHashSet(1), "", preNorm);
        return ur;
    }

    @NotNull
    public static void permute(@NotNull PatternIndex index, @NotNull Collection<PremiseRule> ur, String src, PremiseRule preNorm) {
        PremiseRule r = add(ur, preNorm, src, index);


        if (forwardPermutes(r)) {
            permuteForward(index, ur, src, r, r.backward);
        }

        if (r.backward) {

            //System.err.println("r: " + r);

            r.backwardPermutation((q, reason) -> {

                //System.err.println("  q: " + q + " " + reason);
                PremiseRule b = add(ur, q, src + ':' + reason, index);

                if (forwardPermutes(b)) {
                    permuteForward(index, ur, src, b, r.backward);
                }
            });
        }
    }

    public static void permuteForward(@NotNull PatternIndex index, @NotNull Collection<PremiseRule> ur, String src, @NotNull PremiseRule b, boolean thenBackward) {

        @NotNull PremiseRule bSwap = b.forwardPermutation(index);
        if (bSwap!=null) {
            add(ur, bSwap, src + ":forward", index);
        }
        //PremiseRule f = b;

        if (thenBackward) {
            b.backwardPermutation((s, reasonBF) -> {
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
                (x.contains("Punctuation"))  ||
                //(x.contains("Structural")) ||
                (x.contains("Identity")) ||
                //(x.contains("substitute")) || //TESTING THIS
                (x.contains("Negation"))

            ) {
                fwd[0] = false;
            }
        });
        return fwd[0];
    }


    @NotNull
    static PremiseRule add(@NotNull Collection<PremiseRule> target, @Nullable PremiseRule q, String src, @NotNull PatternIndex index) {
//        if (q == null)
//            throw new RuntimeException("null: " + q + ' ' + src);


        PremiseRule normalized = normalize(q, index);
        normalized.setSource(src);
        target.add(normalized);

        return normalized;
    }

    @NotNull
    static PremiseRule normalize(@Nullable PremiseRule q, @NotNull PatternIndex index) {
        return q.normalizeRule(index).setup(index);
    }

    private static final Pattern spacePattern = Pattern.compile(" ", Pattern.LITERAL);


}

