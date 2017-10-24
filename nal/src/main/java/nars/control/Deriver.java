package nars.control;

import jcog.Util;
import nars.NAR;
import nars.NARS;
import nars.Op;
import nars.Param;
import nars.derive.AbstractPred;
import nars.derive.PrediTerm;
import nars.derive.PrediTrie;
import nars.derive.TrieDeriver;
import nars.derive.instrument.DebugDerivationPredicate;
import nars.derive.rule.PremiseRuleSet;
import nars.index.term.PatternIndex;

import java.io.PrintStream;
import java.util.function.Function;
import java.util.stream.Stream;

public class Deriver extends CycleService {


    public final PrediTerm<Derivation> deriver;
    private final NAR nar;

    private float minPremisesPerConcept = 1;
    private float maxPremisesPerConcept = 4;

    int conceptsPerCycle = 2;

    protected Deriver(NAR nar, String... rulesets) {
        this(PrediTrie.the(
                new PremiseRuleSet(
                        new PatternIndex(), nar, rulesets
                )), nar);
    }

    protected Deriver(PrediTerm<Derivation> deriver, NAR nar) {
        super(nar);
        this.deriver = deriver;
        this.nar = nar;
    }

    public static Function<NAR, Deriver> deriver(Function<NAR, PremiseRuleSet> rules) {
        return (nar) ->
                new Deriver(PrediTrie.the(rules.apply(nar),
                        Param.TRACE ? DebugDerivationPredicate::new : null
                ), nar);
    }

    public static Function<NAR, Deriver> deriver(int nal, String... additional) {
        assert (nal > 0 || additional.length > 0);

        return deriver(nar ->
                PremiseRuleSet.rules(nar, new PatternIndex(),
                        Derivers.defaultRules(nal, additional)
                ));
    }


    @Override
    protected void run(NAR nar) {
        Derivation d = nar.derivation.get().cycle(nar,deriver);

        nar.exe.fire(conceptsPerCycle, a -> {

            Iterable<Premise> h = a.hypothesize(nar, premises(a));
            if (h == null)
                return;

            h.forEach(p -> {

                int matchTTL = Param.TTL_PREMISE_MIN * 3;

                if (p.match(d, matchTTL) != null) {

                    int deriveTTL = Util.lerp(Util.unitize(p.task.priElseZero() / nar.priDefault(p.task.punc())),
                            nar.matchTTLmin.intValue(), nar.matchTTLmax.intValue());

                    d.derive(deriveTTL);
                }

            });

        });

        d.commit(nar);
    }


    private int premises(Activate a) {
        return Math.round(Util.lerp(a.priElseZero(), minPremisesPerConcept, maxPremisesPerConcept));
    }


    public static final Function<NAR, PrediTerm<Derivation>> NullDeriver = (n) -> new AbstractPred<Derivation>(Op.Null) {
        @Override
        public boolean test(Derivation derivation) {
            return true;
        }
    };


    public static Stream<Deriver> derivers(NAR n) {
        return n.services().filter(Deriver.class::isInstance).map(Deriver.class::cast);
    }

    public static void print(NAR n, PrintStream p) {
        derivers(n).forEach(d -> {
            p.println(d.toString());
            TrieDeriver.print(d.deriver, p);
            p.println();
        });
    }
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
