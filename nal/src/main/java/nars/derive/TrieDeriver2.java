package nars.derive;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Sets;
import jcog.bloom.AwesomeBitSet;
import nars.$;
import nars.Op;
import nars.derive.meta.PostCondition;
import nars.derive.meta.op.AbstractPatternOp;
import nars.derive.rule.PremiseRuleSet;
import nars.premise.Derivation;
import nars.term.Term;
import nars.term.container.TermContainer;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.*;

import static nars.time.Tense.DTERNAL;

/**
 * Created by me on 4/15/17.
 */
public class TrieDeriver2  implements Deriver {

    /** RHS, indexed by position in this list */
    final List<PostCondition> posts = $.newArrayList();

    /** essentially a conj (AND) subterm */
    class PreCondition {

        public final Set<PostCondition> enabled = new HashSet();
        final BitSet enables;
        public final Term condition;

        PreCondition(Term c) {
            this.condition = c;
            this.enables = new BitSet(posts.size());
        }

        public void enable(PostCondition p, int post) {
            if (enabled.add(p))
                enables.set(post);
        }

        public int score() {
            int split = 1;
            if (condition instanceof AbstractPatternOp.PatternOp)
                split = 8; //about half the total operator types

            return enabled.size() * split;
        }
        @Override
        public String toString() {
            return score() + " " + condition + " " + enables.toString();
        }
    }

    public TrieDeriver2(@NotNull PremiseRuleSet d) {

        Map<Term, PreCondition> mapping = new HashMap(1024);
        //HashMultimap<TermContainer,PostCondition> power = HashMultimap.create();
        d.rules.forEach(x -> {
            for (PostCondition p : x.POST) {

                int n = posts.size();
                posts.add(p);

                List<Term> c = x.conditions(p);
                for (Term b : c) {
                    mapping.compute(b, (bb, prev) -> {
                        if (prev==null)
                            prev = new PreCondition(bb);
                        prev.enable(p, n);
                        return prev;
                    });
                }

//                Sets.powerSet(new HashSet<Term>(c)).forEach(ss -> {
//                    power.put(TermContainer.the(Op.SETe, DTERNAL, ss.toArray(new Term[ss.size()])), p);
//                });
            }
        });


        List<ObjectIntPair<PreCondition>> s = $.newArrayList();
        mapping.values().forEach((k) -> {
            s.add(PrimitiveTuples.pair(k, k.score()));
        });
        s.sort((a,b) -> {

            int i = Integer.compare(b.getTwo(), a.getTwo());
            if (i == 0)
                return a.getOne().equals(b.getOne()) ? 0 : Integer.compare(a.getOne().hashCode(), b.getOne().hashCode());
            else
                return i;
        });
        s.forEach(x -> {
            System.out.println(x);
        });


    }

    @Override
    public void accept(Derivation derivation) {

    }


    public static void main(String[] args) {

        Object d = new TrieDeriver2(PremiseRuleSet.rules(true, "nal1.nal"));
        //.print(System.out);


    }
}
