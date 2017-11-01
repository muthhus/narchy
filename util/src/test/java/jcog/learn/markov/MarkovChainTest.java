package jcog.learn.markov;

import com.google.common.base.Joiner;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MarkovChainTest {
    @Test
    public void test1() {

        MarkovChain<String> chain = new MarkovChain<String>(2);

        String phrases[] = {
                "foo foo ffoo foo foo foo bar oo",
                "foo doo hoo koo loo yoo oo too"
        };


        for (int i = 0; i < phrases.length; i++) {
            chain.learn(phrases[i].split(" "));
        }


        String phrase = "";

        MarkovSampler<String> sampler = chain.sample(new Random(2));
        for (int i = 0; i < 8; i++) {
            String word = sampler.next(8);
            if (word == null)
                break;

            phrase += word + ' ';
        }

        assertEquals("foo doo hoo koo loo yoo oo too ", phrase);

    }

    @Test
    public void testString() {
        MarkovChain<String> chain = new MarkovChain<String>(3);
        chain.learnAll(
                "she sells sea shells by the sea shore fool".split(" "),
                "a sea shell found by the beach sells for quite a bit".split(" "),
                "a sea monster sells sea shells underneath the beach house".split(" "),
                "sea shells underneath the cabinet are meant for shelly to sell sea shore".split(" ")
        );

        TreeSet<String> sentences = new TreeSet();
        int ii = 20;
        for (int i = 0; i < ii; i++) {
            long start = System.currentTimeMillis();

            List<String> phrase = chain.sample().generate(25);

            long end = System.currentTimeMillis();

            String s = Joiner.on(' ').join(phrase);
            sentences.add(s);

            System.out.println(s + "\t" + (end - start) + " ms");
        }

        assertTrue(sentences.size() >= (ii / 5), "unique sentences");

    }

}