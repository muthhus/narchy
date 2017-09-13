package nars.derive;

import jcog.list.FasterIntArrayList;
import jcog.list.FasterList;
import jcog.math.ByteShuffler;
import nars.control.Derivation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

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
        static final int stackLimit = 128;
        final FasterIntArrayList ver = new FasterIntArrayList(stackLimit);
        final FasterList<PrediTerm<Derivation>> stack = new FasterList(stackLimit);

        /**
         * call to lazily revert the derivation versioning right before it's necessary
         */
        void sync(Derivation d) {
            int ss = stack.size();
            int vv = ver.size();
            if (ss == vv)
                return;


            assert (vv > ss);
            //reduce the version stack to meet the instruction stack
            d.revert(ver.pop(vv - ss));


            //assert (ver.size() == stack.size());
        }

        void queue(Derivation d, Fork f) {

            sync(d);
            assert (ver.size() == stack.size());

            int branches = f.cache.length;
            int stackStart = stack.size();
            int stackEnd = stackStart + branches;
            if (stackEnd < stackLimit) {
                int before = d.now();

                //FAST
                Object[] stackData = stack.array();
                System.arraycopy(f.cache, 0, stackData, stackStart, branches);
                stack.popTo(stackEnd - 1);
                d.shuffler.shuffle(d.random, stackData, stackStart, stackEnd);

                //assert (stack.size() == ver.size() + branches);

                //FAST
                int[] va = ver.array();
                Arrays.fill(va, stackStart, stackEnd, before);
                ver.popTo(stackEnd - 1);

                //assert (ver.size() == stack.size());
            }
        }

        void push(int before, PrediTerm x) {
            ver.add(before);
            stack.add(x);
        }

    }

    final static ThreadLocal<CPU> cpu = ThreadLocal.withInitial(CPU::new);

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
        cycle: do {
            if (cur instanceof Fork) {
                c.queue(d, (Fork) cur);
            } else if (cur instanceof AndCondition) {
                c.sync(d);
                //int preAnd = d.now();
                @NotNull PrediTerm[] cache = ((AndCondition) cur).cache;
                for (int i = 0, cacheLength = cache.length; i < cacheLength; i++) {
                    PrediTerm<Derivation> p = cache[i];
                    if (p instanceof Fork) {
                        assert (i == cacheLength - 1) : "fork must occurr only in the final AND condition";
                        c.queue(d, (Fork) p);
                    } else {
                        if (!p.test(d)) {
                            break;
                        }
                    }
                }

                //d.revert(preAnd);

                //continue; the and has reached the end

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
            if (cur == null)
                break;

        } while (d.live());

        return true;
    }


}
