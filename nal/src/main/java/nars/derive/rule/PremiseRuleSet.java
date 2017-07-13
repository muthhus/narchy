package nars.derive.rule;

import com.google.common.collect.Lists;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.index.term.PatternTermIndex;
import nars.term.Compound;
import nars.term.Term;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
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
public class PremiseRuleSet {

    public final List<PremiseRule> rules;
    private final boolean permuteBackwards, permuteForwards;


//    static final BiConsumer<Pair<Compound, String>, DataOutput> encoder = (x, o) ->{
//        try {
//            IO.writeTerm(o, x.getOne());
//            o.writeUTF(x.getTwo());
//        } catch (IOException e) {
//            throw new RuntimeException(e); //e.printStackTrace();
//        }
//    };
//
//    @NotNull
//    public static PremiseRuleSet rulesCached(String name) throws IOException, URISyntaxException {
//
//        PatternTermIndex p = new PatternTermIndex(1024);
//
//        Function<DataInput,Pair<Compound,String>> decoder = (i) -> {
//            try {
//                return Tuples.pair(
//                        (Compound)readTerm(i, p),
//                        i.readUTF()
//                );
//            } catch (IOException e) {
//                throw new RuntimeException(e); //e.printStackTrace();
//                //return null;
//            }
//        };
//
//        //Path path = Paths.get(Deriver.class.getClassLoader().getResource(name).toURI());
//
//        URL path = Deriver.class.getClassLoader().getResource(name);
//
//        Stream<Pair<Compound, String>> parsed =
//                Util.fileCache(path, PremiseRuleSet.class.getSimpleName(),
//                        () -> load(p, path),
//                        encoder,
//                        decoder,
//                        logger
//                );
//
//        return new PremiseRuleSet(parsed, p);
//    }
//static Stream<Pair<Compound, String>> load(@NotNull PatternTermIndex p, @NotNull URL path) {
//    try {
//
//        return parse(load(readAllLines(Paths.get(path.toURI()))), p);
//    } catch (Exception e) {
//        throw new RuntimeException(e);
//    }
//}
//public PremiseRuleSet(@NotNull List<String> ruleStrings) {
//    this(ruleStrings, new PatternTermIndex());
//}
//
//    public PremiseRuleSet(@NotNull List<String> ruleStrings, @NotNull PatternTermIndex patterns) {
//        this(parse(load(ruleStrings), patterns), patterns);
//    }


    @NotNull
    public static PremiseRuleSet rules(boolean permute, String... name) {
    final PatternTermIndex p = new PatternTermIndex();

        PremiseRuleSet rs = new PremiseRuleSet(
                Stream.of(name).flatMap(n -> {

                            InputStream nn = NAR.class.getResourceAsStream("nal/" + n);
                            byte[] bb;
                            try {
                                bb = nn.readAllBytes();
                            } catch (IOException e) {
                                e.printStackTrace();
                                bb = new byte[0];
                            }
                            return parse(load(bb), p);

                        }
                )
                , p, permute
        );

        logger.info("{} totalRules={}, uniqueComponents={}", name, rs.rules.size(), rs.patterns.size());
        if (rs.errors[0] > 0) {
            logger.warn("\t{} errors={}", name, rs.errors[0]);
        }

        return rs;
    }


    @NotNull
    public final PatternTermIndex patterns;


    private static final Logger logger = LoggerFactory.getLogger(PremiseRuleSet.class);


    public PremiseRuleSet(@NotNull PremiseRule... rules) {
        this(false, rules);
    }

    public PremiseRuleSet(boolean permute, @NotNull PremiseRule... rules) {
        this.patterns = new PatternTermIndex();
        this.rules = $.newArrayList(rules.length);
        for (PremiseRule p : rules) {
            try {
                this.rules.add(normalize(p, patterns));
            } catch (RuntimeException e) {
                logger.error(" {}", e);
            }
        }
        this.permuteBackwards = this.permuteForwards = permute;
    }

    final int[] errors = {0};


    public PremiseRuleSet(@NotNull Stream<Pair<Compound, String>> parsed, @NotNull PatternTermIndex patterns, boolean permute) {
        this.patterns = patterns;
        this.permuteBackwards = this.permuteForwards = permute;
        this.rules = permute(parsed, patterns).distinct().collect(toList());
    }


    @NotNull
    static Collection<String> load(@NotNull byte[] data) {
        return load(Lists.newArrayList(new String(data).split("\n")));
    }

    @NotNull
    static Collection<String> load(@NotNull List<String> lines) {


        StringBuilder current_rule = new StringBuilder(256);
        boolean filtering = false;

        List<String> lines2 = $.newArrayList(1024);

        for (String s : lines) {

            s = s.trim(); //HACK write a better file loader

            if (s.isEmpty() || s.startsWith("//")) {
                continue;
            }

            if (s.contains("..")) {
                s = s.replace("A..", "%A.."); //add var pattern manually to ellipsis
                s = s.replace("%A..B=_", "%A..%B=_"); //add var pattern manually to ellipsis
                s = s.replace("B..", "%B.."); //add var pattern manually to ellipsis
                s = s.replace("%A.._=B", "%A.._=%B"); //add var pattern manually to ellipsis
            }

            if (s.startsWith("try:")) {
                filtering = true;
            }

            lines2.add(s);

        }

        lines = lines2;

        if (filtering) {
            List<String> unparsed_rules = $.newArrayList(1024);
            for (String s : lines) {

                s = s.trim(); //HACK write a better file loader

                boolean currentRuleEmpty = current_rule.length() == 0;
                if (s.startsWith("//") || spacePattern.matcher(s).replaceAll(Matcher.quoteReplacement("")).isEmpty()) {

                    if (!currentRuleEmpty) {

                        if (!filtering || filtering && current_rule.toString().contains("try:")) {
                            unparsed_rules.add(current_rule.toString().trim().replace("try:", "")); //rule is finished, add it
                        }
                        current_rule.setLength(0); //start identifying a new rule
                    }

                } else {
                    //note, it can also be that the current_rule is not empty and this line contains |- which means
                    //its already a new rule, in which case the old rule has to be added before we go on
                    if (!currentRuleEmpty && s.contains("|-")) {

                        if (!filtering || filtering && current_rule.toString().contains("try:")) {
                            unparsed_rules.add(current_rule.toString().trim().replace("try:", "")); //rule is finished, add it
                        }
                        current_rule.setLength(0); //start identifying a new rule

                    }
                    current_rule.append(s).append('\n');
                }
            }

            if (current_rule.length() > 0) {
                if (!filtering || filtering && current_rule.toString().contains("try:")) {
                    unparsed_rules.add(current_rule.toString());
                }
            }

            return unparsed_rules;
            //.parallelStream();
            //.stream();
        } else {
            return lines;//.stream();
        }

    }


    @NotNull
    static Stream<Pair<Compound, String>> parse(@NotNull Collection<String> rawRules, @NotNull PatternTermIndex index) {
        return rawRules.stream()
                //.distinct()
                //.parallel()
                //.sequential()
                .map(src -> {
                    try {
                        return Tuples.pair(parse(src, index), src);
                    } catch (Narsese.NarseseException e) {
                        logger.error(" {}", e);
                        return null;
                    }
                });
    }

    @NotNull
    public static PremiseRule parse(@NotNull String src) throws Narsese.NarseseException {
        return parse(src, new PatternTermIndex());
    }

    @NotNull
    public static PremiseRule parse(@NotNull String src, @NotNull PatternTermIndex index) throws Narsese.NarseseException {

        //(Compound) index.parseRaw(src)
        String[] ab = src.split("\\|\\-");
        if (ab.length != 2) {
            throw new Narsese.NarseseException("Rule component must have arity=2, separated by \"|-\": " + src);
        }

        String A = '(' + ab[0].trim() + ')';
        Term a = index.termRaw(A);
        if (!(a instanceof Compound)) {
            throw new Narsese.NarseseException("Left rule component must be compound: " + src);
        }

        String B = '(' + ab[1].trim() + ')';
        Term b = index.termRaw(B);
        if (!(b instanceof Compound)) {
            throw new Narsese.NarseseException("Right rule component must be compound: " + src);
        }

        return new PremiseRule((Compound) a, (Compound) b);
    }

    @NotNull
    Stream<PremiseRule> permute(@NotNull Stream<Pair<Compound, String>> rawRules, @NotNull PatternTermIndex index) {
        return rawRules.map(rawAndSrc -> {

            String src = rawAndSrc.getTwo();

            Collection<PremiseRule> ur = $.newArrayList(4);
            try {
                PremiseRule preNorm = new PremiseRule(rawAndSrc.getOne());
                permute(preNorm, src, index, ur);
            } catch (RuntimeException ex) {
                throw new RuntimeException("Invalid TaskRule: " + src, ex);
            }

            return ur;
        }).flatMap(Collection::stream);
    }

    @NotNull
    public Set<PremiseRule> permute(@NotNull PremiseRule preNorm) {
        Set<PremiseRule> ur;
        permute(preNorm, "", new PatternTermIndex(), ur = $.newHashSet(1));
        return ur;
    }

    public void permute(@NotNull PremiseRule preNormRule, String src, @NotNull PatternTermIndex index, @NotNull Collection<PremiseRule> ur) {
        add(preNormRule, src, ur, index,
                (PremiseRule r) -> permuteSwap(r, src, index, ur,
                        (PremiseRule s) -> permuteBackward(src, index, ur, r)));
    }

    void permuteBackward(String src, @NotNull PatternTermIndex index, @NotNull Collection<PremiseRule> ur, @NotNull PremiseRule r) {
        if (permuteBackwards && r.permuteBackward) {

            r.backwardPermutation(index, (q, reason) -> {
                PremiseRule b = add(q, src + ':' + reason, ur, index);
                //System.out.println("BACKWARD: " + b);

                //                    //2nd-order backward
                //                    if (forwardPermutes(b)) {
                //                        permuteSwap(b, src, index, ur);
                //                    }
            });
        }
    }

    protected static void add(@NotNull PremiseRule preNorm, String src, @NotNull Collection<PremiseRule> ur, @NotNull PatternTermIndex index, @NotNull Consumer<PremiseRule> each) {

//        Term[] pp = getPremise().terms().clone();
//        pp = ArrayUtils.add(pp, TaskPositive.proto);
//        Compound newPremise = (Compound) $.the(getPremise().op(), pp);
//
//        PremiseRule r = new PremiseRule(newPremise, getConclusion());
//        @NotNull PremiseRule pos = normalize(r, index);
//
//        //System.err.println(term(0) + " |- " + term(1) + "  " + "\t\t" + remapped);

        PremiseRule pos = add(preNorm, src, ur, index);
        if (pos != null)
            each.accept(pos);

//        if (Global.NEGATIVE_RULES) {
//            PremiseRule neg = add(preNorm.negative(index), src + ":Negated", ur, index);
//            if (neg != null)
//                each.accept(neg);
//        }
    }


    public void permuteSwap(@NotNull PremiseRule r, String src, @NotNull PatternTermIndex index, @NotNull Collection<PremiseRule> ur, @NotNull Consumer<PremiseRule> then) {

        then.accept(r);

        if (permuteForwards && r.permuteForward) {

            PremiseRule bSwap = r.swapPermutation(index);
            if (bSwap != null)
                then.accept(add(bSwap, src + ":forward", ur, index));
        }

    }


    @Nullable
    static PremiseRule add(@Nullable PremiseRule q, String src, @NotNull Collection<PremiseRule> target, @NotNull PatternTermIndex index) {
        if (q == null)
            return null;
//            throw new RuntimeException("null: " + q + ' ' + src);


        PremiseRule normalized = normalize(q, index);
        normalized.setSource(src);
        return target.add(normalized) ? normalized : null;
    }

    @NotNull
    static PremiseRule normalize(@Nullable PremiseRule q, @NotNull PatternTermIndex index) {
        return q.normalizeRule(index).setup(index);
    }

    private static final Pattern spacePattern = Pattern.compile(" ", Pattern.LITERAL);


}

