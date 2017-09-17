package nars.op.mental;

import jcog.bag.Bag;
import jcog.list.FasterList;
import nars.Op;
import nars.concept.BaseConcept;
import nars.concept.Concept;
import nars.index.term.TermContext;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * the proxy concepts present a bidirectional facade between a referenced and an alias term (alias term can be just a serial # atom wrapped in a product).
 * <p>
 * it replaces the index entry for the referenced with itself and also adds itself so from its start it intercepts all references to itself or the aliased (abbreviated) term whether this occurrs at the top level or as a subterm in another term (or as a subterm in another abbreviation, etc..)
 * <p>
 * the index is usually a weakmap or equivalent in which abbreviations can be forgotten as well as any other concept.
 * <p>
 * seen from a superterm containing one, it appears as a simple volume=2 concept meanwhile it could be aliasing a concept much larger than it. common "phrase" concepts with a volume >> 2 are good candidates for abbreviation. but when printed, the default toString() method is proxied so it will automatically decompress on output (or other serialization).
 */
public final class AliasConcept extends BaseConcept {


    private final Collection<Termed> templates;

    public static class AliasAtom extends Atom {

        public final Term target;

        protected AliasAtom(@NotNull String id, Term target) {
            super(id);
            this.target = target;
        }

        @Override
        public Term evalSafe(TermContext index, int remain) {
            Term e = target.evalSafe(index, remain);
            if (e != target)
                return e; //if a dynamic result, return that
            else
                return this; //otherwise if constant, return this
        }


//        @Override
//        public boolean equals(Object u) {
//            return super.equals(u) || super.equals(target);
//        }

        @Override
        public boolean unify(@NotNull Term y, @NotNull Unify subst) {

            if (super.unify(y, subst))
                return true;

            Term target = this.target;
            if (y instanceof AliasAtom) {
                //try to unify with y's abbreviated
//                if (tt.unify( ((AliasConcept)y).abbr.term(), subst ))
//                    return true;

                //if this is constant (no variables) then all it needs is equality test
                return target.unify(((AliasAtom) y).target, subst);
            }

            //try to unify with 'y'
            return target.unify(y, subst);
        }

    }


    @NotNull
    public final Compound abbr;

    AliasConcept(@NotNull String abbreviation, Concept decompressed) {
        super(new AliasAtom(abbreviation, decompressed.term()),
                decompressed.beliefs(), decompressed.goals(), decompressed.questions(), decompressed.quests(),
                new Bag[]{decompressed.termlinks(), decompressed.tasklinks()});

        this.abbr = (Compound) decompressed.term();
        put(Abbreviation.class, abbr);

//            Term[] tl = ArrayUtils.add(abbreviated.templates().terms(), abbreviated.term());
//            if (additionalTerms.length > 0)
//                tl = ArrayUtils.addAll(tl, additionalTerms);
        this.templates =
                new FasterList(decompressed.templates()).with(term);

        //rewriteLinks(nar);
    }


    @Override
    public Collection<Termed> templates() {
        return templates;
    }

    //
//        /**
//         * rewrite termlinks and tasklinks which contain the abbreviated term...
//         * (but are not equal to since tasks can not have atom content)
//         * ...replacing it with this alias
//         */
//        private void rewriteLinks(@NotNull NAR nar) {
//            Term that = abbr.term();
//            termlinks().compute(existingLink -> {
//                Term x = existingLink.get();
//                Term y = nar.concepts.replace(x, that, this);
//                return (y != null && y != x && y != Term.False) ?
//                        termlinks().newLink(y, existingLink) :
//                        existingLink;
//            });
//            tasklinks().compute(existingLink -> {
//                Task xt = existingLink.get();
//                Term x = xt.term();
//
//                if (!x.equals(that) && !x.hasTemporal()) {
//                    Term y = $.terms.replace(x, that, this);
//                    if (y != x && y instanceof Compound) {
//                        Task yt = MutableTask.clone(xt, (Compound) y, nar);
//                        if (yt != null)
//                            return termlinks().newLink(yt, existingLink);
//                    }
//                }
//
//                return existingLink;
//
//            });
//        }


//        @Override
//        public boolean equals(Object u) {
//            return super.equals(u);
//        }


//        @Override
//        public final Activation process(@NotNull Task input, NAR nar) {
//            return abbr.process(input, nar);
//        }


//        @NotNull
//        @Override
//        public BeliefTable beliefs() {
//            return abbr.beliefs();
//        }
//
//        @NotNull
//        @Override
//        public BeliefTable goals() {
//            return abbr.goals();
//        }
//
//        @NotNull
//        @Override
//        public QuestionTable questions() {
//            return abbr.questions();
//        }
//
//        @NotNull
//        @Override
//        public QuestionTable quests() {
//            return abbr.quests();
//        }
}
