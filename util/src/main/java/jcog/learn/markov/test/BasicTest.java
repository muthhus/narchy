package jcog.learn.markov.test;

import jcog.learn.markov.MarkovChain;

import java.util.Random;

/**
 * Created by me on 7/12/16.
 */
public class BasicTest {

    public static void main(String args[]) {
        MarkovChain<String> chain = new MarkovChain<String>(1, new Random());

        String phrases[] = {
                "foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo bar",
                "foo doo hoo koo loo yoo oo too"
        };


        for (int i = 0; i < phrases.length; i++) {
            chain.learn(phrases[i].split(" "));
        }

        String word;
        String phrase = "";
        int i = 0;
        while ((word = chain.next(10)) != null) {
            phrase += word + ' ';
            i++;
        }
        System.out.println(phrase);
    }

}
