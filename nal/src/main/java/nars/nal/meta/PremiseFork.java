package nars.nal.meta;

import org.jetbrains.annotations.NotNull;

/**
 * reverting fork for use during premise matching
 */
public final class PremiseFork extends ThenFork {

    @NotNull
    private final ProcTerm[] termCache;

    PremiseFork(@NotNull ProcTerm[] n) {
        super(n);
        if (n.length == 1)
            throw new RuntimeException("unnecessary use of fork");
        this.termCache = n;
    }

    @Override
    public final void accept(@NotNull PremiseEval m) {
        final int stack = m.now();
        for (ProcTerm s : termCache) {
            s.accept(m);
            m.revert(stack);
        }
    }

    @NotNull
    public static ProcTerm the(@NotNull ProcTerm[] n) {

//        //1. test for switch on all op type (TODO several term types with a a default branch for non-op type conditions)
//
//        IntObjectHashMap<ProcTerm> table  = new IntObjectHashMap<ProcTerm>();
//
//        int[] which = new int[]{-1}; //subterm index, ensure they are all referring to the same
//
//        boolean isOpSwitch = Stream.of(n).allMatch(p -> {
//
//            if (p instanceof PremiseBranch) {
//               PremiseBranch b = (PremiseBranch)p;
//               BooleanCondition<PremiseEval> c = b.cond; //condition
//               if (c instanceof AndCondition) {
//                   AndCondition cAnd = (AndCondition) c;
//                   c = (BooleanCondition<PremiseEval>) cAnd.termCache[0]; //first term which happens to hold the op type, but for more robustness, find and pull out any op tpyes which are not in this first position
//               }
//               if (c instanceof SubTermOp) {
//                   SubTermOp sb = (SubTermOp)c;
//
//
//                   if (which[0] == -1)
//                       which[0] = sb.subterm;
//                   else if (which[0] != sb.subterm)
//                       return false;
//
//                   ProcTerm d; //consequence
//
//
//                   if (c instanceof AndCondition) {
//                       AndCondition<PremiseEval> cAnd = (AndCondition) c;
//                       d = (ProcTerm) cAnd.without(sb);
//                       if (d == null)
//                           d = b.conseq;
//                   } else {
//                       d = b.conseq;
//                   }
//
//                   table.put(sb.op, d);
//
//                   return true;
//               }
//            }
//
//            return false;
//
//        });
//        if (which[0]!=-1 && table.size() > 1) {
//            System.out.println(table);
//        }


        return new PremiseFork(n);
    }
}
