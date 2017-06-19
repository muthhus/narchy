package jcog.bloom;

import jcog.bloom.hash.HashProvider;

import java.util.Random;

/**
 * Created by jeff on 14/05/16.
 */
public class StableBloomFilter<E> implements CountingLeakySet<E> {

    private final HashProvider<E> hashProvider;
    private final byte[] cells;
    private final int numberOfCells;
    private final int numberOfHashes;
    private final Random rng = new Random();

    public StableBloomFilter(int numberOfCells,
                             int numberOfHashes,
                             HashProvider<E> hashProvider) {
        this.numberOfCells = numberOfCells;
        this.numberOfHashes = numberOfHashes;
        this.cells = new byte[numberOfCells];
        this.hashProvider = hashProvider;
    }

    /** if the element isnt contained, add it. return true if added, false if already present.*/
    public boolean addIfMissing(E element) {
        int[] hash = hash(element);
        boolean c = contains(hash);
        if (!c) {
            add(hash);
            return true;
        }
        return false;
    }

    @Override
    public void add(E element) {
        add(hash(element));
    }

    @Override
    public boolean contains(E element) {
        return contains(hash(element));
    }

    public void add(int[] indices) {
        for (int i = 0; i < numberOfHashes; i++) {
            increment(indices[i]);
        }
    }

    public boolean contains(int[] indices) {
        boolean mightContain = true;
        for (int i = 0; i < numberOfHashes; i++) {
            mightContain &= cells[indices[i]] > 0;
        }

        return mightContain;
    }


    @Override
    public void remove(E element) {
        int[] indices = hash(element);

        remove(indices);
    }

    public void remove(int[] indices) {
        for (int i = 0; i < numberOfHashes; i++) {
            decrement(indices[i]);
        }
    }


    public void unlearn(float rate) {
        int unlearnedCells = Math.round(numberOfCells * rate);
        unlearn(unlearnedCells);
    }
    public void unlearn(int unlearnedCells) {
        for (int i = 0; i < unlearnedCells; i++) {
            int index = rng.nextInt(numberOfCells);
            decrement(index);
        }
    }

    public int[] hash(E element) {
        int[] hashes = new int[numberOfHashes];

        long h1 = hashProvider.hash1(element);
        long h2 = hashProvider.hash2(element);
        for (int i = 0; i < numberOfHashes; i++) {
            hashes[i] = Math.abs((int) ((h1 + i * h2) % numberOfCells));
        }

        return hashes;
    }

    private void decrement(int idx) {
        if (cells[idx] > 0) {
            cells[idx] -= 1;
        }
    }

    private void increment(int idx) {
        if (cells[idx] < Byte.MAX_VALUE) {
            cells[idx] += 1;
        }
    }

}
