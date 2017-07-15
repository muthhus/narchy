package jcog.learn.markov;

import com.google.common.collect.Iterables;
import jcog.io.Twokenize;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Adds functionality to the MarkovChain class which
 * tokenizes a String argument for use in the Markov graph.
 *
 * @author OEP
 */
public class MarkovSentence extends MarkovChain<String> {
//    public static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRUSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
//    public static final String PUNCTUATION = ";.?!";

    /**
     * Buffer to use when parsing our input source
     */
    //public final List<String> phrase = new ArrayList<String>();
//    private String mTokenChars;
//    private String mTerminateChars;

    public MarkovSentence(int tupleLength) {
        super(new HashMap<>(), tupleLength, new Random());
    }

//    public void setTokenChars(String chars) {
//        mTokenChars = chars;
//    }
//
//    public void setTerminateChars(String chars) {
//        mTerminateChars = chars;
//    }

//    /**
//     * Converts a String to a ByteArrayInputStream and parses.
//     *
//     * @param s String object to parse.
//     */
//    public void parseSentence(InputStream is) throws IOException {
//        parseSentence(new String(is.readBytes(is.readAllBytes()));
//    }

    /**
     * Stream-safe method to parse an InputStream.
     *
     * @param is InputStream to parse
     */
    public void parseSentence(String sentence)  {
        List<Twokenize.Span> phrase = Twokenize.twokenize(sentence);
        learn(Iterables.transform(phrase, Twokenize.Span::toString));
    }


    public String generateSentence() {
        return generateSentence(-1);
    }

    /**
     * Make our generated Markov phrase into a String
     * object that is more versatile.
     *
     * @return String of our Markov phrase
     */
    public String generateSentence(int len) {
        // Get our phrase as an unwieldy ArrayList
        List<String> phrase = generate(len);

        // Get our StringBuffer ready and calculate the size beforehand BECAUSE IT'S SO MUCH FASTER.
        StringBuilder sb = new StringBuilder();
        int sz = phrase.size();

        // Iterate over the ArrayList.
        for (int i = 0; i < sz; i++) {
            // Grab our word.
            String word = phrase.get(i);

            // Capitalize if this is the first word
            if (i == 0) word = word.substring(0, 1).toUpperCase() + word.substring(1);

            // Add a space if it isn't the last word.
            if (i != sz - 1) word = word + ' ';

            sb.append(word);
        }

        return sb.toString();
    }

//    /**
//     * Alias method to help us flush the buffer into the Markov engine.
//     */
//    private void flushBuffer() {
//        if (phrase.isEmpty()) return;
////		System.out.println("Adding: " + mPhraseBuffer.toString());
//        this.learn(phrase);
//        phrase.clear();
//    }

//    /**
//     * Alias method to add a word to the phrase buffer.
//     *
//     * @param word the word to add
//     */
//    private void pushWord(String word) {
//        if (word == null || word.length() == 0) return;
//        phrase.add(word.toLowerCase());
//    }
}