package nars.derive.time;

import nars.term.Term;

import static nars.time.Tense.ETERNAL;

public class SolutionEvent extends AbsoluteEvent {


    SolutionEvent(ITemporalize  t, Term term, long start) {
        this(t, term, start, start != ETERNAL ? start + term.dtRange() : ETERNAL);
    }

    SolutionEvent(ITemporalize t, Term term, long start, long end) {
        super(t, term, start, end);
    }

//        SolutionEvent(Term unknown) {
//            super(unknown, XTERNAL, XTERNAL);
//        }
}
