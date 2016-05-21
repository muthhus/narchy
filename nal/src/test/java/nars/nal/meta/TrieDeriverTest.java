package nars.nal.meta;

import com.google.common.collect.Sets;
import nars.concept.Concept;
import nars.nal.Deriver;
import nars.term.Termed;
import nars.term.index.PatternIndex;
import org.apache.commons.math3.stat.Frequency;
import org.junit.Test;

import java.util.*;

import static java.lang.System.out;
import static org.junit.Assert.*;

public class TrieDeriverTest {

    final static TrieDeriver d = Deriver.getDefaultDeriver();

    @Test public void testRuleTrie() {

        d.trie.printSummary();
        /*for (Term p : x.roots) {
            out.println();
            out.println(p);
        }*/
        assert(d.roots.length > 1);
    }

    @Test public void printRuleSet() {

//        List<PremiseRule> rr = d.rules.rules;
//        System.out.println(rr.size() + " rules");
//        rr.forEach(r -> {
//            System.out.println(r);
//        });

        d.trie.costAnalyze((t) -> 1, System.out);
    }

    @Test
    public void testRuleStatistics() {
        List<PremiseRule> R = d.rules.rules;
        int registeredRules = R.size();


        Frequency f = new Frequency();
        R.forEach(f::addValue);
        Iterator<Map.Entry<Comparable<?>, Long>> ii = f.entrySetIterator();
        while (ii.hasNext()) {
            Map.Entry<Comparable<?>, Long> e = ii.next();
            if (e.getValue() > 1) {
                System.err.println("duplicate: " + e);
            }
        }
        out.println("total: " + f.getSumFreq() + ", unique=" + f.getUniqueCount());

        HashSet<PremiseRule> setRules = Sets.newHashSet(R);

        assertEquals("no duplicates", registeredRules, setRules.size());

        Set<BoolCondition> preconds = new HashSet();
        int totalPrecond = 0;

        out.println("total precondtions = " + totalPrecond + ", unique=" + preconds.size());

        //preconds.forEach(p -> System.out.println(p));


        //Set<TaskBeliefPair> ks = d.ruleIndex.keySet();
//        System.out.println("Patterns: keys=" + ks.size() + ", values=" + d.ruleIndex.size());
//        for (TaskBeliefPair pp : ks) {
//            System.out.println(pp + " x " + d.ruleIndex.get(pp).size());
//
//        }


    }

//    @Test public void testPostconditionSingletons() {
////        System.out.println(PostCondition.postconditions.size() + " unique postconditions " + PostCondition.totalPostconditionsRequested);
////        for (PostCondition p : PostCondition.postconditions.values()) {
////            System.out.println(p);
////        }
//
//    }

    @Test public void testPatternIndexContainsNoConcepts() {
        PatternIndex p = d.rules.patterns;
        //out.println(p.data);
        //out.println(p.atoms);
        p.forEach(t -> {

            assertFalse( t instanceof Concept );

            //test all subterms are in the pattern index too
            t.term().recurseTerms((s, parent)->{
                Termed sub = p.get(s, false);
                if (sub == null) {
                    System.out.println("subterm " + s + " of " + parent + " not in PatternIndex");
                }
                assertNotNull(sub);
            });

            //out.println(t);
        });
        //System.out.println("compounds: "+ p.internings + " internings, " + p.size() + " unique");

    }




//
//        FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

//
//        List<PremiseRule> R = Deriver.getDefaultDeriver().rules.rules;
//        for (PremiseRule r : R) {
//            byte barray[] = conf.asByteArray(r);
//            System.out.println(r + " "+ barray.length + " bytes");
//
//            Object decoded = conf.asObject(barray);
//            System.out.println("\t " + decoded);
//        }
//    }

//    @Test
//    public void testDerivationComparator() {
//
//        NARComparator c = new NARComparator(
//                new Default(),
//                new Default()
//        ) {
//
//
//        };
//        c.input("<x --> y>.\n<y --> z>.\n");
//
//
//
//        int cycles = 64;
//        for (int i = 0; i < cycles; i++) {
//            if (!c.areEqual()) {
//
//                /*System.out.println("\ncycle: " + c.time());
//                c.printTasks("Original:", c.a);
//                c.printTasks("Rules:", c.b);*/
//
////                System.out.println(c.getAMinusB());
////                System.out.println(c.getBMinusA());
//            }
//            c.frame(1);
//        }
//
//        System.out.println("\nDifference: " + c.time());
//        System.out.println("Original - Rules:\n" + c.getAMinusB());
//        System.out.println("Rules - Original:\n" + c.getBMinusA());
//
//    }
}
