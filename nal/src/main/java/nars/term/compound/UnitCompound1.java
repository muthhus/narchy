package nars.term.compound;

import jcog.Util;
import nars.IO;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import nars.term.container.TermVector1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;

import static com.google.common.collect.Iterators.singletonIterator;
import static nars.Op.NEG;
import static nars.time.Tense.DTERNAL;

/**
 * Compound inheriting directly from TermVector1
 * NOT READY YET
 */
public class UnitCompound1 extends TermVector1 implements Compound {

    private final Op op;
    private final int hash;

    public UnitCompound1(@NotNull Op op, @NotNull Term arg) {
        super(arg);

        this.op = op;
        this.hash = Util.hashCombine(hashCodeSubTerms(), op.ordinal(), DTERNAL);
        if (arg.isNormalized()) setNormalized();
    }

//    @Override
//    public int init(@NotNull int[] meta) {
//        the.init(meta);
//        //meta[5] |= op.bit;
//        return hash;
//    }


    @Override
    public @NotNull Term unneg() {
        return op == NEG ? sub : this;
    }

    @Override
    public int structure() {
        return op.bit | super.structure();
    }

    @Override
    public void init(@NotNull int[] meta) {
        sub.init(meta);
        meta[4]++; //for wrapping it
        meta[5] |= op().bit;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public int hashCodeSubTerms() {
        return super.hashCode();
    }


    @Override
    public boolean equals(@Nullable Object that) {
        if (this == that) return true;

        if (!(that instanceof Compound)) {
            return false;
        }

        Compound t = (Compound) that;

        return hash == that.hashCode() && (op == t.op()) && (t.size() == 1) && /*&& (t.dt() == DTERNAL) &&*/ sub.equals(t.sub(0));
    }


    @NotNull
    @Override
    public String toString() {
        return IO.Printer.stringify(this).toString();
    }

    @Override
    public @NotNull Op op() {
        return op;
    }

    @Deprecated /* HACK */
    @Override
    public @NotNull TermContainer subterms() {
        return new SubtermView(this);
    }

    @Override
    public boolean isTemporal() {
        return sub.isTemporal();
    }


    @Override
    public boolean impossibleSubTermVolume(int otherTermVolume) {
        return otherTermVolume > sub.volume() /* volume() -  size() */;
    }

    @Override
    public boolean isNormalized() {
        return sub.isNormalized();
    }

    @Override
    public void setNormalized() {
        if (sub instanceof Compound)
            ((Compound) sub).setNormalized();
    }

    @Override
    public int dt() {
        return DTERNAL;
    }

    private static final class SubtermView implements TermContainer {
        private final UnitCompound1 c;

        public SubtermView(UnitCompound1 c) {
            this.c = c;
        }

        @Override
        public boolean isTemporal() {
            return c.sub.isTemporal();
        }

        @Override
        public int vars() {
            return c.sub.vars();
        }

        @Override
        public int varQuery() {
            return c.sub.varQuery();
        }

        @Override
        public int varDep() {
            return c.sub.varDep();
        }

        @Override
        public int varIndep() {
            return c.sub.varIndep();
        }

        @Override
        public int varPattern() {
            return c.varPattern();
        }


        @Override
        public int structure() {
            return c.sub.structure();
        }

        @Override
        public int volume() {
            return c.volume();
        }

        @Override
        public int complexity() {
            return c.complexity();
        }

        @Override
        public @NotNull Term sub(int i) {
            assert(i == 0);
            return c.sub;
        }

        @Override
        public int hashCode() {
            return c.hashCodeSubTerms();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;

            if (obj instanceof TermContainer) {
                TermContainer t = (TermContainer) obj;
                if (t.size() == 1) {
                    return c.sub.equals(t.sub(0));
                }
            }
            return false;
        }

        @Override
        public void forEach(Consumer<? super Term> action, int start, int stop) {
            assert(start == 0 && stop == 1);
            action.accept(c.sub);
        }

        @NotNull
        @Override
        public Iterator<Term> iterator() {
            return singletonIterator(c.sub);
        }

        @Override
        public int size() {
            return 1;
        }
    }
}
