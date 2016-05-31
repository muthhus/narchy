package nars.nal.meta;

import nars.Op;
import nars.term.compound.GenericCompound;
import nars.term.container.TermSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


/** parallel branching */
public final class Fork extends GenericCompound<ProcTerm> implements ProcTerm {
    @NotNull
    protected final ProcTerm[] termCache;



    protected Fork(@NotNull ProcTerm[] actions) {
        super(Op.CONJUNCTION, TermSet.the(actions));
        if (actions.length == 1)
            throw new RuntimeException("unnecessary use of fork");
        this.termCache = actions;
    }

    @Override
    public final void accept(@NotNull PremiseEval m) {
        final int stack = m.now();
        for (ProcTerm s : termCache) {
            s.accept(m);
            m.revert(stack);
        }
    }

    @Nullable
    public static ProcTerm compile(@NotNull List<ProcTerm> t) {
        return compile(t.toArray(new ProcTerm[t.size()]));
    }

    @Nullable public static ProcTerm compile(@NotNull ProcTerm[] n) {
        switch (n.length) {
            case 0: return null;
            case 1: return n[0];
            default: return new Fork(n);
        }
    }

    @Override
    public void appendJavaProcedure(@NotNull StringBuilder s) {
        //s.append("/* " + this + "*/");
        for (ProcTerm p : terms()) {
            s.append("\t\t");
            p.appendJavaProcedure(s);
            s.append('\n');
        }
    }


//    public static PremiseMatch fork(PremiseMatch m, ProcTerm<PremiseMatch> proc) {
//        int revertTime = m.now();
//        proc.accept(m);
//        m.revert(revertTime);
//        return m;
//    }

//        @Override public void accept(C m) {
//
////            try {
////                method.invoke(m);
////            } catch (Throwable throwable) {
////                throwable.printStackTrace();
////            }
//
//            int revertTime = m.now();
//            for (ProcTerm<PremiseMatch> s : terms()) {
//                s.accept(m);
//                m.revert(revertTime);
//            }
//        }

//private final MethodHandle method;
//            try {
//                MethodHandles.Lookup l = MethodHandles.publicLookup();
//
//                Binder b = null;
//                for (ProcTerm p : children) {
//                    //MethodHandle ph = l.findVirtual(p.getClass(), "accept", methodType(PremiseMatch.class));
//
//
//                    Binder a = new Binder(Binder.from(PremiseMatch.class, PremiseMatch.class)
//                            .append(ProcTerm.class, p))
//                            .foldStatic(ThenFork.class, "fork").dropLast(2);
//
//                    if (b!=null)
//                        b = a.to(b);
//                    else
//                        b = a;
//
//                }
//                this.method = b!=null ? b.identity() : null;
//
//            } catch (Exception e) {
//                e.printStackTrace();
//                throw new RuntimeException(e);
//            }

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


}
