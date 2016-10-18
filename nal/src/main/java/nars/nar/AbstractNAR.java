package nars.nar;

import nars.NAR;
import nars.Param;
import nars.index.term.TermIndex;
import nars.nal.nal8.AbstractOperator;
import nars.nar.exe.Executioner;
import nars.op.data.*;
import nars.op.mental.doubt;
import nars.op.mental.schizo;
import nars.op.out.say;
import nars.op.sys.reset;
import nars.op.time.STMTemporalLinkage;
import nars.term.atom.Atom;
import nars.time.Clock;
import org.jetbrains.annotations.NotNull;

import java.util.Random;


/**
 * Default set of NAR parameters which have been classically used for development.
 * <p>
 * WARNING this Seed is not immutable yet because it extends Param,
 * which is supposed to be per-instance/mutable. So do not attempt
 * to create multiple NAR with the same Default seed model
 */
public abstract class AbstractNAR extends NAR {



    public AbstractNAR(@NotNull Clock clock, @NotNull TermIndex index, @NotNull Random rng, @NotNull Atom self, Executioner exe) {
        super(clock, index, rng, self, exe);

        durMin.setValue(BUDGET_EPSILON * 2f);



    }



    /** NAL7 plugins */
    protected void initNAL7() {

        new STMTemporalLinkage(this, 2);

    }

    /* NAL8 plugins */
    protected void initNAL8() {
        for (AbstractOperator o : defaultOperators)
            onExec(o);
    }





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

            //new echo(),


            new doubt(),        // decrease the confidence of a belief
//            new hesitate(),      // decrease the confidence of a goal

            //Meta
            new reflect(),
            //new jclass(),

            // feeling operations
            //new feelHappy(),
            //new feelBusy(),


            // math operations
            //new length(),
            //new add(),

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


    @NotNull
    @Override
    public String toString() {
        return self + ":" + getClass().getSimpleName();
    }


}
