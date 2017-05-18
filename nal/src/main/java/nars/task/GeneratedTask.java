package nars.task;

import nars.term.Compound;
import nars.truth.Truth;


public class GeneratedTask extends NALTask {

    public GeneratedTask(Compound term, byte punc, Truth truth, long creation, long start, long end, long[] evidence) {
        super(term, punc, truth, creation, start, end, evidence);
    }

    @Override
    public boolean isInput() {
        return false;
    }
}
