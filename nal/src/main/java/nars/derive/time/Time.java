package nars.derive.time;

import static nars.time.Tense.*;

/**
 * used for preserving an offset within an eternal context
 */
public class Time {

    //        public static Time Unknown = new Time(ETERNAL, XTERNAL);
    public final long base;
    public final int offset;

    @Override
    public String toString() {
        return str(base) + '|' + str(offset);
    }

    static String str(int offset) {
        if (offset == XTERNAL)
            return "+-";
        else
            return Integer.toString(offset);
    }

    static String str(long base) {
        if (base == ETERNAL)
            return "ETE";
        else
            return Long.toString(base);
    }


    static Time the(long base, int offset) {
//            if (base == ETERNAL && offset == XTERNAL)
//                return Unknown;
//            else {

        if (base != ETERNAL && offset != DTERNAL && offset != XTERNAL)
            return new Time(base + offset, 0); //direct absolute
        else
            return new Time(base, offset);
//            }
    }

    private Time(long base, int offset) {
        this.base = base;
        assert(offset!=DTERNAL && offset!=XTERNAL);
        this.offset = offset;
    }


    public Time add(int offset) {

        assert(offset!=DTERNAL && offset!=XTERNAL);

        if (offset == 0)
            return this;

//        if (this.offset == DTERNAL && offset == DTERNAL)
//            return this; //no effect, adding dternal to dternal
//
//        assert (this.offset != DTERNAL && offset != DTERNAL) :
//                "this.base=" + this.base + ", this.offset=" + this.offset + " + " + offset + " = ?";

//        if (this.offset == XTERNAL)
//            return Time.the(base, offset); //set initial dt
//        else
            return Time.the(base, this.offset + offset);
    }

    public long abs() {
//            if (base == ETERNAL) {
//                return ETERNAL;
//            }
//
//            assert(offset!=XTERNAL);
//            assert(offset!=DTERNAL);
//            return base + offset;
        if (base != ETERNAL) {
            if (offset == XTERNAL || offset == DTERNAL)
                return base;
            else
                return base + offset;
        } else
            return ETERNAL;
    }
}
