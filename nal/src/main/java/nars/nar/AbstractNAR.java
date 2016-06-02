package nars.nar;

import nars.Global;
import nars.NAR;
import nars.budget.policy.DefaultConceptBudgeting;
import nars.concept.Concept;
import nars.index.TermIndex;
import nars.nal.Deriver;
import nars.nal.meta.PremiseRule;
import nars.nal.nal8.AbstractOperator;
import nars.nal.op.ImmediateTermTransform;
import nars.op.data.*;
import nars.op.math.add;
import nars.op.math.length;
import nars.op.mental.Anticipate;
import nars.op.mental.Inperience;
import nars.op.mental.doubt;
import nars.op.mental.schizo;
import nars.op.out.echo;
import nars.op.out.say;
import nars.op.sys.reset;
import nars.op.time.STMTemporalLinkage;
import nars.task.Task;
import nars.term.atom.Atom;
import nars.time.Clock;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Consumer;


/**
 * Default set of NAR parameters which have been classically used for development.
 * <p>
 * WARNING this Seed is not immutable yet because it extends Param,
 * which is supposed to be per-instance/mutable. So do not attempt
 * to create multiple NAR with the same Default seed model
 */
public abstract class AbstractNAR extends NAR {

    @NotNull
    public final DefaultConceptBudgeting conceptWarm, conceptCold;

    public static final int INDEX_TO_CORE_INITIAL_SIZE_RATIO = 4;

    public AbstractNAR(@NotNull Clock clock, TermIndex index, @NotNull Random random) {
        this(clock, index, random, Global.DEFAULT_SELF);
    }

    public AbstractNAR(@NotNull Clock clock, TermIndex index, @NotNull Random rng, @NotNull Atom self) {
        super(clock, index, rng, self);

        /*this.conceptBeliefsMax.set(12);
        this.conceptGoalsMax.set(9);
        this.conceptQuestionsMax.set(3);*/
        conceptWarm = new DefaultConceptBudgeting(10, 10, 3, 16, 8);
        conceptCold = new DefaultConceptBudgeting(8, 8, 1, 8, 4);

        derivationDurabilityThreshold.setValue(Global.DERIVATION_DURABILITY_THRESHOLD);

        taskProcessThreshold.setValue(0); //warning: if this is not zero, it could remove un-TaskProcess-able tasks even if they are stored by a Concept

        //budget propagation thresholds
        termLinkThreshold.setValue(Global.BUDGET_EPSILON);
        taskLinkThreshold.setValue(Global.BUDGET_EPSILON);

        executionThreshold.setValue(Global.TRUTH_EPSILON);



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

    /** NAL7 plugins */
    public void initNAL7() {

        the(new STMTemporalLinkage(this, 2)); //TEMPORARY
    }

    /* NAL8 plugins */
    public void initNAL8() {
        /* derivation operators available at runtime */
        for (Class<? extends ImmediateTermTransform> c : PremiseRule.Operators) {
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


    protected @NotNull Deriver newDeriver() {
        return Deriver.getDefaultDeriver();
    }


    /**
     * process a Task through its Concept
     */
    @Nullable @Override
    public final Concept process(@NotNull Task input, float activation) {

        Concept c = concept(input, true);
        if (c == null) {
            if (Global.DEBUG) {
                //throw new InvalidTaskException(input, "Inconceivable");
                logger.error("Inconceivable: {}", input);
            }
            input.delete("Inconceivable");
            return null;
        }

        float business = input.pri() * activation;
        emotion.busy(business);


        Task t = c.process(input, this);
        if (t != null && !t.isDeleted()) {
            //TaskProcess succeeded in affecting its concept's state (ex: not a duplicate belief)

            t.onConcept(c);

            //propagate budget
            MutableFloat overflow = new MutableFloat();

            conceptualize(c, t, activation, activation, overflow);

            emotion.stress(overflow);


            eventTaskProcess.emit(t); //signal any additional processes

        } else {
            emotion.frustration(business);
        }

        return c;
    }


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

            //new js(), //javascript evalaution

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

    /** reports all active concepts or those which can be reached */
    @Override
    @Nullable
    public abstract NAR forEachConcept(@NotNull Consumer<Concept> recip);



//    public static class DefaultTermIndex2 extends MapIndex3 {
//
//        public DefaultTermIndex2(int capacity, Random random) {
//            super(capacity, Terms.terms, new DefaultConceptBuilder(random, 32, 32));
//
//        }
//
//    }

}
