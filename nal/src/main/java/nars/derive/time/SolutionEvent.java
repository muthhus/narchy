package nars.derive.time;

import nars.term.Term;

import static nars.time.Tense.ETERNAL;

public class SolutionEvent extends AbsoluteEvent {


    SolutionEvent(Term term, long start) {
        this(term, start, start != ETERNAL ? start + term.dtRange() : ETERNAL);
    }

    SolutionEvent(Term term, long start, long end) {
        super(term, start, end);
    }

//        SolutionEvent(Term unknown) {
//            super(unknown, XTERNAL, XTERNAL);
//        }
}
