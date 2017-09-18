package nars.derive;

import jcog.Util;
import jcog.list.FasterList;
import jcog.trie.TrieNode;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.control.Derivation;
import nars.derive.op.AbstractPatternOp.PatternOp;
import nars.derive.op.UnificationPrototype;
import nars.derive.op.UnifyOneSubterm;
import nars.derive.rule.PremiseRule;
import nars.derive.rule.PremiseRuleSet;
import nars.term.Term;
import nars.util.TermTrie;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

import java.io.PrintStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static org.eclipse.collections.impl.tuple.Tuples.pair;


/**
 * separates rules according to task/belief term type but otherwise involves significant redundancy we'll eliminate in other Deriver implementations
 */
public enum TrieDeriver {
    ;

    public static PrediTerm<Derivation> the(PremiseRuleSet r, NAR nar) {
        return the(r, nar, (x) -> x);
    }

    public static PrediTerm<Derivation> the(PremiseRuleSet r, NAR nar, Function<PrediTerm<Derivation>, PrediTerm<Derivation>> each) {

        final TermTrie<Term, TrieExecutor.Choice> trie = new RuleTrie(nar, r);

        List<PrediTerm<Derivation>> bb = subtree(trie.root);
        PrediTerm[] roots = bb.toArray(new PrediTerm[bb.size()]);

        PrediTerm<Derivation> tf = Fork.fork(roots).transform(each);

        return tf;
        //return new TrieExecutor(tf);
    }


//    public static TrieDeriver get(String individualRule) throws Narsese.NarseseException {
//        return get(new PremiseRuleSet(true, PremiseRule.rule(individualRule)));
//    }

    public static void print(Object p, @NotNull PrintStream out, int indent) {

        /*if (p instanceof IfThen) {

            IfThen it = (IfThen) p;

            TermTrie.indent(indent);
            out.println(Util.className(p) + " (");
            print(it.cond, out, indent + 2);

            TermTrie.indent(indent);
            out.println(") ==> {");

            print(it.conseq, out, indent + 2);
            TermTrie.indent(indent);
            out.println("}");

        } *//*else if (p instanceof If) {

            indent(indent); out.println(Util.className(p) + " {");
            {
                If it = (If) p;
                print(it.cond, out, indent + 2);
            }
            indent(indent); out.println("}");

        }  else */

        if (p instanceof AndCondition) {
            TermTrie.indent(indent);
            out.println("and {");
            AndCondition ac = (AndCondition) p;
            for (PrediTerm b : ac.cache) {
                print(b, out, indent + 2);
            }
            TermTrie.indent(indent);
            out.println("}");
        } else if (p instanceof Fork) {
            TermTrie.indent(indent);
            out.println(Util.className(p) + " {");
            Fork ac = (Fork) p;
            for (PrediTerm b : ac.cache) {
                print(b, out, indent + 2);
            }
            TermTrie.indent(indent);
            out.println("}");

        } else if (p instanceof OpSwitch) {
            OpSwitch sw = (OpSwitch) p;
            TermTrie.indent(indent);
            out.println("switch(op(" + (sw.subterm==0 ? "task" : "belief") + ")) {");
            int i = -1;
            for (PrediTerm b : sw.swtch) {
                i++;
                if (b == null) continue;

                TermTrie.indent(indent + 2);
                out.println('"' + Op.values()[i].toString() + "\": {");
                print(b, out, indent + 4);
                TermTrie.indent(indent + 2);
                out.println("}");

            }
            TermTrie.indent(indent);
            out.println("}");
        } else {

            if (p instanceof UnificationPrototype)
                p = ((UnificationPrototype) p).transform((x) -> x);

            TermTrie.indent(indent);
            out.print( /*Util.className(p) + ": " +*/ p);


            out.println();

        }


    }

    static void forEach(Object p, @NotNull Consumer out) {
        out.accept(p);

        /*if (p instanceof IfThen) {

            IfThen it = (IfThen) p;

            TermTrie.indent(indent);
            out.println(Util.className(p) + " (");
            print(it.cond, out, indent + 2);

            TermTrie.indent(indent);
            out.println(") ==> {");

            print(it.conseq, out, indent + 2);
            TermTrie.indent(indent);
            out.println("}");

        } *//*else if (p instanceof If) {

            indent(indent); out.println(Util.className(p) + " {");
            {
                If it = (If) p;
                print(it.cond, out, indent + 2);
            }
            indent(indent); out.println("}");

        }  else */
        if (p instanceof AndCondition) {
            //TermTrie.indent(indent);
            //out.println("and {");
            AndCondition ac = (AndCondition) p;
            for (PrediTerm b : ac.cache) {
                forEach(b, out);
            }
        } else if (p instanceof Fork) {
            //TermTrie.indent(indent);
            //out.println(Util.className(p) + " {");
            Fork ac = (Fork) p;
            for (PrediTerm b : ac.cache) {
                forEach(b, out);
            }
//            TermTrie.indent(indent);
//            out.println("}");

        } else if (p instanceof OpSwitch) {
            OpSwitch sw = (OpSwitch) p;
            //TermTrie.indent(indent);
            //out.println("SubTermOp" + sw.subterm + " {");
            for (PrediTerm b : sw.cases.values()) {
                if (b == null) continue;

                //TermTrie.indent(indent + 2);
                //out.println('"' + Op.values()[i].toString() + "\": {");
                //print(b, out, indent + 4);
                forEach(b, out);
                //TermTrie.indent(indent + 2);
                //out.println("}");

            }
//            TermTrie.indent(indent);
//            out.println("}");
        } else if (p instanceof UnifyOneSubterm.UnifySubtermThenConclude) {
            forEach(((UnifyOneSubterm.UnifySubtermThenConclude) p).eachMatch, out);
        } else {

            if (p instanceof UnificationPrototype)
                throw new UnsupportedOperationException();
            //((MatchTermPrototype) p).builder();

//            TermTrie.indent(indent);
//            out.println( /*Util.className(p) + ": " +*/ p);


        }


    }

    static void forEach(@Nullable PrediTerm in, PrediTerm x, @NotNull BiConsumer<PrediTerm, PrediTerm> out) {
        out.accept(in, x);

        /*if (p instanceof IfThen) {

            IfThen it = (IfThen) p;

            TermTrie.indent(indent);
            out.println(Util.className(p) + " (");
            print(it.cond, out, indent + 2);

            TermTrie.indent(indent);
            out.println(") ==> {");

            print(it.conseq, out, indent + 2);
            TermTrie.indent(indent);
            out.println("}");

        } *//*else if (p instanceof If) {

            indent(indent); out.println(Util.className(p) + " {");
            {
                If it = (If) p;
                print(it.cond, out, indent + 2);
            }
            indent(indent); out.println("}");

        }  else */
        if (x instanceof AndCondition) {
            //TermTrie.indent(indent);
            //out.println("and {");
            AndCondition ac = (AndCondition) x;
            for (PrediTerm y : ac.cache) {
                forEach(x, y, out);
            }
        } else if (x instanceof Fork) {
            //TermTrie.indent(indent);
            //out.println(Util.className(p) + " {");
            Fork ac = (Fork) x;
            for (PrediTerm y : ac.cache) {
                forEach(x, y, out);
            }
//            TermTrie.indent(indent);
//            out.println("}");

        } else if (x instanceof OpSwitch) {
            OpSwitch sw = (OpSwitch) x;
            //TermTrie.indent(indent);
            //out.println("SubTermOp" + sw.subterm + " {");
            for (PrediTerm y : sw.cases.values()) {
                if (y == null) continue;

                //TermTrie.indent(indent + 2);
                //out.println('"' + Op.values()[i].toString() + "\": {");
                //print(b, out, indent + 4);
                forEach(x, y, out);
                //TermTrie.indent(indent + 2);
                //out.println("}");

            }
//            TermTrie.indent(indent);
//            out.println("}");
        } else if (x instanceof UnifyOneSubterm.UnifySubtermThenConclude) {
            forEach(x, ((UnifyOneSubterm.UnifySubtermThenConclude) x).eachMatch, out);
        }
    }


    protected static List<PrediTerm<Derivation>> compileSwitch(List<PrediTerm<Derivation>> bb) {

        bb = factorSubOpToSwitch(bb, 0, 2);
        bb = factorSubOpToSwitch(bb, 1, 2);

        return bb;
    }


//    static class StackFrame {
//        private final int i;
//        int version;
//        BoolPred<Derivation> op;
//
//        public StackFrame(BoolPred<Derivation> op, int now, int i) {
//            this.version = now;
//            this.op = op;
//            this.i = i;
//        }
//
//        @Override
//        public String toString() {
//            return "StackFrame{" +
//                    "i=" + i +
//                    ", version=" + version +
//                    ", op=" + op.getClass() +
//                    '}';
//        }
//    }
//
//    /* one-method interpreter, doesnt recursive call but maintains its own stack */
//    @Override
//    public boolean test(@NotNull Derivation m) {
//
//        FasterList<StackFrame> stack = new FasterList(16);
//
//        BoolPred<Derivation> cur = this;
//        int i = 0;
//        //StackFrame x = new StackFrame(this, m.now(), 0);
//        BoolPred[] and = new BoolPred[]{this};
//
//        main:
//        do {
//
//            System.out.println(stack);
//
//
//            BoolPred y = and[i++];
//            if (y instanceof Fork) {
//                cur = y;
//                Fork f = (Fork) cur;
//                if (i == f.cached.length) {
//                    StackFrame pop = stack.removeLast();
//                    m.revert(pop.version);
//                    cur = pop.op;
//                    i = pop.i;
//                    continue;
//                } else if (i == 0) {
//                    stack.add(new StackFrame(f, m.now(), i)); //save
//                }
//                cur = f.cached[i++];
//            } else {
//                if (!y.test(m))
//                    break main;
//            }
//
//        } while (!stack.isEmpty());
//
//        return true;
//    }

    @NotNull
    private static List<PrediTerm<Derivation>> factorSubOpToSwitch(@NotNull List<PrediTerm<Derivation>> bb, int subterm, int minToCreateSwitch) {
        if (!bb.isEmpty()) {
            Map<PatternOp, PrediTerm<Derivation>> cases = $.newHashMap(8);
            List<PrediTerm<Derivation>> removed = $.newArrayList(); //in order to undo
            bb.removeIf(p -> {
                if (p instanceof AndCondition) {
                    AndCondition ac = (AndCondition) p;
                    if (ac.OR(x -> {
                        if (x instanceof PatternOp) {
                            PatternOp so = (PatternOp) x;
                            if (so.taskOrBelief == subterm) {
                                if (null == cases.putIfAbsent(so, ac.without(so))) {
                                    removed.add(p);
                                    return true;
                                }
                            }
                        }
                        return false;
                    }))
                        return true;

                }
                return false;
            });


            int numCases = cases.size();
            if (numCases >= minToCreateSwitch) {
                if (numCases != removed.size()) {
                    throw new RuntimeException("switch fault");
                }

                EnumMap<Op, PrediTerm<Derivation>> caseMap = new EnumMap(Op.class);
                cases.forEach((c, p) -> caseMap.put(Op.values()[c.opOrdinal], p));
                bb.add(new OpSwitch(subterm, caseMap));
            } else {
                bb.addAll(removed); //undo
            }
        }

        return bb;
    }

    @Nullable
    public static PrediTerm<Derivation> ifThen(@NotNull Stream<PrediTerm<Derivation>> cond, @Nullable PrediTerm<Derivation> conseq) {
        return AndCondition.the(AndCondition.compile(
                (conseq != null ? Stream.concat(cond, Stream.of(conseq)) : cond).collect(toList())
        ));
    }


    public static void print(PrediTerm<Derivation> d, @NotNull PrintStream out) {
        print(d, out, 0);

//        out.println("Fork {");
//
//        for (BoolPred p : roots())
//            print(p, out, 2);
//
//        out.println("}");
    }


    @NotNull
    static List<PrediTerm<Derivation>> subtree(@NotNull TrieNode<List<Term>, TrieExecutor.Choice> node) {


        List<PrediTerm<Derivation>> bb = $.newArrayList(node.childCount());

        node.forEach(n -> {

            List<PrediTerm<Derivation>> conseq = subtree(n);

            int nStart = n.start();
            int nEnd = n.end();
            PrediTerm<Derivation> branch = ifThen(
                    conditions(n.seq().stream().skip(nStart).limit(nEnd - nStart)),
                    !conseq.isEmpty() ? (PrediTerm<Derivation>) Fork.fork(conseq.toArray(new PrediTerm[conseq.size()])) : null
            );

            if (branch != null)
                bb.add(branch);
        });

        return compileSwitch(bb);
    }

//    public void recurse(@NotNull CauseEffect each) {
//        for (BoolCondition p : roots) {
//            recurse(null, p, each);
//        }
//    }

//        public interface CauseEffect extends BiConsumer<Term, Term> {
//
//        }

//    public static Term recurse(Term pred, Term curr, @NotNull CauseEffect each) {
//
//        each.accept(pred, curr);
//
//        /*if (curr instanceof IfThen) {
//
//            IfThen it = (IfThen) curr;
////            each.accept(/*recurse(curr, _/it.cond/_, each)-/, recurse(curr, it.conseq, each));
//
//        } else */
//        if (curr instanceof AndCondition) {
//
//            AndCondition ac = (AndCondition) curr;
//            Term p = curr;
//            for (BoolCondition b : ac.termCache) {
//                p = recurse(p, b, each);
//            }
//
//        } else if (curr instanceof Fork) {
//            Fork ac = (Fork) curr;
//            for (BoolCondition b : ac.termCache) {
//                recurse(curr, b, each);
//            }
//        } else if (curr instanceof PatternOpSwitch) {
//            PatternOpSwitch sw = (PatternOpSwitch) curr;
//            int i = -1;
//            for (BoolCondition b : sw.proc) {
//                i++;
//                if (b == null)
//                    continue;
//
//                //construct a virtual if/then branch to emulate the entire switch structure
//                recurse(curr,
//                        AndCondition.the(Lists.newArrayList(
//                                new PatternOp(sw.subterm, Op.values()[i]), b)),
//                        each);
//
//            }
//        }
//
//        return curr;
//    }

    @NotNull
    static Stream<PrediTerm<Derivation>> conditions(@NotNull Stream<Term> t) {

//
//            final AtomicReference<UnificationPrototype> unificationParent = new AtomicReference<>(null);
//
//            return t.filter(x -> {
//                if (x instanceof Conclude) {
//                    //link this derivation action to the previous Match,
//                    //allowing multiple derivations to fold within a Match's actions
//                    UnificationPrototype mt = unificationParent.getAndSet(null);
//                    if (mt == null) {
//                        throw new RuntimeException("detached Derive action: " + x + " in branch: " + t);
//                        //System.err.println("detached Derive action: " + x + " in branch: " + t);
//                    } else {
//                        mt.derive((Conclude) x);
//                    }
//                    return false;
//                } else if (x instanceof BoolPred) {
//                    if (x instanceof UnificationPrototype) {
//
//                        unificationParent.set((UnificationPrototype) x);
//                    }
//                }
//                return true;
        return t./*filter(x -> !(x instanceof Conclude)).*/map(x -> (PrediTerm<Derivation>) x);
    }


    //    @NotNull
    //    private static ProcTerm compileActions(@NotNull List<ProcTerm> t) {
    //
    //        switch (t.size()) {
    //            case 0: return null;
    //            case 1:
    //                return t.get(0);
    //            default:
    //                //optimization: find expression prefix types common to all, and see if a switch can be formed
    //
    //        }
    //
    //    }

    /**
     * derivation term graph, gathered for analysis
     */
    //public final HashMultimap<MatchTerm,Derive> derivationLinks = HashMultimap.create();

    static final class RuleTrie extends TermTrie<Term, TrieExecutor.Choice> {

        private final NAR nar;

        public RuleTrie(NAR nar, PremiseRuleSet r) {
            super();
            this.nar = nar;

            Map<Set<Term>, RoaringBitmap> pre = new HashMap<>();
            List<Pair<PrediTerm<Derivation>, PremiseRule>> conclusions = $.newArrayList(r.size()*4);


            ObjectIntHashMap<Term> preconditionCount = new ObjectIntHashMap(256);

            r.forEach(rule -> {

                assert (rule.POST != null) : "null POSTconditions:" + rule;

                for (PostCondition p : rule.POST) {

                    Pair<Set<Term>,PrediTerm<Derivation>> c = rule.conditions(p, this.nar);

                    c.getOne().forEach((k) -> preconditionCount.addToValue(k, 1));

                    int n = conclusions.size();
                    conclusions.add(pair(c.getTwo(), rule));

                    pre.computeIfAbsent(c.getOne(), (x)->new RoaringBitmap()).add(n);

                }
            });

//            System.out.println("PRECOND");
//            preconditionCount.keyValuesView().toSortedListBy((x)->x.getTwo()).forEach((x)->System.out.println(Texts.iPad(x.getTwo(),3) + "\t" + x.getOne() ));

            List<List<Term>> paths = $.newArrayList();
            pre.forEach((k,v) -> {
                FasterList<Term> path = new FasterList(k);
                path.sort((a, b) -> {

                    if (a.equals(b)) return 0;

                    int ac = preconditionCount.get(a);
                    int bc = preconditionCount.get(b);
                    if (ac > bc) return -1;
                    else if (ac < bc) return +1;
                    else return a.compareTo(b);
                });

                PrediTerm<Derivation>[] ll = StreamSupport.stream(v.spliterator(), false).map((i) -> conclusions.get(i).getOne()).toArray(PrediTerm[]::new);
                TrieExecutor.Choice cx = new TrieExecutor.Choice(ll);
                path.add(cx);
                put(path, cx);
            });

        }

        @Override
        protected void onMatch(Term existing, Term incoming) {
            if (existing instanceof UnificationPrototype) {
                //merge the set of conclusions where overlapping


                ((UnificationPrototype) existing).conclude.addAll(((UnificationPrototype) incoming).conclude);
                //((UnificationPrototype) incoming).conclude.addAll(((UnificationPrototype) existing).conclude);
                //incomingConcs.clear();
            }
        }


    }

    //    //TODO not complete
    //    protected void compile(@NotNull ProcTerm p) throws IOException, CannotCompileException, NotFoundException {
    //        StringBuilder s = new StringBuilder();
    //
    //        final String header = "public final static String wtf=" +
    //                '"' + this + ' ' + new Date() + "\"+\n" +
    //                "\"COPYRIGHT (C) OPENNARS. ALL RIGHTS RESERVED.\"+\n" +
    //                "\"THIS SOURCE CODE AND ITS GENERATOR IS PROTECTED BY THE AFFERO GENERAL PUBLIC LICENSE: https://gnu.org/licenses/agpl.html\"+\n" +
    //                "\"http://github.com/opennars/opennars\";\n";
    //
    //        //System.out.print(header);
    //        p.appendJavaProcedure(s);
    //
    //
    //        ClassPool pool = ClassPool.getDefault();
    //        pool.importPackage("nars.truth");
    //        pool.importPackage("nars.nal");
    //
    //        CtClass cc = pool.makeClass("nars.nal.CompiledDeriver");
    //        CtClass parent = pool.get("nars.nal.Deriver");
    //
    //        cc.addField(CtField.make(header, cc));
    //
    //        cc.setSuperclass(parent);
    //
    //        //cc.addConstructor(parent.getConstructors()[0]);
    //
    //        String initCode = "nars.Premise p = m.premise;";
    //
    //        String m = "public void run(nars.nal.PremiseMatch m) {\n" +
    //                '\t' + initCode + '\n' +
    //                '\t' + s + '\n' +
    //                '}';
    //
    //        System.out.println(m);
    //
    //
    //        cc.addMethod(CtNewMethod.make(m, cc));
    //        cc.writeFile("/tmp");
    //
    //        //System.out.println(cc.toBytecode());
    //        System.out.println(cc);
    //    }
    //

    //final static Logger logger = LoggerFactory.getLogger(TrieDeriver.class);


    //    final static void run(RuleMatch m, List<TaskRule> rules, int level, Consumer<Task> t) {
    //
    //        final int nr = rules.size();
    //        for (int i = 0; i < nr; i++) {
    //
    //            TaskRule r = rules.get(i);
    //            if (r.minNAL > level) continue;
    //
    //            PostCondition[] pc = m.run(r);
    //            if (pc != null) {
    //                for (PostCondition p : pc) {
    //                    if (p.minNAL > level) continue;
    //                    ArrayList<Task> Lx = m.apply(p);
    //                    if(Lx!=null) {
    //                        for (Task x : Lx) {
    //                            if (x != null)
    //                                t.accept(x);
    //                        }
    //                    }
    //                    /*else
    //                        System.out.println("Post exit: " + r + " on " + m.premise);*/
    //                }
    //            }
    //        }
    //    }


    //    static final class Return extends Atom implements ProcTerm {
    //
    //        public static final ProcTerm the = new Return();
    //
    //        private Return() {
    //            super("return");
    //        }
    //
    //
    //        @Override
    //        public void appendJavaProcedure(@NotNull StringBuilder s) {
    //            s.append("return;");
    //        }
    //
    //        @Override
    //        public void accept(PremiseEval versioneds) {
    //            System.out.println("why call this");
    //            //throw new UnsupportedOperationException("should not be invoked");
    //        }
    //
    //    }

    //    /** just evaluates a boolean condition HACK */
    //    static final class If extends GenericCompound implements ProcTerm {
    //
    //
    //        public final transient @NotNull BoolCondition cond;
    //
    //
    //        public If(@NotNull BoolCondition cond) {
    //            super(Op.IMPLICATION,
    //                    TermVector.the( cond, Return.the)
    //            );
    //
    //            this.cond = cond;
    //        }
    //
    //        @Override public void accept(@NotNull PremiseEval m) {
    //            final int stack = m.now();
    //            cond.booleanValueOf(m);
    //            m.revert(stack);
    //        }
    //
    //    }


}
