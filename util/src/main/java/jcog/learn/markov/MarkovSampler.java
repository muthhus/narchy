package jcog.learn.markov;

import jcog.list.FasterList;

import java.util.List;
import java.util.Random;

public class MarkovSampler<T> {

    public final MarkovChain<T> model;

    /**
     * Pointer to the current node. Methods next() uses this
     */
    private MarkovChain.Chain<T> current;

    /**
     * Index for which data element is next in our tuple
     */
    private int tupleIndex;

    /**
     * Keeps up with how long our gradual chain is
     */
    private int chainSize;

    /**
     * Nodes use this to find the next node
     */
    private final Random rng;

    public MarkovSampler(MarkovChain<T> model, Random rng) {
        this.model = model;
        this.rng = rng;
    }

    public List<T> generate() {
        return generate(-1);
    }

    /**
     * Use our graph to randomly generate a possibly valid phrase
     * from our data structure.
     *
     * @param max sequence length, or -1 for unlimited
     * @return generated phrase
     */
    public List<T> generate(int maxLen) {
        // Go ahead and choose our first node
        MarkovChain.Chain<T> current = model.START.next(rng);

        // We will put our generated phrase in here.
        List<T> phrase = new FasterList<T>();

        // As a safety, check for nulls
        // Iterate til we get to the trailer
        int s = 0;
        while (current != null && current != model.END) {
            // Iterate over the data tuple in the node and add stuff to the phrase
            // if it is non-null
            List<T> cd = current.data;

            if (maxLen != -1 && (s + cd.size() >= maxLen)) {

                //only get the prefix up to a certain length to avoid
                for (int i = 0; i < maxLen - s; i++) {
                    phrase.add(cd.get(i));
                    s++;
                }
                break;

            } else {
                phrase.addAll(cd);
                s += cd.size();
            }

            if (maxLen != -1 && s == maxLen)
                break;

            current = current.next(rng);
        }

        // Out pops pure genius
        return phrase;
    }

    /**
     * Re-initialize the chain pointer  and
     * tuple index to start from the top.
     */
    public void reset() {
        current = null;
        tupleIndex = 0;
    }

    /**
     * Returns the next element in our gradual chain.
     * Ignores maximum length.
     *
     * @return next data element
     */
    public T next() {
        return next(false, 0);
    }

    /**
     * Returns the next element and loops to the front of chain
     * on termination.
     *
     * @return next element
     */
    public T nextLoop() {
        return next(true, 0);
    }

    public T next(int maxLength) {
        return next(false, maxLength);
    }

    public T next(boolean loop) {
        return next(loop, 0);
    }

    /**
     * Get next element pointed by our single-element.
     * This will also update the data structure to get ready
     * to serve the next data element.
     *
     * @param loop if you would like to loop
     * @return data element at the current node tuple index
     */
    public T next(boolean loop, int maxLength) {

        if (model.nodes.isEmpty())
            return null;

        // In case mCurrent hasn't been initialized yet.
        if (current == null || current == model.START) current = model.START.next(rng);

        // Handle behavior in case we're at the trailer at the start.
        if (current == model.END) {
            if (loop == true) {

                if (maxLength > 0 && chainSize >= maxLength)
                    current = model.START.next(rng);
                else
                    current = model.START.next(rng);

                tupleIndex = 0;
            }
            // No more data for non-loopers
            else {
                return null;
            }
        }

        T returnValue = current.get(tupleIndex);

        tupleIndex++;
        chainSize++;

        // We've reached the end of this tuple.
        if (tupleIndex >= current.length()) {

            if (maxLength > 0 && chainSize >= maxLength) current = current.next(rng);
            else current = current.next(rng);

            tupleIndex = 0;
        }

        return returnValue;
    }

}
