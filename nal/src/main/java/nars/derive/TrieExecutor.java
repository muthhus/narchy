package nars.derive;

import jcog.list.FasterIntArrayList;
import jcog.list.FasterList;
import nars.control.Derivation;
import org.jetbrains.annotations.NotNull;

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


    static final class CPU<D> {
        static final int stackLimit = 32;
        final FasterIntArrayList ver = new FasterIntArrayList(stackLimit);
        final FasterList<PrediTerm<D>> stack = new FasterList<>(stackLimit);

//        /**
//         * call to lazily revert the derivation versioning right before it's necessary
//         */
//        void sync(Derivation d) {
//            int ss = stack.size();
//            int vv = ver.size();
//            if (ss == vv)
//                return;
//
//
//            assert (vv > ss);
//            d.revert(ver.pop(vv - ss)); //reduce the version stack to meet the instruction stack
//        }

        void fork(Derivation d, PrediTerm<Derivation>[] f) {

//            sync(d);
            //assert (ver.size() == stack.size());

            int branches = f.length;
            int stackStart = stack.size();
            int stackEnd = stackStart + branches;
            if (stackEnd < stackLimit) {
                int before = d.now();

                //FAST
                Object[] stackData = stack.array();
                System.arraycopy(f, 0, stackData, stackStart, branches);
                stack.setSize(stackEnd);

                d.shuffler.shuffle(d.random, stackData, stackStart, stackEnd);

                //assert (stack.size() == ver.size() + branches);

                //FAST
                int[] va = ver.array();
                Arrays.fill(va, stackStart, stackEnd, before);
                ver.setSize(stackEnd);

                //assert (ver.size() == stack.size());
            }
        }

//        void push(int before, @NotNull PrediTerm x) {
//            ver.add(before);
//            stack.add(x);
//        }

    }

    final static ThreadLocal<CPU<Derivation>> cpu = ThreadLocal.withInitial(CPU::new);

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
        while (true) {

            PrediTerm<Derivation> next = cur.exec(d, c);

            if (next == cur) {
                break; //termination signal
            } else if (next == null) {
                cur = stack.removeLastElseNull();
                if (cur == null || !d.revertAndContinue(ver.pop()))
                    break;
            } else {
                cur = next;
            }

        }// while (d.live());

        return true;
    }


}
