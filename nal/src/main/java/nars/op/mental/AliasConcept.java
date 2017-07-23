package nars.op.mental;

import com.google.common.io.ByteArrayDataOutput;
import nars.NAR;
import nars.Op;
import nars.concept.BaseConcept;
import nars.index.term.TermContext;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.container.TermContainer;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;

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

    public static class AliasAtom extends Atom {

        public final Term target;

        protected AliasAtom(@NotNull String id, Term target) {
            super(id);
            this.target = target;
        }

        @Override
        public int structure() {
            return target.structure() | Op.ATOM.bit;
        }

        @Override
        public Term eval(TermContext index) {
            return target;
        }

        @Override
        public void append(ByteArrayDataOutput out) {
            target.append(out); //serialize the expanded version
        }

        @Override
        public boolean equals(Object u) {
            return super.equals(u) || super.equals(target);
        }

        @Override
        public boolean unify(@NotNull Term y, @NotNull Unify subst) {

            Term tt = target;
            if (y instanceof AliasAtom) {
                //try to unify with y's abbreviated
//                if (tt.unify( ((AliasConcept)y).abbr.term(), subst ))
//                    return true;

                //if this is constant (no variables) then all it needs is equality test
                return tt.unify(((AliasAtom) y).target, subst);
            }

            //try to unify with 'y'
            return tt.unify(y, subst);
        }

    }


    @NotNull
    public final Compound abbr;

    @NotNull static public AliasConcept get(@NotNull String compressed, @NotNull Compound decompressed, @NotNull NAR nar) {
        AliasConcept a = new AliasConcept(compressed, decompressed, nar);
        return a;
    }

    AliasConcept(@NotNull String abbreviation, Compound abbr, @NotNull NAR nar) {
        super(new AliasAtom(abbreviation, abbr), null, null, nar.conceptBuilder);

        this.abbr = abbr;

//            Term[] tl = ArrayUtils.add(abbreviated.templates().terms(), abbreviated.term());
//            if (additionalTerms.length > 0)
//                tl = ArrayUtils.addAll(tl, additionalTerms);
//            this.templates = TermVector.the(tl);

        //rewriteLinks(nar);
    }

    @Override
    public TermContainer templates() {
        return abbr.subterms();
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
