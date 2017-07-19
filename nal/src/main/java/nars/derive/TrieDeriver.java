package nars.derive;

import jcog.Util;
import jcog.trie.TrieNode;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.control.premise.Derivation;
import nars.derive.meta.*;
import nars.derive.meta.op.AbstractPatternOp.PatternOp;
import nars.derive.meta.op.MatchTerm;
import nars.derive.meta.op.UnificationPrototype;
import nars.derive.rule.PremiseRule;
import nars.derive.rule.PremiseRuleSet;
import nars.term.Term;
import nars.util.TermTrie;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;


/**
 * separates rules according to task/belief term type but otherwise involves significant redundancy we'll eliminate in other Deriver implementations
 */
public enum TrieDeriver  { ;

    public static PrediTerm<Derivation> the(PremiseRuleSet r, NAR nar) {
        return the(r, nar, (x)->x);
    }

    public static PrediTerm<Derivation> the(PremiseRuleSet r, NAR nar, Function<PrediTerm<Derivation>,PrediTerm<Derivation>> each) {

        //return Collections.unmodifiableList(premiseRules);
        final TermTrie<Term, PremiseRule> trie = new RuleTrie();
        r.rules.forEach(trie::put);

        @NotNull List<PrediTerm> bb = subtree(trie.root);
        PrediTerm[] roots = bb.toArray(new PrediTerm[bb.size()]);

        for (int i = 0; i < roots.length; i++)
            roots[i] = build(roots[i], each);

        @Nullable PrediTerm deriver = Fork.fork(roots);

        if (nar!=null) {
            forEachConclude(deriver, x -> {
                if (x.cause == -1)
                    x.setCause(nar.newCause(x).id);
            });
        }

        return deriver;

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
            for (PrediTerm b : ac.cached) {
                print(b, out, indent + 2);
            }
            TermTrie.indent(indent);
            out.println("}");

        } else if (p instanceof PatternOpSwitch) {
            PatternOpSwitch sw = (PatternOpSwitch) p;
            TermTrie.indent(indent);
            out.println("SubTermOp" + sw.subterm + " {");
            int i = -1;
            for (PrediTerm b : sw.proc) {
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
                p = ((UnificationPrototype) p).build();

            TermTrie.indent(indent);
            out.println( /*Util.className(p) + ": " +*/ p);

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
            for (PrediTerm b : ac.cached) {
                forEach(b, out);
            }
//            TermTrie.indent(indent);
//            out.println("}");

        } else if (p instanceof PatternOpSwitch) {
            PatternOpSwitch sw = (PatternOpSwitch) p;
            //TermTrie.indent(indent);
            //out.println("SubTermOp" + sw.subterm + " {");
            int i = -1;
            for (PrediTerm b : sw.proc) {
                i++;
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
        } else if (p instanceof MatchTerm) {
            forEach(((MatchTerm) p).eachMatch, out);
        } else {

            if (p instanceof UnificationPrototype)
                throw new UnsupportedOperationException();
            //((MatchTermPrototype) p).build();

//            TermTrie.indent(indent);
//            out.println( /*Util.className(p) + ": " +*/ p);


        }


    }

    protected static List<PrediTerm> compileSwitch(List<PrediTerm> bb) {

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
    private static List<PrediTerm> factorSubOpToSwitch(@NotNull List<PrediTerm> bb, int subterm, int minToCreateSwitch) {
        if (!bb.isEmpty()) {
            Map<PatternOp, PrediTerm> cases = $.newHashMap(8);
            List<PrediTerm> removed = $.newArrayList(); //in order to undo
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


            int numCases = cases.size();
            if (numCases > minToCreateSwitch) {
                if (numCases != removed.size()) {
                    throw new RuntimeException("switch fault");
                }
                bb.add(new PatternOpSwitch(subterm, cases));
            } else {
                bb.addAll(removed); //undo
            }
        }

        return bb;
    }

    /**
     * final processing step before finalized usable form
     */
    protected static PrediTerm build(PrediTerm p, Function<PrediTerm<Derivation>,PrediTerm<Derivation>> each) {
        /*if (p instanceof IfThen) {
            IfThen it = (IfThen) p;
            return new IfThen(build(it.cond), build(it.conseq) ); //HACK wasteful
        } else */
        if (p instanceof AndCondition) {
            AndCondition ac = (AndCondition) p;
            PrediTerm[] termCache = ac.cache;
            for (int i = 0; i < termCache.length; i++) {
                termCache[i] = build(termCache[i], each);
            }
        } else if (p instanceof Fork) {
            Fork ac = (Fork) p;
            PrediTerm[] termCache = ac.cached;
            for (int i = 0; i < termCache.length; i++) {
                termCache[i] = build(termCache[i], each);
            }

        } else if (p instanceof PatternOpSwitch) {
            PatternOpSwitch sw = (PatternOpSwitch) p;
            PrediTerm[] proc = sw.proc;
            for (int i = 0; i < proc.length; i++) {
                PrediTerm b = proc[i];
                if (b != null)
                    proc[i] = build(b, each);
                //else {
                //continue
                //}
            }
        } else if (p instanceof UnificationPrototype) {
            p = ((UnificationPrototype) p).build();
        }

        return each.apply(p);
    }

    @Nullable
    public static PrediTerm ifThen(@NotNull Stream<PrediTerm> cond, @Nullable PrediTerm conseq) {
        return AndCondition.the(AndCondition.compile(
                (conseq != null ? Stream.concat(cond, Stream.of(conseq)) : cond).collect(toList())
        ));
    }

    public static void forEachConclude(PrediTerm<Derivation> d, Consumer<Conclude> p) {
        forEach(d, (c) -> {
            if (c instanceof Conclude) {
                p.accept((Conclude) c);
            }
        });
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
    static List<PrediTerm> subtree(@NotNull TrieNode<List<Term>, PremiseRule> node) {


        List<PrediTerm> bb = $.newArrayList(node.childCount());

        node.forEach(n -> {

            List<PrediTerm> conseq = subtree(n);

            int nStart = n.start();
            int nEnd = n.end();
            PrediTerm branch = ifThen(
                    conditions(n.seq().stream().skip(nStart).limit(nEnd - nStart)),
                    !conseq.isEmpty() ? Fork.compile(conseq) : null
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
    static Stream<PrediTerm> conditions(@NotNull Stream<Term> t) {
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
        return t./*filter(x -> !(x instanceof Conclude)).*/map(x -> (PrediTerm) x);
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

    static final class RuleTrie extends TermTrie<Term, PremiseRule> {

        public RuleTrie() {
            super();
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

        @Override
        public void put(@Nullable PremiseRule rule) {

            assert (rule != null && rule.POST != null) : "Null rule:" + rule;

            for (PostCondition p : rule.POST) {

                List<Term> c = rule.conditions(p);

                PremiseRule existing = put(c, rule);

//                if (existing != null) {
//                    throw new RuntimeException("Duplicate condition sequence:\n\t" + c + "\n\t" + existing);
//                }
//                    if (existing != null && s != existing && existing.equals(s)) {
//                        System.err.println("DUPL: " + existing);
//                        System.err.println("      " + existing.getSource());
//                        System.err.println("EXST: " + s.getSource());
//                        System.err.println();
//                    }
//                }
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
