package nars.util.signal.markov.test;


import nars.util.signal.markov.MarkovChain;

import java.util.List;

public class StringTest {
    public static void main(String[] args) {
        String phrase1[] = "she sells sea shells by the sea shore fool".split(" ");
        String phrase2[] = "a sea shell found by the beach sells for quite a bit".split(" ");
        String phrase3[] = "a sea monster sells sea shells underneath the beach house".split(" ");
        String phrase4[] = "sea shells underneath the cabinet are meant for shelly to sell sea shore".split(" ");

        MarkovChain<String> chain = new MarkovChain<String>(3);
        chain.learn(phrase1);
        chain.learn(phrase2);
        chain.learn(phrase3);
        chain.learn(phrase4);

        long start = System.currentTimeMillis();
        List<String> phrase = chain.generate();
        long dt = System.currentTimeMillis() - start;

        for (String word : phrase) {
            System.out.print(word + " ");
        }
        System.out.println();

        System.out.println(dt + " ms");
    }
}
