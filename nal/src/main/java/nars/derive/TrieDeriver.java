package nars.derive;

import jcog.Util;
import jcog.trie.TrieNode;
import nars.$;
import nars.Narsese;
import nars.Op;
import nars.control.premise.Derivation;
import nars.derive.meta.*;
import nars.derive.meta.constraint.MatchConstraint;
import nars.derive.meta.op.AbstractPatternOp.PatternOp;
import nars.derive.meta.op.MatchOneSubtermPrototype;
import nars.derive.meta.op.MatchTermPrototype;
import nars.derive.rule.PremiseRule;
import nars.derive.rule.PremiseRuleSet;
import nars.term.Term;
import nars.util.TermTrie;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;


/**
 * separates rules according to task/belief term type but otherwise involves significant redundancy we'll eliminate in other Deriver implementations
 */
public class TrieDeriver implements Deriver {

    private BoolPred<Derivation> pred;


    /**
     * derivation term graph, gathered for analysis
     */
    //public final HashMultimap<MatchTerm,Derive> derivationLinks = HashMultimap.create();

    static final class TermPremiseRuleTermTrie extends TermTrie<Term, PremiseRule> {

        public TermPremiseRuleTermTrie(@NotNull PremiseRuleSet ruleset) {
            super(ruleset.rules);
        }

        @Override
        public void index(@Nullable PremiseRule rule) {

            if (rule == null || rule.POST == null)
                throw new RuntimeException("Null rule");

            for (PostCondition result : rule.POST) {

                List<Term> c = rule.conditions(result);

                PremiseRule existing = trie.put(c, rule);

                /*if (existing != null)
                    throw new RuntimeException("Duplicate condition sequence:\n\t" + c + "\n\t" + existing);*/

//                    if (existing != null && s != existing && existing.equals(s)) {
//                        System.err.println("DUPL: " + existing);
//                        System.err.println("      " + existing.getSource());
//                        System.err.println("EXST: " + s.getSource());
//                        System.err.println();
//                    }
            }
        }
    }


    public static TrieDeriver get(String individualRule) throws Narsese.NarseseException {
        return get(new PremiseRuleSet(true, PremiseRule.rule(individualRule)));
    }

    public TrieDeriver(BoolPred... root) {
        this.pred = Fork.compile(root);
    }

    @Override
    public final boolean test(Derivation d) {
        return pred.test(d);
    }

    public static TrieDeriver get(@NotNull PremiseRuleSet ruleset) {

        //return Collections.unmodifiableList(premiseRules);
        final TermTrie<Term, PremiseRule> trie = new TermPremiseRuleTermTrie(ruleset);

        Compiler c = new Compiler();

        @NotNull List<BoolPred> bb = c.subtree(trie.trie.root);
        BoolPred[] roots = bb.toArray(new BoolPred[bb.size()]);

        for (int i = 0; i < roots.length; i++) {
            roots[i] = c.build(roots[i]);
        }

        return new TrieDeriver(Fork.compile(roots));

        /*
        for (ProcTerm<PremiseMatch> p : roots) {
            try {
                compile(p);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        */
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
            for (BoolPred b : ac.termCache) {
                print(b, out, indent + 2);
            }
            TermTrie.indent(indent);
            out.println("}");
        } else if (p instanceof Fork) {
            TermTrie.indent(indent);
            out.println(Util.className(p) + " {");
            Fork ac = (Fork) p;
            for (BoolPred b : ac.cached) {
                print(b, out, indent + 2);
            }
            TermTrie.indent(indent);
            out.println("}");

        } else if (p instanceof PatternOpSwitch) {
            PatternOpSwitch sw = (PatternOpSwitch) p;
            TermTrie.indent(indent);
            out.println("SubTermOp" + sw.subterm + " {");
            int i = -1;
            for (BoolPred b : sw.proc) {
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

            if (p instanceof MatchTermPrototype)
                ((MatchTermPrototype) p).build();

            TermTrie.indent(indent);
            out.println( /*Util.className(p) + ": " +*/ p);

        }


    }

    public void print(@NotNull PrintStream out) {
        print(pred, out, 0);

//        out.println("Fork {");
//
//        for (BoolPred p : roots())
//            print(p, out, 2);
//
//        out.println("}");
    }

    static class Compiler {

        final AtomicReference<MatchTermPrototype> matchParent = new AtomicReference<>(null);

        @NotNull
        private List<BoolPred> subtree(@NotNull TrieNode<List<Term>, PremiseRule> node) {


            List<BoolPred> bb = $.newArrayList(node.childCount());

            node.forEach(n -> {

                BoolPred branch = ifThen(
                        conditions(n.seq().subList(n.start(), n.end())),
                        Fork.compile(subtree(n))
                );

                if (branch != null)
                    bb.add(branch);
            });

            return optimize(bb);
        }

        protected static List<BoolPred> optimize(List<BoolPred> bb) {

            bb = factorSubOpToSwitch(bb, 0, 2);
            bb = factorSubOpToSwitch(bb, 1, 2);

            return bb;
        }

        @NotNull
        private static List<BoolPred> factorSubOpToSwitch(@NotNull List<BoolPred> bb, int subterm, int minToCreateSwitch) {
            Map<PatternOp, BoolPred> cases = $.newHashMap(8);
            List<BoolPred> removed = $.newArrayList(); //in order to undo
            bb.removeIf(p -> {
                if (p instanceof AndCondition) {
                    AndCondition ac = (AndCondition) p;
                    if (ac.OR(x -> {
                        if (x instanceof PatternOp) {
                            PatternOp so = (PatternOp) x;
                            if (so.subterm == subterm) {
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


            if (cases.size() > minToCreateSwitch) {
                if (cases.size() != removed.size()) {
                    throw new RuntimeException("switch fault");
                }
                bb.add(new PatternOpSwitch(subterm, cases));
            } else {
                bb.addAll(removed); //undo
            }

            return bb;
        }


        /**
         * final processing step before finalized usable form
         */
        private static BoolPred build(BoolPred p) {
        /*if (p instanceof IfThen) {
            IfThen it = (IfThen) p;
            return new IfThen(build(it.cond), build(it.conseq) ); //HACK wasteful
        } else */
            if (p instanceof AndCondition) {
                AndCondition ac = (AndCondition) p;
                BoolPred[] termCache = ac.termCache;
                for (int i = 0; i < termCache.length; i++) {
                    BoolPred b = termCache[i];
                    termCache[i] = build(b);
                }
            } else if (p instanceof Fork) {
                Fork ac = (Fork) p;
                BoolPred[] termCache = ac.cached;
                for (int i = 0; i < termCache.length; i++) {
                    BoolPred b = termCache[i];
                    termCache[i] = build(b);
                }

            } else if (p instanceof PatternOpSwitch) {
                PatternOpSwitch sw = (PatternOpSwitch) p;
                BoolPred[] proc = sw.proc;
                for (int i = 0; i < proc.length; i++) {
                    BoolPred b = proc[i];
                    if (b != null)
                        proc[i] = build(b);
                    //else {
                    //continue
                    //}
                }
            } else {

                if (p instanceof MatchTermPrototype)
                    return ((MatchTermPrototype) p).build();

            }

            return p;
        }

//    public void recurse(@NotNull CauseEffect each) {
//        for (BoolCondition p : roots) {
//            recurse(null, p, each);
//        }
//    }

        public interface CauseEffect extends BiConsumer<Term, Term> {

        }

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
        private List<BoolPred> conditions(@NotNull Collection<Term> t) {

            return t.stream().filter(x -> {
                if (x instanceof Conclude) {
                    //link this derivation action to the previous Match,
                    //allowing multiple derivations to fold within a Match's actions
                    MatchTermPrototype mt = matchParent.get();
                    if (mt == null) {
                        throw new RuntimeException("detached Derive action: " + x + " in branch: " + t);
                        //System.err.println("detached Derive action: " + x + " in branch: " + t);
                    } else {
                        //HACK
                        Conclude dx = (Conclude) x;
                        mt.derive(dx);
                        //derivationLinks.put(mt, dx);
                    }
                    return false;
                } else if (x instanceof BoolPred) {
                    if (x instanceof MatchTermPrototype) {
                        matchParent.set((MatchTermPrototype) x);
                    }
                    //return true;
                }
                return true;
            }).map(x -> (BoolPred) x).collect(Collectors.toList());
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


        @Nullable
        public static BoolPred ifThen(@NotNull List<BoolPred> cond, @Nullable BoolPred conseq) {
            //
            //        BoolCondition cc = AndCondition.the(cond);
            //
            //        if (cc != null) {


            //return conseq == null ? cc : new IfThen(cc, conseq);
            List<BoolPred> ccc = $.newArrayList(conseq != null ? conseq.size() : 0 + 1);
            ccc.addAll(cond);
            if (conseq != null)
                ccc.add(conseq);
            return AndCondition.the(compileAnd(ccc));
            //
            //        } else {
            //            /*if (conseq!=null)
            //                throw new RuntimeException();*/
            //            return conseq;
            //        }
        }

        /**
         * combine certain types of items in an AND expression
         */
        static List<BoolPred> compileAnd(List<BoolPred> ccc) {
            if (ccc.size() == 1)
                return ccc;

            SortedSet<MatchConstraint> constraints = new TreeSet<MatchConstraint>((a,b) -> {
                if (a.equals(b)) return 0;
                int i = Integer.compare(a.cost(), b.cost());
                return i == 0 ? a.compareTo(b) : i;
            });
            Iterator<BoolPred> il = ccc.iterator();
            while (il.hasNext()) {
                BoolPred c = il.next();
                if (c instanceof MatchConstraint) {
                    constraints.add((MatchConstraint) c);
                    il.remove();
                }
            }


            if (!constraints.isEmpty()) {


                int iMatchTerm = -1; //first index of a MatchTerm op, if any
                for (int j = 0, cccSize = ccc.size(); j < cccSize; j++) {
                    BoolPred c = ccc.get(j);
                    if ((c instanceof MatchOneSubtermPrototype || c instanceof Fork) && iMatchTerm == -1) {
                        iMatchTerm = j;
                    }
                }
                if (iMatchTerm == -1)
                    iMatchTerm = ccc.size();

                //1. sort the constraints and add them at the end
                int c = constraints.size();
                if (c > 1) {
                    ccc.add(iMatchTerm, new MatchConstraint.CompoundConstraint(constraints.toArray(new MatchConstraint[c])));
                } else
                    ccc.add(iMatchTerm, constraints.iterator().next()); //just add the singleton at the end
            }

            return ccc;
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

}
