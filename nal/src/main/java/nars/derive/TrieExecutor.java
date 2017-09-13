package nars.derive;

import jcog.list.FasterIntArrayList;
import jcog.list.FasterList;
import jcog.math.ByteShuffler;
import nars.control.Derivation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * stackless recursive virtual machine which
 * feedback controls for obeying AIKR.  the goal was to
 * write a more efficient TrieDeriver evaluation procedure
 * by avoiding recursive Java calls with an iterative loop
 * and custom stack behavior that includes the feedback
 * and shuffling requirements within it
 */
public class TrieExecutor extends AbstractPred<Derivation> {


    static final class CPU {
        static final int stackLimit = 64;
        final FasterIntArrayList ver = new FasterIntArrayList(stackLimit);
        final FasterList<PrediTerm<Derivation>> stack = new FasterList(stackLimit);

        /**
         * call to lazily revert the derivation versioning right before it's necessary
         */
        void sync(Derivation d) {
            int ss = stack.size();
            if (ss == 0) {
                //special case, just reset to zero
                ver.popTo(0);
                d.revert(0);
            } else {
                int sizeDelta = ver.size() - ss;
                if (sizeDelta > 0) {
                    //reduce the version stack to meet the instruction stack
                    d.revert(ver.pop(sizeDelta));
                }
            }
        }

        void loadFork(Derivation d, Fork f) {

            sync(d);

            int branches = f.cache.length;
            ByteShuffler b = d.shuffler;

            int stackStart = stack.size();
            final int HEADROOM = 2;
            int stackEnd = Math.min(stackLimit - HEADROOM, stackStart + branches);
            if (stackEnd > stackStart) {
                int before = d.now();


                Object[] stackData = stack.array();

                for (PrediTerm p : f.cache) {
                    push(before, p);
                }
//            System.arraycopy(f.cache, 0, stackData, stackStart, stackEnd-stackStart);
//            stack.popTo(stackEnd);

                b.shuffle(d.random, stackData, stackStart, stackEnd);
            }
        }

        void push(int before, PrediTerm x) {
            ver.add(before);
            stack.add(x);
        }

    }

    static final ThreadLocal<CPU> cpu = ThreadLocal.withInitial(CPU::new);

    private final PrediTerm<Derivation> root;

    public TrieExecutor(PrediTerm<Derivation> root) {
        super(root);
        this.root = root;
    }

    @Override
    public boolean test(Derivation d) {

        CPU c = cpu.get();
        FasterList<PrediTerm<Derivation>> stack = c.stack;
        FasterIntArrayList ver = c.ver;

        stack.clearFast();
        ver.clearFast();

        PrediTerm<Derivation> cur = root;
        do {
            if (cur instanceof Fork) {
                c.loadFork(d, (Fork) cur);
            } else if (cur instanceof AndCondition) {
                c.sync(d);
                @NotNull PrediTerm[] cache = ((AndCondition) cur).cache;
                for (int i = 0, cacheLength = cache.length; i < cacheLength; i++) {
                    PrediTerm<Derivation> p = cache[i];
                    if (p instanceof Fork) {
                        assert (i == cacheLength - 1) : "fork must occurr only in the final AND condition";
                        c.loadFork(d, (Fork) p);
                    } else {
                        if (!p.test(d))
                            break;
                    }
                }
            } else if (cur instanceof OpSwitch) {
                @Nullable PrediTerm<Derivation> next = ((OpSwitch) cur).branch(d);
                if (next != null) {
                    cur = next;
                    continue;
                } //else the switch has no path for the current context, so continue
            } else {
                c.sync(d);
                cur.test(d);
            }

            cur = stack.removeLastElseNull();
            if (cur == null) break;

        } while (d.tick());

        return true;
    }


}
