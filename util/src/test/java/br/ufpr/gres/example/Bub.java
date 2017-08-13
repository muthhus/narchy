//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package br.ufpr.gres.example;

import java.util.ArrayList;

public class Bub {
    private final ArrayList<Integer> elements = new ArrayList();

    public Bub() {
    }

    public void add(int value) throws IllegalArgumentException {
        if (value < 0) {
            throw new IllegalArgumentException();
        } else {
            this.elements.add(value);
        }
    }

    public void bubbleSort() {
        for(int out = this.elements.size() - 1; out > 0; --out) {
            for(int in = 0; in < out; ++in) {
                int x = this.elements.get(in).intValue();
                int y = this.elements.get(in + 1).intValue();
                if (x > y) {
                    this.swap(in, in + 1);
                }
            }
        }

    }

    public int get(int i) {
        return this.elements.get(i).intValue();
    }

    public ArrayList<Integer> getVector() {
        return this.elements;
    }

    public int size() {
        return this.elements.size();
    }

    private void swap(int i, int j) {
        int temp = this.elements.get(i).intValue();
        this.elements.set(i, this.elements.get(j));
        this.elements.set(j, temp);
    }
}
