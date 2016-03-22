package nars.nar;

import nars.Global;
import nars.NAR;
import nars.concept.Concept;
import nars.concept.DefaultConceptBuilder;
import nars.nal.Deriver;
import nars.nal.meta.PremiseRule;
import nars.nal.nal8.AbstractOperator;
import nars.nal.nal8.operator.TermFunction;
import nars.op.data.*;
import nars.op.math.add;
import nars.op.math.length;
import nars.op.mental.*;
import nars.op.out.echo;
import nars.op.out.say;
import nars.op.sys.js;
import nars.op.sys.reset;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermIndex;
import nars.term.atom.Atom;
import nars.term.container.TermContainer;
import nars.term.index.MapIndex2;
import nars.time.Clock;
import nars.util.data.map.UnifriedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Default set of NAR parameters which have been classically used for development.
 * <p>
 * WARNING this Seed is not immutable yet because it extends Param,
 * which is supposed to be per-instance/mutable. So do not attempt
 * to create multiple NAR with the same Default seed model
 */
public abstract class AbstractNAR extends NAR {


    public AbstractNAR(Clock clock, TermIndex index, Random random) {
        this(clock, index, random, Global.DEFAULT_SELF);
    }

    public AbstractNAR(Clock clock, TermIndex index, Random rng, @NotNull Atom self) {
        super(clock, index, rng, self);

        initDefaults();

    }

    protected void initHigherNAL() {
        if (nal() >= 7) {
            initNAL7();
            if(nal() >=8) {
                initNAL8();
//                if (nal() >= 9) {
//                    initNAL9();
//                }
            }
        }
    }

    public void initNAL7() {
        //NAL7 plugins
        the(new STMTemporalLinkage(this));
    }

    public void initNAL8() {
        /** derivation operators available at runtime */
        for (Class<? extends TermFunction> c : PremiseRule.Operators) {
            try {
                onExec(c.newInstance());
            } catch (Exception e) {
                error(e);
            }
        }

        //new shell(this);
        for (AbstractOperator o : defaultOperators)
            onExec(o);


//        for (AbstractOperator o : exampleOperators)
//            onExec(o);
    }

    @Deprecated public void initNAL9() {

        the(new Anticipate(this));
        the(new Inperience(this));
        //memory.the(new Abbreviation(this, "_"));

        //onExec(Counting.class);

//                /*if (internalExperience == Minimal) {
//                    new InternalExperience(this);
//                    new Abbreviation(this);
//                } else if (internalExperience == Full)*/ {
//                    on(FullInternalExperience.class);
//                    on(Counting.class);
//                }
    }



    protected void initDefaults() {


        this.duration.set(5);

        this.conceptBeliefsMax.set(16);
        this.conceptGoalsMax.set(12);
        this.conceptQuestionsMax.set(3);

        this.conceptForgetDurations.setValue(2.0);
        this.termLinkForgetDurations.setValue(5.0);
        this.taskLinkForgetDurations.setValue(3.0);

        this.derivationDurabilityThreshold.setValue(Global.DERIVATION_DURABILITY_THRESHOLD);

        this.taskProcessThreshold.setValue(0); //warning: if this is not zero, it could remove un-TaskProcess-able tasks even if they are stored by a Concept

        //budget propagation thresholds
        this.termLinkThreshold.setValue(Global.BUDGET_PROPAGATION_EPSILON);
        this.taskLinkThreshold.setValue(Global.BUDGET_PROPAGATION_EPSILON);

        this.executionThreshold.setValue(Global.TRUTH_EPSILON);

        this.shortTermMemoryHistory.set(2);



    }

    @Nullable
    abstract protected Function<Term, Concept> newConceptBuilder();


//    public static final AbstractOperator[] exampleOperators = {
//            //new Wait(),
//            new NullOperator("break"),
//            new NullOperator("drop"),
//            new NullOperator("goto"),
//            new NullOperator("open"),
//            new NullOperator("pick"),
//            new NullOperator("strike"),
//            new NullOperator("throw"),
//            new NullOperator("activate"),
//            new NullOperator("deactivate")
//    };





    public final AbstractOperator[] defaultOperators = {

            //system control

            //PauseInput.the,
            new reset(),
            //new eval(),
            //new Wait(),

//            new believe(),  // accept a statement with a default truth-value
//            new want(),     // accept a statement with a default desire-value
//            new wonder(),   // find the truth-value of a statement
//            new evaluate(), // find the desire-value of a statement
            //concept operations for internal perceptions
//            new remind(),   // create/activate a concept
//            new consider(),  // do one inference step on a concept
//            new name(),         // turn a compount term into an atomic term
            //new Abbreviate(),
            //new Register(),

            new echo(),


            new doubt(),        // decrease the confidence of a belief
//            new hesitate(),      // decrease the confidence of a goal

            //Meta
            new reflect(),
            //new jclass(),

            // feeling operations
            //new feelHappy(),
            //new feelBusy(),


            // math operations
            new length(),
            new add(),

            new intToBitSet(),

            //new MathExpression(),

            new complexity(),

            //Term manipulation
            new flat.flatProduct(),
            new similaritree(),

            //new NumericCertainty(),

            //io operations
            new say(),

            new schizo(),     //change Memory's SELF term (default: SELF)

            new js(), //javascript evalaution

            /*new json.jsonfrom(),
            new json.jsonto()*/
         /*
+         *          I/O operations under consideration
+         * observe          // get the most active input (Channel ID: optional?)
+         * anticipate       // get the input matching a given statement with variables (Channel ID: optional?)
+         * tell             // output a judgment (Channel ID: optional?)
+         * ask              // output a question/quest (Channel ID: optional?)
+         * demand           // output a goal (Channel ID: optional?)
+         */

//        new Wait()              // wait for a certain number of clock cycle


        /*
         * -think            // carry out a working cycle
         * -do               // turn a statement into a goal
         *
         * possibility      // return the possibility of a term
         * doubt            // decrease the confidence of a belief
         * hesitate         // decrease the confidence of a goal
         *
         * feel             // the overall happyness, average solution quality, and predictions
         * busy             // the overall business
         *


         * do               // to turn a judgment into a goal (production rule) ??

         *
         * count            // count the number of elements in a set
         * arithmatic       // + - * /
         * comparisons      // < = >
         * logic        // binary logic
         *



         * -assume           // local assumption ???
         *
         * observe          // get the most active input (Channel ID: optional?)
         * anticipate       // get input of a certain pattern (Channel ID: optional?)
         * tell             // output a judgment (Channel ID: optional?)
         * ask              // output a question/quest (Channel ID: optional?)
         * demand           // output a goal (Channel ID: optional?)


        * name             // turn a compount term into an atomic term ???
         * -???              // rememberAction the history of the system? excutions of operatons?
         */
    };


//    static String readFile(String path, Charset encoding)
//            throws IOException {
//        byte[] encoded = Files.readAllBytes(Paths.get(path));
//        return new String(encoded, encoding);
//    }

//    protected DerivationFilter[] getDerivationFilters() {
//        return new DerivationFilter[]{
//                new FilterBelowConfidence(0.01),
//                new FilterDuplicateExistingBelief()
//                //param.getDefaultDerivationFilters().add(new BeRational());
//        };
//    }


//    protected final Concept newConcept(Term t) {
//        return conceptBuilder.apply(t);
//    }




    //    /**
//     * rank function used for concept belief and goal tables
//     */
//    public BeliefTable.RankBuilder newConceptBeliefGoalRanking() {
//        return (c, b) ->
//                BeliefTable.BeliefConfidenceOrOriginality;
//        //new BeliefTable.BeliefConfidenceAndCurrentTime(c);
//
//    }




    @NotNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + nal() + ']';
    }

    @Nullable
    protected Deriver newDeriver() {
        return Deriver.getDefaultDeriver();
    }

    /** reports all active concepts or those which can be reached */
    @Override
    @Nullable
    public abstract NAR forEachConcept(Consumer<Concept> recip);


    public static class DefaultTermIndex extends MapIndex2  {

        public DefaultTermIndex(int capacity, @NotNull Random random) {
            super(new UnifriedMap(capacity),
                  new DefaultConceptBuilder(random, 32, 32));

        }
    }
    public static class WeakTermIndex extends MapIndex2  {

        public WeakTermIndex(int capacity, @NotNull Random random) {
            super(new WeakHashMap<TermContainer, SubtermNode>(capacity),
                    new DefaultConceptBuilder(random, 32, 32));

        }
    }

//    public static class DefaultTermIndex2 extends MapIndex3 {
//
//        public DefaultTermIndex2(int capacity, Random random) {
//            super(capacity, Terms.terms, new DefaultConceptBuilder(random, 32, 32));
//
//        }
//
//    }

}
