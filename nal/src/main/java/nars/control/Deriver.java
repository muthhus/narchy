package nars.control;

import jcog.Util;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.derive.*;
import nars.derive.instrument.DebugDerivationPredicate;
import nars.derive.rule.PremiseRuleSet;
import nars.index.term.PatternIndex;

import java.io.PrintStream;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * an individual deriver process: executes a particular Deriver model
 * specified by a set of premise rules.
 * <p>
 * runtime intensity is metered and throttled by causal feedback
 */
public class Deriver extends NARService {

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

    public final PrediTerm<Derivation> deriver;
    private final NAR nar;
    private final Causable can;

    private float minPremisesPerConcept = 3;
    private float maxPremisesPerConcept = 9;

    protected Deriver(PrediTerm<Derivation> deriver, NAR nar) {
        super(nar);
        this.deriver = deriver;
        this.nar = nar;

        Try t = (Try) ((AndCondition) (deriver)).cache[((AndCondition) (deriver)).cache.length - 1]; //HACK

        //this.cause = nar.newCauseChannel(this);
        this.can = new Causable(nar) {
            @Override
            protected int next(NAR n, int iterations) {
                return Deriver.this.run(iterations);
            }

            @Override
            public float value() {
                t.cache.update(nar.time());
                return t.cache.valueSum();
            }
        };
        //this.can.can.update(1,1,0.0);
    }


    protected int run(int toFire) {


        NAR nar = this.nar;
        Derivation d = derivation.get().cycle(nar, deriver);

        int matchTTL = Param.TTL_PREMISE_MIN * 3;
        int ttlMin = nar.matchTTLmin.intValue();
        int ttlMax = nar.matchTTLmax.intValue();


        int fireRemain[] = new int[]{toFire};
        BatchActivation activator = BatchActivation.get();


        nar.exe.fire(a -> {

            int hh = premises(a);
            Iterable<Premise> h = a.hypothesize(nar, activator, hh);

            if (h != null) {

                for (Premise p : h) {

                    if (p.match(d, matchTTL) != null) {

                        int deriveTTL = Util.lerp(Util.unitize(
                                p.task.priElseZero() / nar.priDefault(p.task.punc())),
                                ttlMin, ttlMax);

                        d.derive(deriveTTL);
                    }
                }
            } else {
                //premise miss
            }


            return --fireRemain[0] > 0;
        });

        activator.commit(nar);

        int derived = d.commit(nar::input);

        return toFire - fireRemain[0];
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

    //    public final IterableThreadLocal<Derivation> derivation =
//            new IterableThreadLocal<>(() -> new Derivation(this));
    public static final ThreadLocal<Derivation> derivation =
            ThreadLocal.withInitial(Derivation::new);

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
