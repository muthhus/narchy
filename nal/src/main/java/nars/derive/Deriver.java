package nars.derive;

import jcog.util.FileCache;
import nars.IO;
import nars.NAR;
import nars.derive.rule.PremiseRuleSet;
import nars.index.term.PatternTermIndex;
import nars.term.Compound;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static nars.IO.readTerm;
import static nars.derive.rule.PremiseRuleSet.parsedRules;

/**
 * Implements a strategy for managing submitted derivation processes
 * ( via the run() methods )
 * <p>
 * Created by patrick.hammer on 30.07.2015.
 * <p>
 * TODO remove Deriver and just consider any BoolPred<Derivation> a deriver
 */
public interface Deriver {


    static PremiseRuleSet DEFAULT(int level) {
        Set<String> files = new TreeSet();
        switch (level) {
            case 8:
            case 7:
                files.add("induction.nal");
                //fallthru
            case 6:
            case 5:
                files.add("nal6.nal");
                //fallthru
            case 4:
            case 3:
            case 2:
                files.add("nal3.nal");
                files.add("nal2.nal");
                //fallthru
            case 1:
                files.add("misc.nal"); //TODO split this up
                files.add("nal1.nal");
                break;
            default:
                throw new UnsupportedOperationException();
        }

        @NotNull PremiseRuleSet RULES = PremiseRuleSet.rules(true, files.toArray(new String[files.size()])       );
        return RULES;
    }

    /**
     * for now it seems there is a leak so its better if each NAR gets its own copy. adds some overhead but we'll fix this later
     * not working yet probably due to unsupported ellipsis IO codec. will fix soon
     */
    static PremiseRuleSet DEFAULT_RULES_cached() {


        return new PremiseRuleSet(
                Stream.of(
                        "nal1.nal",
                        //"nal4.nal",
                        "nal6.nal",
                        "misc.nal",
                        "induction.nal",
                        "nal2.nal",
                        "nal3.nal"
                ).flatMap(x -> {
                    try {
                        return rulesParsed(x);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return Stream.empty();
                }), new PatternTermIndex(), true);
    }




//    PremiseRuleSet DEFAULT_RULES = PremiseRuleSet.rules(true,
//                "nal1.nal",
//                //"nal4.nal",
//                "nal6.nal",
//                "misc.nal",
//                "induction.nal",
//                "nal2.nal",
//                "nal3.nal"
//        );


//    Cache<String, Deriver> derivers = Caffeine.newBuilder().build();
//    Function<String,Deriver> loader = (s) -> new TrieDeriver(PremiseRuleSet.rules(s));

//    @NotNull
//    static Deriver get(String... path) {
//        PremiseRuleSet rules = PremiseRuleSet.rules(true, path);
//        return TrieDeriver.get(rules);
//    }


    Logger logger = LoggerFactory.getLogger(Deriver.class);

    BiConsumer<Stream<Compound>, DataOutput> encoder = (x, o) -> {
        try {
            IO.writeTerm(x, o);
            //o.writeUTF(x.getTwo());
        } catch (IOException e) {
            throw new RuntimeException(e); //e.printStackTrace();
        }
    };


    @NotNull
    static Stream<Pair<Compound, String>> rulesParsed(String ruleSet) throws IOException, URISyntaxException {

        PatternTermIndex p = new PatternTermIndex();

        Function<DataInput, Compound> decoder = (i) -> {
            try {
                return //Tuples.pair(
                        (Compound) readTerm(i, p);
                //,i.readUTF()
                //);
            } catch (IOException e) {
                throw new RuntimeException(e); //e.printStackTrace();
                //return null;
            }
        };


        URL path = NAR.class.getResource("nal/" + ruleSet);

        Stream<Compound> parsed =
                FileCache.fileCache(path, PremiseRuleSet.class.getSimpleName(),
                        () -> load(ruleSet),
                        encoder,
                        decoder,
                        logger
                );

        return parsed.map(x -> Tuples.pair(x, "."));
    }

    static Stream<Compound> load(String ruleFile) {

        return parsedRules(new PatternTermIndex(), ruleFile).map(x -> x.getOne() /* HACK */);

    }

}
