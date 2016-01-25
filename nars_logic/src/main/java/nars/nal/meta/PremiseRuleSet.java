package nars.nal.meta;

import nars.$;
import nars.Global;
import nars.Narsese;
import nars.nal.Deriver;
import nars.term.Compound;
import nars.term.index.PatternIndex;
import nars.util.data.list.FasterList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
public class PremiseRuleSet {

    private static final Pattern twoSpacePattern = Pattern.compile("  ", Pattern.LITERAL);
    private static final Pattern equivOperatorPattern = Pattern.compile("<=>", Pattern.LITERAL);
    private static final Pattern implOperatorPattern = Pattern.compile("==>", Pattern.LITERAL);
    private static final Pattern conjOperatorPattern = Pattern.compile("&&", Pattern.LITERAL);
    private final List<PremiseRule> premiseRules = new FasterList<>();


    public PremiseRuleSet() throws IOException, URISyntaxException {
        this(Paths.get(Deriver.class.getResource("default.meta.nal").toURI()));
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
            premiseRules.add(p);
        }
    }

    public PremiseRuleSet(@NotNull Collection<String> ruleStrings) {
        int[] errors = {0};

        parse(load(ruleStrings), patterns).forEach(s -> premiseRules.add(s));


        logger.info("indexed " + premiseRules.size() + " total rules, consisting of " + patterns.size() + " unique pattern components terms");
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
            if (s.startsWith("try:")) {
                single_rule_test = true;
                break;
            }
        }

        for (String s : lines) {
            boolean currentRuleEmpty = current_rule.length() == 0;

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


    private static final String[] equFull = {"<=>"/*, "</>", "<|>"*/};
    private static final String[] implFull = {"==>"/*, "=/>" , "=|>", "=\\>"*/};
    private static final String[] conjFull = {"&&"/*, "&|", "&/"*/};
    @Nullable
    private static final String[] unchanged = {null};

    /**
     * //TODO do this on the parsed rule, because string contents could be unpredictable:
     * permute(rule, Map<Op,Op[]> alternates)
     *
     * @param rules
     * @param ruleString
     */
    static void permuteTenses(@NotNull Collection<String> rules /* results collection */,
                              @NotNull String ruleString) {

        //Original version which permutes in different tenses

        if (!ruleString.contains("Order:ForAllSame")) {
            rules.add(ruleString);
            return;
        }

        String[] equs =
                ruleString.contains("<=>") ?
                        equFull :
                        unchanged;


        String[] impls =
                ruleString.contains("==>") ?
                        implFull :
                        unchanged;

        String[] conjs =
                ruleString.contains("&&") ?
                        conjFull :
                        unchanged;


        rules.add(ruleString);


        for (String equ : equs) {

            String p1 = equ != null ? equivOperatorPattern.matcher(ruleString).replaceAll(Matcher.quoteReplacement(equ)) : ruleString;

            for (String imp : impls) {

                String p2 = imp != null ? implOperatorPattern.matcher(p1).replaceAll(Matcher.quoteReplacement(imp)) : p1;

                for (String conj : conjs) {

                    String p3 = conj != null ? conjOperatorPattern.matcher(p2).replaceAll(Matcher.quoteReplacement(conj)) : p2;

                    rules.add(p3);
                }
            }
        }


    }


    @NotNull
    static Set<PremiseRule> parse(@NotNull Collection<String> rawRules, @NotNull PatternIndex index) {


        Set<String> expanded = new HashSet(rawRules.size() * 4); //Global.newHashSet(1); //new ConcurrentSkipListSet<>();


        rawRules/*.parallelStream()*/.forEach(rule -> {

            String p = preprocess(rule);


            //there might be now be A_1..maxVarArgsToMatch in it, if this is the case we have to add up to maxVarArgsToMatch ur
            /*if (p.contains("A_1..n") || p.contains("A_1..A_i.substitute(_)..A_n")) {
                addUnrolledVarArgs(expanded, p, maxVarArgsToMatch);
            } else {*/
            permuteTenses(expanded, p);



        });//.forEachOrdered(s -> expanded.addAll(s));


        Set<PremiseRule> ur = Global.newHashSet(rawRules.size()*4);
        //ListMultimap<TaskRule, TaskRule> ur = MultimapBuilder.linkedHashKeys().arrayListValues().build();


        //accumulate these in a set to eliminate duplicates
        expanded.forEach(src -> {
            try {


                PremiseRule preNorm = new PremiseRule((Compound) Narsese.the().term(src, $.terms, false /* raw */));

                PremiseRule r = add(ur, preNorm, src, index);

                if (r.allowBackward) {
                    addQuestions(ur, r, src, index);

                    PremiseRule f = r.forwardPermutation();
                    //if (r.allowBackward)
                        addQuestions(ur, f, src, index);
                    add(ur, f, src, index);
                }


            } catch (Exception ex) {
                logger.error("Invalid TaskRule: {}", ex);
                ex.printStackTrace();
            }
        });

        return ur;
    }

    private static void addQuestions(@NotNull Collection<PremiseRule> target, @NotNull PremiseRule r, String src, @NotNull PatternIndex patterns) {

        r.forEachQuestionReversal((q,reason) -> add(target, q, src + "//" + reason, patterns));

     }

    @Nullable
    static PremiseRule add(@NotNull Collection<PremiseRule> target, @Nullable PremiseRule q, String src, @NotNull PatternIndex index) {
        if (q == null)
            throw new RuntimeException("null: " + q + ' ' + src);

        q = q.normalizeRule(index).setup(index);
        q.setSource(src);
        target.add(q);
        return q;
    }
    private static final Pattern spacePattern = Pattern.compile(" ", Pattern.LITERAL);

    @NotNull
    public List<PremiseRule> getPremiseRules() {
        return premiseRules;
        //return Collections.unmodifiableList(premiseRules);
    }


}

