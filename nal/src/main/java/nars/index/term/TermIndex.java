package nars.index.term;

import nars.*;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.conceptualize.ConceptBuilder;
import nars.derive.meta.match.EllipsisMatch;
import nars.index.TermBuilder;
import nars.premise.Derivation;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.compound.SerialCompound;
import nars.term.container.TermContainer;
import nars.term.subst.MapSubst;
import nars.term.subst.Subst;
import nars.term.transform.CompoundTransform;
import nars.term.transform.VariableNormalization;
import nars.term.util.InvalidTermException;
import nars.term.var.Variable;
import nars.util.task.InvalidTaskException;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.impl.factory.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static nars.Op.CONJ;
import static nars.term.Term.False;
import static nars.term.Termed.termOrNull;
import static nars.term.Terms.compoundOrNull;

/**
 *
 */
public abstract class TermIndex extends TermBuilder {


    private static final Logger logger = LoggerFactory.getLogger(TermIndex.class);
    protected NAR nar;


    /**
     * get if not absent
     */
    @Nullable
    public Termed get(@NotNull Term t) {
        return get(t, false);
    }

    @Nullable
    public abstract Termed get(@NotNull Term key, boolean createIfMissing);

    /**
     * sets or replaces the existing value, unless the existing value is a PermanentConcept it must not
     * be replaced with a non-Permanent concept
     */
    public abstract void set(@NotNull Term src, Termed target);

    public final void set(@NotNull Termed t) {
        set(t.term(), t);
    }


    abstract public void clear();

    abstract public void forEach(Consumer<? super Termed> c);


    /**
     * called when a concept has been modified, ie. to trigger persistence
     */
    public void commit(Concept c) {
        //by default does nothing
    }

    public void start(NAR nar) {
        this.nar = nar;
        conceptBuilder().start(nar);
    }

    /**
     * # of contained terms
     */
    public abstract int size();

    @NotNull
    abstract public ConceptBuilder conceptBuilder();


    /**
     * a string containing statistics of the index's current state
     */
    @NotNull
    public abstract String summary();

    public abstract void remove(@NotNull Term entry);


//    public final HijacKache<Compound, Term> normalizations =
//            new HijacKache<>(Param.NORMALIZATION_CACHE_SIZE, 4);
//    public final HijacKache<ProtoCompound, Term> terms =
//            new HijacKache<>(Param.TERM_CACHE_SIZE, 4);

//    final Function<? super ProtoCompound, ? extends Term> termizer = pc -> {
//
//        return theSafe(pc.op(), pc.dt(), pc.terms() );
//    };
//
//    private int volumeMax(Op op) {
//        if (nar!=null) {
//            return nar.termVolumeMax.intValue();
//        } else {
//            return Param.COMPOUND_VOLUME_MAX;
//        }
//    }

    @NotNull
    private final Term theSafe(@NotNull Op o, int dt, @NotNull Term[] u) {
        try {
            return super.the(o, dt, u);
            //return t == null ? False : t;
        } catch (@NotNull InvalidTermException | InvalidTaskException x) {
            if (Param.DEBUG_EXTRA) {
                logger.warn("{x} : {} {} {}", x, o, dt, u);
            }
        } catch (Throwable e) {
            logger.error("{x} : {} {} {}", e, o, dt, u);
        }
        return False; //place a False placeholder so that a repeat call will not have to discover this manually
    }


    /**
     * returns the resolved term according to the substitution
     */
    @Nullable
    public static Term transform(@NotNull Term src, @NotNull Subst f) {

        Term y = f.xy(src);
        if (y != null)
            return y; //an assigned substitution, whether a variable or other type of term

        Op op = src.op();
        switch (op) {
            case ATOM:
            case INT:
            case VAR_DEP:
            case VAR_INDEP:
            case VAR_QUERY:
                return src; //unassigned literal atom or non-pattern var
            case VAR_PATTERN:
                return null; //unassigned pattern variable
        }

        return transform((Compound)src, f);
    }

    public static @Nullable Term transform(@NotNull Compound src, @NotNull Subst f) {

        //no variables that could be substituted, so return this constant
        if (f instanceof Derivation && src.vars() + src.varPattern() == 0) //shortcut for premise evaluation matching
            return src;

        int len = src.size();
        List<Term> sub = $.newArrayList(len /* estimate */);

        boolean strict = f instanceof Derivation;

        boolean changed = false;

        Op op = src.op();

        //early prefilter for True/False subterms
        boolean filterTrueFalse = !(op.statement || op == CONJ);


        //use COMPOUND_VOLUME_MAX instead of trying for the nar's to provide construction head-room that can allow terms
        //to reduce and potentially meet the requirement
        int volLimit = Param.COMPOUND_VOLUME_MAX - 1; /* -1 for the wrapping compound contribution of +1 volume if succesful */
        int volSum = 0, volAt = 0, subAt = 0;
        for (int i = 0; i < len; i++) {
            Term t = ((Compound) src).term(i);
            Term u = transform(t, f);


            if (u instanceof EllipsisMatch) {

                ((EllipsisMatch) u).expand(op, sub);
                subAt = sub.size();

                for (; volAt < subAt; volAt++) {
                    Term st = sub.get(volAt);
                    if (filterTrueFalse && isTrueOrFalse(st)) return null;
                    volSum += st.volume();
                    if (volSum >= volLimit) {
                        return null;
                    } //HARD VOLUME LIMIT REACHED
                }

                changed = true;

            } else {

                if (u == null) {

                    if (strict) {
                        return null;
                    }

                    u = t; //keep value

                } else {
                    changed |= (u != t);
                }

                if (filterTrueFalse && isTrueOrFalse(u))
                    return null;
                volSum += u.volume();
                if (volSum >= volLimit) {
                    return null;
                } //HARD VOLUME LIMIT REACHED

                sub.add(u);

                subAt++;

            }


        }

        Term transformed;
        int ss = sub.size();
        if (!changed || (ss == len && ((Compound) src).equalTerms(sub)))
            transformed = (Compound) src;
        else {
            transformed =
                    //the(cop, crc.dt(), sub.toArray(new Term[ss]));
                    new SerialCompound(op, src.dt(), sub.toArray(new Term[ss]));
        }

//        //cache the result
//        if (transformed!=null) //TODO store false for 'null' result
//            f.cache(src, transformed);

        return transformed;
    }


//    @NotNull
//    public final Term the(@NotNull Compound csrc, @NotNull TermContainer newSubs) {
//        if (csrc.subterms().equals(newSubs)) {
//            return csrc;
//        } else {
//            return the(csrc.op(), csrc.dt(), newSubs.terms());
//        }
//    }

    @NotNull
    public final Term the(@NotNull Compound csrc, @NotNull Term[] args) {
        if (csrc.equalTerms(args))
            return csrc;
        return the(csrc.op(), csrc.dt(), args);
    }

    @NotNull
    public final Term the(@NotNull Compound csrc, int newDT) {
        if (csrc.dt() == newDT) //no change
            return csrc;

        return the(csrc.op(), newDT, csrc.terms());
    }

//    @Override
//    public final @NotNull Term the(@NotNull Op op, int dt, @NotNull Term[] args) throws InvalidTermException {
//
////        int totalVolume = 0;
////        for (Term x : u)
////            totalVolume += x.volume();
//
////        if (totalVolume > volumeMax(op))
////            throw new InvalidTermException(op, dt, u, "Too voluminous");
//
//        boolean cacheable =
//                //(totalVolume > 2)
//                        //&&
//                (op !=INH) || !(args[0].op() == PROD && args[1].op()==ATOM && get(args[1]) instanceof Functor) //prevents caching for potential transforming terms
//                ;
//
//        if (cacheable) {
//
//            return terms.computeIfAbsent(new ProtoCompound.RawProtoCompound(op, dt, args), termizer);
//
//        } else {
//            return super.the(op, dt, args);
//        }
//    }

//    @Deprecated
//    public final @NotNull Term the(@NotNull Op op, @NotNull Term... tt) {
//        return the(op, DTERNAL, tt); //call this implementation's, not super class's
//    }


    public void print(@NotNull PrintStream out) {
        forEach(out::println);
        out.println();
    }

    private Term _normalize(Compound src) {

        Term result;

        try {
            int vars = src.vars();
            int pVars = src.varPattern();
            int totalVars = vars + pVars;

            if (totalVars > 0) {
                result = transform(src,
                        (vars == 1 && pVars == 0) ?
                                VariableNormalization.singleVariableNormalization //special case for efficiency
                                :
                                new VariableNormalization(totalVars /* estimate */)
                );
            } else {
                result = src;
//                result =
//                        src.hasAll(Op.OpBits) ?
//                                transform(src, CompoundTransform.None) : //force subterm functor eval
//                                ((Term) src);
            }


        } catch (InvalidTermException e) {

            if (Param.DEBUG_EXTRA)
                logger.warn("normalize {} : {}", src, e);

            result = InvalidCompound;
        }

        return result;
    }


    @Nullable
    public final Term normalize(@NotNull Compound x) {

        if (x.isNormalized()) {
            return x;
        } else {
            //see if subterms need change

            Term y = _normalize(x);

            if (y instanceof Compound) {

                //if (c!=null) {
                //c = compoundOrNull($.unneg((Compound) c));
                ((Compound) y).setNormalized();
                //}
            }
            return y;
        }

    }



//    private boolean cacheNormalization(@NotNull Compound src) {
//        return false;
//    }


//    @Nullable
//    public Term the(@NotNull Compound src, @NotNull List<Term> newSubs) {
//        if (src.size() == newSubs.size() && src.equalTerms(newSubs) )
//            return src;
//        else
//            return the(src.op(), src.dt(), newSubs.toArray(new Term[newSubs.size()]));
//    }


    @NotNull
    public Term transform(@NotNull Compound src, @NotNull CompoundTransform t) {
        if (!t.testSuperTerm(src))
            return src;

        int dt = src.dt();
        TermContainer tc = transform(src, src, src.dt(), t);

        if (tc != src) {
            //construct new compound with same op and dt
            return the(src.op(), dt, tc);
        } else {
            return src; //((Term) src).eval(this);
        }
    }

    @NotNull
    private TermContainer transform(@NotNull TermContainer src, Compound superterm, int dt, @NotNull CompoundTransform t) {

        int modifications = 0;

        int s = src.size();

        Term[] target = new Term[s];

        Term[] xx = src.terms();
        for (int i = 0; i < s; i++) {

            Term x = xx[i], y;

            if (t.test(x)) {
                y = t.apply(superterm, x);
            } else if (x instanceof Compound) {
                y = transform((Compound) x, t); //recurse
            } else {
                y = null;
            }

//            if (y != null)
//                y = y.eval(this);

            //if (x != y) { //must be refernce equality test for some variable normalization cases
            if (y != null && !x.equals(y)) { //must be refernce equality test for some variable normalization cases
                modifications++;
            } else {
                y = x; //use original value
            }

            target[i] = y;
        }

        return modifications == 0 ? src : TermContainer.the(superterm.op(), dt, target);
    }


    @Nullable
    public Term transform(@NotNull Compound src, @NotNull ByteList path, @NotNull Term replacement) {
        return transform(src, path, 0, replacement);
    }

    @Nullable
    private Term transform(@NotNull Term src, @NotNull ByteList path, int depth, @NotNull Term replacement) {
        int ps = path.size();
        if (ps == depth)
            return replacement;
        if (ps < depth)
            throw new RuntimeException("path overflow");

        if (!(src instanceof Compound))
            return src; //path wont continue inside an atom

        int n = src.size();
        Compound csrc = (Compound) src;

        Term[] target = new Term[n];


        for (int i = 0; i < n; ) {
            Term x = csrc.term(i);
            Term y;
            if (path.get(depth) != i) {
                //unchanged subtree
                y = x;
            } else {
                //replacement is in this subtree
                y = transform(x, path, depth + 1, replacement);
            }

            target[i++] = y;
        }

        return the(csrc.op(), csrc.dt(), target);
    }


    @NotNull
    public Term parseRaw(@NotNull String termToParse) throws Narsese.NarseseException {
        return Narsese.the().term(termToParse, this, false);
    }

    @Nullable
    public <T extends Termed> T parse(@NotNull String termToParse) throws Narsese.NarseseException {
        return (T) (Narsese.the().term(termToParse, this, true));
    }

    /**
     * applies normalization and anonymization to resolve the term of the concept the input term maps t
     */
    @Nullable
    public final Concept concept(@NotNull Term possiblyConceptualizable, boolean createIfMissing) {

        Term term = conceptualizable(possiblyConceptualizable);
        if (term == null)
            return null;

        @Nullable Termed c = get(term, createIfMissing);
        if (!(c instanceof Concept)) {
            if (createIfMissing) {
                throw new Concept.InvalidConceptException(term, "Failed to build concept");
            }
            return null;
        }

        Concept cc = (Concept) c;
        if (cc.isDeleted()) {
            cc.state(conceptBuilder().init(), nar);
        }

        return cc;
    }

    @Nullable
    private Term conceptualizable(@NotNull Term term) {
        Term termPre = null;
        while (term instanceof Compound && termPre != term && term != null) {
//            //shouldnt need to check for this here
//            if (isTrueOrFalse(term))
//                throw new UnsupportedOperationException();

            termPre = term;

            switch (term.op()) {
                case VAR_DEP:
                case VAR_INDEP:
                case VAR_QUERY:
                case VAR_PATTERN:
                    //throw new InvalidConceptException((Compound)term, "variables can not be conceptualized");
                    return null;

                case NEG:
                    term = term.unneg(); //fallthru

                default:

                    if (term instanceof Compound) {

//                        if (Param.FILTER_CONCEPTS_WITHOUT_ATOMS) {
//                            if (!term.hasAny(ATOM.bit | Op.INT.bit))
//                                return null;
//                        } /*else {
//                            //only filter 0-length compounds, example: ()
//                            if (term.size() == 0)
//                                return null;
//                        }*/

                        term = normalize((Compound) term);
                    }

                    if (term instanceof Compound) {
                        term = Terms.atemporalize((Compound) term);
                    }

                    break;

            }
        }

        if ((term instanceof Variable) || (TermBuilder.isTrue(term)))
            return null;

        return term;
    }


    @Nullable
    public Term replace(@NotNull Term src, Map<Term, Term> m) {
        return termOrNull(transform(src, new MapSubst(m)));
    }

    @Nullable
    public Term replace(@NotNull Term src, Term from, Term to) {
        return replace(src, Maps.mutable.of(from, to));
    }


    /**
     * implementations can override this to update the index when a concept's state changes, ex: to re-evaluate it's features
     */
    public void onStateChanged(Concept c) {
        /* nothing */
    }


    protected void onRemove(Termed value) {
        if (value instanceof Concept) {
            if (value instanceof PermanentConcept) {
                //refuse deletion
                set(value.term(), value);
            } else {
                ((Concept) value).delete(nar);
            }
        }
    }

    public Term eval(Compound x) {

        //eval before normalizing
        Compound z = compoundOrNull(x.eval(this));
        if (z == null)
            return null;

        return normalize(z);
    }

    //    @Override
//    protected @NotNull Term statement(@NotNull Op op, int dt, @NotNull Term subject, @NotNull Term predicate) {
//        if ( op == INH && predicate instanceof Atom && !(predicate instanceof Concept) && transformImmediates() ) {
//            //resolve atomic statement predicates in inheritance, for inline term rewriting
//            Termed existingPredicate = get(predicate);
//            if (existingPredicate!=null)
//                predicate = existingPredicate.term();
//        }
//
//        return super.statement(op, dt, subject, predicate);
//    }
}
