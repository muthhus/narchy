package nars.nal.rule;

import nars.Global;
import nars.index.PatternIndex;
import nars.nal.Deriver;
import nars.term.Compound;
import nars.term.Termed;
import nars.util.Util;
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
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;


/**
 * Holds an set of derivation rules and a pattern index of their components
 */
public class PremiseRuleSet  {

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
        String ret = rule.toString();

        if (ret.contains("..")) {
            ret = ret.replace("A..", "%A.."); //add var pattern manually to ellipsis
            ret = ret.replace("%A..B=_", "%A..%B=_"); //add var pattern manually to ellipsis
            ret = ret.replace("B..", "%B.."); //add var pattern manually to ellipsis
            ret = ret.replace("%A.._=B", "%A.._=%B"); //add var pattern manually to ellipsis
        }

        return '<' + ret + '>'; //ret.replace("\n", "");/*.replace("A_1..n","\"A_1..n\"")*/ //TODO: implement A_1...n notation, needs dynamic term construction before matching
    }



    @NotNull
    static Stream<PremiseRule> parse(@NotNull Stream<CharSequence> rawRules, @NotNull PatternIndex index) {
        return rawRules
                .map(PremiseRuleSet::preprocess)
                .distinct()
                .parallel()
                //.sequential()
                .map(src-> {

            Set<PremiseRule> ur = Global.newHashSet(512);
            try {

                Termed prt = index.parseRaw(src);

                PremiseRule preNorm = new PremiseRule((Compound) prt);

                permute(preNorm, src, index, ur);

            } catch (Exception ex) {
                logger.error("Invalid TaskRule: {} {}", src, ex.getMessage());
                ex.printStackTrace();
            }

            return ur;
        }).flatMap(Collection::stream);
    }

    @NotNull
    public static Set<PremiseRule> permute(PremiseRule preNorm) {
        Set<PremiseRule> ur;
        permute(preNorm, "", new PatternIndex(), ur = Global.newHashSet(1));
        return ur;
    }

    public static void permute(PremiseRule preNormRule, String src, @NotNull PatternIndex index, @NotNull Collection<PremiseRule> ur) {

        posNegPermute(preNormRule, src, (PremiseRule r) -> {

            permuteSwap(r, src, index, ur, (PremiseRule s) -> {

                if (Global.BACKWARD_QUESTION_RULES && r.allowBackward) {

                    r.backwardPermutation(index, (q, reason) -> {

                        PremiseRule b = add(q, src + ':' + reason, ur, index);

                        //                    //2nd-order backward
                        //                    if (forwardPermutes(b)) {
                        //                        permuteSwap(b, src, index, ur);
                        //                    }
                    });
                }

            });

        }, ur, index);
    }

    protected static void posNegPermute(PremiseRule preNorm, String src, Consumer<PremiseRule> each, @NotNull Collection<PremiseRule> ur, @NotNull PatternIndex index) {
        PremiseRule pos = add(preNorm.positive(index), src, ur, index);
        if (pos!=null)
            each.accept(pos);

        if (Global.NEGATIVE_RULES) {
            PremiseRule neg = add(preNorm.negative(index), src + ":Negated", ur, index);
            if (neg != null)
                each.accept(neg);
        }
    }



    public static void permuteSwap(@NotNull PremiseRule r, String src, @NotNull PatternIndex index, @NotNull Collection<PremiseRule> ur, Consumer<PremiseRule> then) {

        then.accept( r );

        if (Global.SWAP_RULES && permuteSwap(r)) {
            PremiseRule bSwap = r.swapPermutation(index);
            if (bSwap != null)
                then.accept(add(bSwap, src + ":forward", ur, index));
        }

    }

    /** whether a rule will be forward permuted */
    static boolean permuteSwap(@NotNull PremiseRule r) {
        boolean[] fwd = new boolean[] { true };
        r.recurseTerms((s) -> {

            if (!fwd[0])
                return; //already disqualified


            String x = s.toString();

            if ((x.contains("task(")) ||
                (x.contains("belief(")) ||
                (x.contains("time(")) ||
                //(x.contains("Punctuation"))  ||
                //(x.contains("Structural")) ||
                //(x.contains("Identity")) ||
                (x.contains("substitute"))  //TESTING THIS
                //(x.contains("Negation"))

            ) {
                fwd[0] = false;
            }
        });
        return fwd[0];
    }


    static PremiseRule add(@Nullable PremiseRule q, String src, @NotNull Collection<PremiseRule> target, @NotNull PatternIndex index) {
        if (q == null)
            return null;
//            throw new RuntimeException("null: " + q + ' ' + src);


        PremiseRule normalized = normalize(q, index);
        normalized.setSource(src);
        return target.add(normalized) ? normalized : null;
    }

    @NotNull
    static PremiseRule normalize(@Nullable PremiseRule q, @NotNull PatternIndex index) {
        return q.normalizeRule(index).setup(index);
    }

    private static final Pattern spacePattern = Pattern.compile(" ", Pattern.LITERAL);


}

