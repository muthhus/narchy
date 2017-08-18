package nars.derive;

import nars.NAR;
import nars.Param;
import nars.control.Derivation;
import nars.derive.instrument.DebugDerivationPredicate;
import nars.derive.rule.PremiseRuleSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Implements a strategy for managing submitted derivation processes
 * ( via the run() methods )
 * <p>
 * Created by patrick.hammer on 30.07.2015.
 * <p>
 * TODO remove Deriver and just consider any BoolPred<Derivation> a deriver
 */
public interface Deriver {


    static PremiseRuleSet DEFAULT(int level, String... otherFiles) {
        Set<String> files = new TreeSet();
        switch (level) {
            case 8:
            case 7:
                //TODO move temporal induction to a separate file
                //fallthru
            case 6:
            case 5:
                files.add("induction.nal");  //TODO nal6 only needs general induction, not the temporal parts
                files.add("nal6.nal");
                files.add("misc.nal"); //TODO split this up
                //fallthru
            case 4:
            case 3:
            case 2:
                files.add("nal3.nal");
                files.add("nal2.nal");
                //fallthru
            case 1:
                files.add("nal1.nal");
                break;
            default:
                throw new UnsupportedOperationException();
        }

        Collections.addAll(files, otherFiles);

        @NotNull PremiseRuleSet RULES = PremiseRuleSet.rules(true, files.toArray(new String[files.size()])       );
        return RULES;
    }

    static Function<NAR, PrediTerm<Derivation>> newDeriver(int nal, String... otherFiles) {
        if (nal == 0) {
            return (n) -> PrediTerm.NullDeriver;
        }

        return (nar) -> {
            PrediTerm<Derivation> x = TrieDeriver.the(DEFAULT(nal, otherFiles), nar, (PrediTerm<Derivation> d) -> {
                if (Param.TRACE)
                    return new DebugDerivationPredicate(d);
                else
                    return d;
            });
            if (Param.TRACE) {
                TrieDeriver.print(x, System.out);
            }
            return x;
        };
    }

//    /**
//     * for now it seems there is a leak so its better if each NAR gets its own copy. adds some overhead but we'll fix this later
//     * not working yet probably due to unsupported ellipsis IO codec. will fix soon
//     */
//    static PremiseRuleSet DEFAULT_RULES_cached() {
//
//
//        return new PremiseRuleSet(
//                Stream.of(
//                        "nal1.nal",
//                        //"nal4.nal",
//                        "nal6.nal",
//                        "misc.nal",
//                        "induction.nal",
//                        "nal2.nal",
//                        "nal3.nal"
//                ).flatMap(x -> {
//                    try {
//                        return rulesParsed(x);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    return Stream.empty();
//                }), new PatternTermIndex(), true);
//    }




//    PremiseRuleSet DEFAULT_RULES = PremiseRuleSet.rules(true,
//                "nal1.nal",
//                //"nal4.nal",
//                "nal6.nal",
//                "misc.nal",
//                "induction.nal",
//                "nal2.nal",
//                "nal3.nal"
//        );


//    Cache<String, Deriver> derivers = Caffeine.newBuilder().builder();
//    Function<String,Deriver> loader = (s) -> new TrieDeriver(PremiseRuleSet.rules(s));

//    @NotNull
//    static Deriver get(String... path) {
//        PremiseRuleSet rules = PremiseRuleSet.rules(true, path);
//        return TrieDeriver.get(rules);
//    }


//    Logger logger = LoggerFactory.getLogger(Deriver.class);
//
//    BiConsumer<Stream<Compound>, DataOutput> encoder = (x, o) -> {
//        try {
//            IO.writeTerm(x, o);
//            //o.writeUTF(x.getTwo());
//        } catch (IOException e) {
//            throw new RuntimeException(e); //e.printStackTrace();
//        }
//    };
//
//
//    @NotNull
//    static Stream<Pair<PremiseRule, String>> rulesParsed(String ruleSet) throws IOException, URISyntaxException {
//
//        PatternTermIndex p = new PatternTermIndex();
//
//        Function<DataInput, PremiseRule> decoder = (i) -> {
//            try {
//                return //Tuples.pair(
//                        (PremiseRule) readTerm(i, p);
//                //,i.readUTF()
//                //);
//            } catch (IOException e) {
//                throw new RuntimeException(e); //e.printStackTrace();
//                //return null;
//            }
//        };
//
//
//        URL path = NAR.class.getResource("nal/" + ruleSet);
//
//        Stream<PremiseRule> parsed =
//                FileCache.fileCache(path, PremiseRuleSet.class.getSimpleName(),
//                        () -> load(ruleSet),
//                        encoder,
//                        decoder,
//                        logger
//                );
//
//        return parsed.map(x -> Tuples.pair(x, "."));
//    }
//
//    static Stream<PremiseRule> load(String ruleFile) {
//        return parsedRules(new PatternTermIndex(), ruleFile).map(Pair::getOne /* HACK */);
//    }

}
