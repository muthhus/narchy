package nars.derive;

import jcog.list.FasterIntArrayList;
import jcog.list.FasterList;
import nars.control.Derivation;

import java.util.Arrays;

/** deriver virtual machine state */
final class CPU<D> {

    static final int stackLimit = 32;
    final FasterIntArrayList ver = new FasterIntArrayList(stackLimit);
    final FasterList<PrediTerm> stack = new FasterList<>(stackLimit);

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

    void fork(Derivation d, PrediTerm<Derivation>[] branch) {

//            sync(d);
        //assert (ver.size() == stack.size());

        int branches = branch.length;
        int stackStart = stack.size();
        int stackEnd = stackStart + branches;
        if (stackEnd < stackLimit) {
            int before = d.now();

            Object[] stackData = stack.array();
            System.arraycopy(branch, 0, stackData, stackStart, branches);
            stack.setSize(stackEnd);
            //assert (stack.size() == ver.size() + branches);

            d.shuffler.shuffle(d.random, stackData, stackStart, stackEnd);

            int[] va = ver.array();
            Arrays.fill(va, stackStart, stackEnd, before);
            ver.setSize(stackEnd);
            //assert (ver.size() == stack.size());

            loaded(d, stackStart, stackEnd);
        }
    }

    /** filter method for annotating a predicate being pushed on the stack.
     * it can pass the input value through or wrap it in some method
     */
    protected void loaded(Derivation d, int start, int end) {
        //<custom implemenation which wraps the new items in equally budgeted Budgeted instances
        int ttl = d.ttl;
        int num = end - start;
        int ttlPer = ttl / num;
        for (int i = start; i < end; i++) {
            stack.set(i, new Budgeted(stack.get(i), ttlPer));
        }
        //</custom implemenation>
    }

    static class Budgeted extends AbstractPred<Derivation> {
        private final PrediTerm<Derivation> rel;
        int ttl;

        Budgeted(PrediTerm<Derivation> rel, int ttl) {
            super(rel);
            this.ttl = ttl;
            this.rel = rel;
        }

        @Override
        public boolean test(Derivation d) {
            return rel.test(d);
        }

        @Override
        public PrediTerm<Derivation> exec(Derivation d, CPU cpu) {
            int fund = Math.min(ttl, d.ttl);
            this.ttl = d.getAndSetTTL(fund) - fund;
            PrediTerm<Derivation> y = rel.exec(d, cpu);
            d.addTTL(ttl); //refund
            return y;
        }
    }

//        void push(int before, @NotNull PrediTerm x) {
//            ver.add(before);
//            stack.add(x);
//        }

}
