package nars.nal.derive;

import com.google.common.collect.Lists;
import nars.$;
import nars.Op;
import nars.nal.Deriver;
import nars.nal.meta.*;
import nars.nal.meta.op.AbstractPatternOp.PatternOp;
import nars.nal.meta.op.MatchTermPrototype;
import nars.nal.meta.Conclude;
import nars.nal.rule.PremiseRule;
import nars.nal.rule.PremiseRuleSet;
import nars.term.Term;
import nars.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.magnos.trie.TrieNode;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * separates rules according to task/belief term type but otherwise involves significant redundancy we'll eliminate in other Deriver implementations
 */
public class TrieDeriver extends Deriver {

    @NotNull
    public final ProcTerm[] roots;
    @Nullable
    public final TermTrie<Term, PremiseRule> trie;

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

            if (rule == null || rule.postconditions == null)
                throw new RuntimeException("Null rule");

            for (PostCondition result : rule.postconditions) {

                List<Term> c = rule.conditions(result);
                PremiseRule existing = trie.put(c, rule);

                if (existing != null)
                    throw new RuntimeException("Duplicate condition sequence:\n\t" + c + "\n\t" + existing);

//                    if (existing != null && s != existing && existing.equals(s)) {
//                        System.err.println("DUPL: " + existing);
//                        System.err.println("      " + existing.getSource());
//                        System.err.println("EXST: " + s.getSource());
//                        System.err.println();
//                    }
            }
        }
    }


    public TrieDeriver(String... rule) {
        this(new PremiseRuleSet(Lists.newArrayList(rule)));
    }

    public TrieDeriver(@NotNull PremiseRuleSet ruleset) {
        super(ruleset);

        //return Collections.unmodifiableList(premiseRules);
        this.trie = new TermPremiseRuleTermTrie(ruleset);

        @NotNull List<ProcTerm> bb = subtree(trie.trie.root);
        this.roots = bb.toArray(new ProcTerm[bb.size()]);

        for (int i = 0; i < roots.length; i++) {
            roots[i] = build(roots[i]);
        }

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

    @Override
    public final void run(@NotNull PremiseEval m) {
        int now = m.now();
        for (ProcTerm r : roots)
            r.accept(m, now);
    }

    /**
     * HACK warning: use of this singular matchParent tracker is not thread-safe. assumes branches will be processed in a linear, depth first order
     */
    @NotNull
    final transient AtomicReference<MatchTermPrototype> matchParent = new AtomicReference<>(null);

    @NotNull
    private List<ProcTerm> subtree(@NotNull TrieNode<List<Term>, PremiseRule> node) {

        List<ProcTerm> bb = $.newArrayList(node.childCount());

        node.forEach(n -> {

            ProcTerm branch = ifThen(
                    conditions(n.seq().subList(n.start(), n.end()), matchParent),
                    Fork.compile(subtree(n))
            );

            if (branch != null)
                bb.add(branch);
        });

        return optimize(bb);
    }

    protected static List<ProcTerm> optimize(List<ProcTerm> bb) {

        bb = factorSubOpToSwitch(bb, 0, 2);
        bb = factorSubOpToSwitch(bb, 1, 2);

        return bb;
    }

    @NotNull
    private static List<ProcTerm> factorSubOpToSwitch(@NotNull List<ProcTerm> bb, int subterm, int minToCreateSwitch) {
        Map<PatternOp, ProcTerm> cases = $.newHashMap(8);
        List<ProcTerm> removed = $.newArrayList(); //in order to undo
        bb.removeIf(p -> {
            if (p instanceof IfThen) {
                IfThen ii = (IfThen) p;
                BoolCondition cond = ii.cond;
                ProcTerm cnsq = ii.conseq;
                if (cond instanceof PatternOp) {
                    PatternOp so = (PatternOp) cond;
                    if (so.subterm == subterm) {
                        if (null == cases.putIfAbsent(so, cnsq)) {
                            removed.add(p);
                            return true;
                        }
                    }
                } else if (cond instanceof AndCondition) {
                    //TODO extract it
                    AndCondition ac = (AndCondition)cond;
                    if (ac.or(x -> {
                        if (x instanceof PatternOp) {
                            PatternOp so = (PatternOp)x;
                            if (so.subterm == subterm) {
                                if (null == cases.putIfAbsent(so, new IfThen(ac.without(so), cnsq) )) {
                                    removed.add(p);
                                    return true;
                                }
                            }
                        }
                        return false;
                    }))
                        return true;
                }
            }
            return false;
        });



        if (cases.size() > minToCreateSwitch) {
            if (cases.size()!=removed.size()) {
                throw new RuntimeException("switch fault");
            }
            bb.add(new PatternOpSwitch(subterm, cases));
        } else {
            bb.addAll(removed); //undo
        }

        return bb;
    }

    public void print(@NotNull PrintStream out) {
        out.println("Fork {");

        for (ProcTerm p : roots)
            print(p, out, 2);

        out.println("}");
    }

    /** final processing step before finalized usable form */
    private static ProcTerm build(ProcTerm p) {
        if (p instanceof IfThen) {
            IfThen it = (IfThen) p;
            return new IfThen( (BoolCondition)build(it.cond), build(it.conseq) ); //HACK wasteful
        } else if (p instanceof AndCondition) {
            AndCondition ac = (AndCondition) p;
            BoolCondition[] termCache = ac.termCache;
            for (int i = 0; i < termCache.length; i++) {
                BoolCondition b = termCache[i];
                termCache[i] = (BoolCondition) build(b);
            }
        } else if (p instanceof Fork) {
            Fork ac = (Fork) p;
            ProcTerm[] termCache = ac.termCache;
            for (int i = 0; i < termCache.length; i++) {
                ProcTerm b = termCache[i];
                termCache[i] = build(b);
            }

        } else if (p instanceof PatternOpSwitch) {
            PatternOpSwitch sw = (PatternOpSwitch) p;
            ProcTerm[] proc = sw.proc;
            for (int i = 0; i < proc.length; i++) {
                ProcTerm b = proc[i];
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

    public void recurse(@NotNull CauseEffect each) {
        for (ProcTerm p : roots) {
            recurse(null, p, each);
        }
    }

    public interface CauseEffect extends BiConsumer<Term,Term> {

    }

    public static Term recurse(Term pred, Term curr, @NotNull CauseEffect each) {

        each.accept(pred, curr);

        if (curr instanceof IfThen) {

            IfThen it = (IfThen) curr;
            each.accept(/*recurse(curr, */it.cond/*, each)*/, recurse(curr, it.conseq, each));

        } else if (curr instanceof AndCondition) {

            AndCondition ac = (AndCondition) curr;
            Term p = curr;
            for (BoolCondition b : ac.termCache) {
                p = recurse(p, b, each);
            }

        } else if (curr instanceof Fork) {
            Fork ac = (Fork) curr;
            for (ProcTerm b : ac.termCache) {
                recurse(curr, b, each);
            }
        } else if (curr instanceof PatternOpSwitch) {
            PatternOpSwitch sw = (PatternOpSwitch) curr;
            int i = -1;
            for (ProcTerm b : sw.proc) {
                i++;
                if (b == null)
                    continue;

                //construct a virtual if/then branch to emulate the entire switch structure
                recurse(curr,
                        new IfThen(
                            new PatternOp(sw.subterm, Op.values()[i]),
                            b), each);

            }
        }

        return curr;
    }

    public static void print(Object p, @NotNull PrintStream out, int indent) {

        if (p instanceof IfThen) {

            IfThen it = (IfThen) p;

            TermTrie.indent(indent);
            out.println(Util.className(p) + " (");
            print(it.cond, out, indent + 2);

            TermTrie.indent(indent);
            out.println(") ==> {");

            print(it.conseq, out, indent + 2);
            TermTrie.indent(indent);
            out.println("}");

        } /*else if (p instanceof If) {

            indent(indent); out.println(Util.className(p) + " {");
            {
                If it = (If) p;
                print(it.cond, out, indent + 2);
            }
            indent(indent); out.println("}");

        } */ else if (p instanceof AndCondition) {
            TermTrie.indent(indent);
            out.println("and {");
            AndCondition ac = (AndCondition) p;
            for (BoolCondition b : ac.termCache) {
                print(b, out, indent + 2);
            }
            TermTrie.indent(indent);
            out.println("}");
        } else if (p instanceof Fork) {
            TermTrie.indent(indent); out.println(Util.className(p) + " {");
            Fork ac = (Fork) p;
            for (ProcTerm b : ac.termCache) {
                print(b, out, indent + 2);
            }
            TermTrie.indent(indent); out.println("}");

        } else if (p instanceof PatternOpSwitch) {
            PatternOpSwitch sw = (PatternOpSwitch) p;
            TermTrie.indent(indent); out.println("SubTermOp" + sw.subterm + " {");
            int i = -1;
            for (ProcTerm b : sw.proc) {
                i++;
                if (b == null) continue;

                TermTrie.indent(indent+2); out.println( '"' + Op.values()[i].toString() + "\": {");
                print(b, out, indent + 4);
                TermTrie.indent(indent+2); out.println("}");

            }
            TermTrie.indent(indent); out.println("}");
        } else {

            if (p instanceof MatchTermPrototype)
                ((MatchTermPrototype) p).build();

            TermTrie.indent(indent);
            out.println( /*Util.className(p) + ": " +*/ p);

        }


    }


    @NotNull
    private static List<BoolCondition> conditions(@NotNull Collection<Term> t, @NotNull AtomicReference<MatchTermPrototype> matchParent) {

        return t.stream().filter(x -> {
            if (x instanceof BoolCondition) {
                if (x instanceof MatchTermPrototype) {
                    matchParent.set((MatchTermPrototype) x);
                }
                return true;
            }
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
            } else {
                throw new RuntimeException("not boolean condition" + x + " in branch: " + t + " (" + x.getClass() + ')');
                //System.out.println("\tnot boolean condition");
                //return false;
            }
        }).map(x -> (BoolCondition) x).collect(Collectors.toList());
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
    public static ProcTerm ifThen(@NotNull List<BoolCondition> cond, @Nullable ProcTerm conseq) {

        BoolCondition cc = AndCondition.the(cond);

        if (cc != null) {

            return conseq == null ? cc : new IfThen(cc, conseq);

        } else {
            /*if (conseq!=null)
                throw new RuntimeException();*/
            return conseq;
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
