package nars.index.term;

import jcog.map.nbhm.HijacKache;
import nars.*;
import nars.concept.Concept;
import nars.concept.util.ConceptBuilder;
import nars.concept.util.InvalidConceptException;
import nars.index.TermBuilder;
import nars.nal.meta.Derivation;
import nars.nal.meta.match.EllipsisMatch;
import nars.task.util.InvalidTaskException;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.term.compound.ProtoCompound;
import nars.term.container.TermContainer;
import nars.term.subst.MapSubst;
import nars.term.subst.Subst;
import nars.term.transform.CompoundTransform;
import nars.term.transform.TermTransform;
import nars.term.transform.VariableNormalization;
import nars.term.util.InvalidTermException;
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
import java.util.function.Function;

import static nars.Op.INH;
import static nars.Op.PROD;
import static nars.term.Term.False;
import static nars.term.Termed.termOrNull;

/**
 *
 */
public abstract class TermIndex extends TermBuilder {


    public static final Logger logger = LoggerFactory.getLogger(TermIndex.class);
    protected NAR nar;


    /**
     * get if not absent
     */
    @Nullable
    public final Termed get(@NotNull Term t) {
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


    /** called when a concept has been modified, ie. to trigger persistence */
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


    public final HijacKache<Compound, Compound> normalizations =
            new HijacKache<>(Param.NORMALIZATION_CACHE_SIZE, 4);
    public final HijacKache<ProtoCompound, Term> terms =
            new HijacKache<>(Param.TERM_CACHE_SIZE, 4);

//    final ThreadLocal<Map<Compound,Compound>> normalizations =
//            ThreadLocal.withInitial( () ->
//                new CapacityLinkedHashMap(Param.NORMALIZATION_CACHE_SIZE_PER_THREAD)
//            );
//            //Collections.synchronizedMap( new CapacityLinkedHashMap(16*1024) );
    //final Cache<Compound,Compound> normalizations = Caffeine.newBuilder().maximumSize(Param.NORMALIZATION_CACHE_SIZE).build();


    final Function<? super ProtoCompound, ? extends Term> termizer = pc -> {

        return theSafe(pc.op(), pc.dt(), pc.terms() );
    };

    private int volumeMax(Op op) {
        if (nar!=null) {
            return nar.termVolumeMax.intValue();
        } else {
            return Param.COMPOUND_VOLUME_MAX;
        }
    }

    @NotNull private final Term theSafe(@NotNull Op o, int dt, @NotNull Term[] u) {
        try {
            return super.the(o, dt, u);
            //return t == null ? False : t;
        } catch (@NotNull InvalidTermException | InvalidTaskException x) {
            if (Param.DEBUG_EXTRA) {
                logger.warn("{x} : {} {} {}", x, o, dt, u);
            }
        } catch (Throwable e) {
            logger.error("{x} : {} {} {}",  e, o, dt, u);
        }
        return False; //place a False placeholder so that a repeat call will not have to discover this manually
    }






    /**
     * returns the resolved term according to the substitution
     */
    @Nullable
    public Term transform(@NotNull Term src, @NotNull Subst f) {


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



        //no variables that could be substituted, so return this constant
        if (f instanceof Derivation && src.vars() + src.varPattern() == 0) //shortcut for premise evaluation matching
            return src;

        int len = src.size();
        List<Term> sub = $.newArrayList(len /* estimate */);

        boolean strict = f instanceof Derivation;

        boolean changed = false;
        Compound crc = (Compound) src;


        //use COMPOUND_VOLUME_MAX instead of trying for the nar's to provide construction head-room that can allow terms
        //to reduce and potentially meet the requirement
        int volLimit = Param.COMPOUND_VOLUME_MAX-1; /* -1 for the wrapping compound contribution of +1 volume if succesful */
        int volSum = 0, volAt = 0, subAt = 0;
        for (int i = 0; i < len; i++) {
            Term t = crc.term(i);
            Term u = transform(t, f);


            if (u instanceof EllipsisMatch) {

                ((EllipsisMatch) u).expand(op, sub);
                subAt = sub.size();
                changed = true;

            } else {

                if (u == null) {

                    if (strict) { return null; }

                    sub.add(t); //keep value

                } else {
                    sub.add(u);
                    changed |= (u != t);
                }

                subAt++;

            }

            for (; volAt < subAt; volAt++) {
                volSum+=sub.get(volAt).volume();
                if (volSum > volLimit) { return null; } //HARD VOLUME LIMIT REACHED
            }
        }

        Term transformed;
        int ss = sub.size();
        if (!changed || (ss==len && crc.equalTerms(sub)))
            transformed = crc;
        else {
            transformed = the(crc.op(), crc.dt(), sub.toArray(new Term[ss]));
        }

//        //cache the result
//        if (transformed!=null) //TODO store false for 'null' result
//            f.cache(src, transformed);

        return transformed;
    }


    @NotNull
    public final Term the(@NotNull Compound csrc, @NotNull TermContainer newSubs) {
        if (csrc.subterms().equals(newSubs)) {
            return csrc;
        } else {
            return the(csrc.op(), csrc.dt(), newSubs.terms());
        }
    }

    @NotNull
    public final Term the(@NotNull Compound csrc, @NotNull Term[] args) {
        if (csrc.equalTerms(args))
            return csrc;
        return the(csrc.op(), csrc.dt(), args);
    }

    @NotNull
    public final Term the(@NotNull Compound csrc, int newDT) {
        if (csrc.dt()==newDT) //no change
            return csrc;

        return the(csrc.op(), newDT, csrc.terms());
    }

    @Override
    public final @NotNull Term the(@NotNull Op op, int dt, @NotNull Term[] args) throws InvalidTermException {

//        int totalVolume = 0;
//        for (Term x : u)
//            totalVolume += x.volume();

//        if (totalVolume > volumeMax(op))
//            throw new InvalidTermException(op, dt, u, "Too voluminous");

        boolean cacheable =
                //(totalVolume > 2)
                        //&&
                ((op !=INH) || !(args[1] instanceof TermTransform && args[0].op() == PROD)) //prevents caching for potential transforming terms
                ;

        if (cacheable) {

            return terms.computeIfAbsent(new ProtoCompound.RawProtoCompound(op, dt, args), termizer);

        } else {
            return super.the(op, dt, args);
        }
    }

//    @Deprecated
//    public final @NotNull Term the(@NotNull Op op, @NotNull Term... tt) {
//        return the(op, DTERNAL, tt); //call this implementation's, not super class's
//    }


    public void print(@NotNull PrintStream out) {
        forEach(out::println);
        out.println();
    }

    final Function<? super Compound, ? extends Compound> normalizer = src -> {

        Compound result;

        try {
            int numVars = src.vars();

            Term r = transform(src,
                            (numVars == 1 && src.varPattern() == 0) ?
                                    VariableNormalization.singleVariableNormalization :
                                    new VariableNormalization(numVars /* estimate */)
                    );

            result = !(r instanceof Compound) ? InvalidCompound : (Compound) r;

        } catch (InvalidTermException e) {

            if (Param.DEBUG_EXTRA)
                logger.warn("normalize {} : {}", src, e);

            result = InvalidCompound;
        }

        return result;
    };

    @NotNull
    public final Term internCompound(Compound uninterned) {
        return the(uninterned.op(), uninterned.dt(), uninterned.terms());
    }

    @Nullable
    public final Compound normalize(@NotNull Compound src) {


        if (src.isNormalized()) {
            return src;
        } else {
            //see if subterms need change
            Compound tgt = normalizations.computeIfAbsent(src, normalizer);
            if (tgt == InvalidCompound)
                return null;

            //if (c!=null) {
            //c = compoundOrNull($.unneg((Compound) c));
            ((Compound) tgt).setNormalized();
            //}
            return tgt;
        }

    }


    @Nullable
    public Term the(@NotNull Compound src, @NotNull List<Term> newSubs) {
        if (src.size() == newSubs.size() && src.equalTerms(newSubs) )
            return src;
        else
            return the(src.op(), src.dt(), newSubs.toArray(new Term[newSubs.size()]));
    }


    @NotNull
    public Term transform(@NotNull Compound src, @NotNull CompoundTransform t) {
        if (!t.testSuperTerm(src))
            return internCompound(src);

        TermContainer tc = transform(src, src, t);

        if (tc != src) {
            //construct new compound with same op and dt
            return the(src.op(), src.dt(), tc);
        } else {
            return src; //unmodified
        }
    }

    @NotNull
    public TermContainer transform(@NotNull TermContainer src, Compound superterm, @NotNull CompoundTransform t) {

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

            //if (x != y) { //must be refernce equality test for some variable normalization cases
            if (y!=null && !x.equals(y)) { //must be refernce equality test for some variable normalization cases
                modifications++;
            } else {
                y = x; //use original value
            }

            target[i] = y;
        }

        return modifications == 0 ? src : TermContainer.the(superterm.op(), target);
    }


    @Nullable
    public Term transform(@NotNull Compound src, @NotNull ByteList path, @NotNull Term replacement) {
        return transform(src, path, 0, replacement);
    }

    @Nullable
    public Term transform(@NotNull Term src, @NotNull ByteList path, int depth, @NotNull Term replacement) {
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


        for (int i = 0; i < n;) {
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
        return (T) /*the*/(Narsese.the().term(termToParse, this, true));
    }

    /**
     * applies normalization and anonymization to resolve the term of the concept the input term maps t
     */
    @Nullable
    public final Concept concept(@NotNull Term term, boolean createIfMissing) {

        term = conceptualizable(term, false);
        if (term == null)
            return null;

        @Nullable Termed c = get(term, createIfMissing);
        if (!(c instanceof Concept)) {
            if (createIfMissing) {
                throw new InvalidConceptException(term, "Failed to build concept");
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
    public Term conceptualizable(@NotNull Term term, boolean forTermlink) {
        Term termPre;
        do {
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

                        if (!forTermlink && Param.FILTER_CONCEPTS_WITHOUT_ATOMS) {
                            if (!term.hasAny(Op.ATOM.bit | Op.INT.bit))
                                return null;
                        } else {
                            //only filter 0-length compounds, example: ()
                            if (term.size() == 0)
                                return null;
                        }

                        term = normalize((Compound) term);
                    }

                    if (term instanceof Compound) {
                        term = Terms.atemporalize((Compound) term);
                    }

                    break;

            }
        } while (termPre != term && term != null);

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
    public void onPolicyChanged(Concept c) {
        /* nothing */
    }


    static boolean isDeletable(@NotNull Concept c) {
        return c.get(Concept.Savior.class) == null;
    }

    /**
     * attempts to delete a concept
     */
    protected void delete(@NotNull Concept value, @NotNull NAR nar) {
        if (isDeletable(value))
            value.delete(nar);
    }

    @Override
    protected @NotNull Term statement(@NotNull Op op, int dt, @NotNull Term subject, @NotNull Term predicate) {
        if ( op == INH && predicate instanceof Atom && !(predicate instanceof Concept) && transformImmediates() ) {
            //resolve atomic statement predicates in inheritance, for inline term rewriting
            Termed existingPredicate = get(predicate);
            if (existingPredicate!=null)
                predicate = existingPredicate.term();
        }

        return super.statement(op, dt, subject, predicate);
    }
}
