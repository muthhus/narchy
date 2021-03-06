package nars.nal;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.Concept;

/**
 * Created by me on 1/28/16.
 */
public class TuffySmokesTest {

    static NAR n;

    static void print() throws Narsese.NarseseException {
        for (String name : new String[]{"Edward", "Anna", "Bob", "Frank", "Gary", "Helen"}) {
            String t = "<" + name + " --> Cancer>";
            //n.input(t + "?");
            Concept c = n.conceptualize(t);

            System.err.print(System.identityHashCode(c) + " ");

            if (c == null) {
                System.err.println(t + " unknown" + " ");
            } else {
//                if (!c.hasBeliefs()) {
//                    System.err.println(t + " no beliefs");
//
//
//                    //n.input(t + ". %0.5;0.1%");
//
//                } else {
//                    Task b = c.beliefs().top(ETERNAL);
//                    System.err.println(t + " = " + b);
//                    //System.err.println(b.getExplanation());
//                    continue;
//                }
            }
            //n.input(t + "?");
        }
    }

    static void input(NAR n) throws Narsese.NarseseException {
        n.input("<(Anna&Bob) --> Friends>. %1.00;0.99%");
        n.input("<(Anna&Edward) --> Friends>. %1.00;0.99%");
        n.input("<(Anna&Frank) --> Friends>. %1.00;0.99%");
        n.input("<(Edward&Frank) --> Friends>. %1.00;0.99%");
        n.input("<(Gary&Helen) --> Friends>. %1.00;0.99%");
        n.input("<(Gary&Frank) --> Friends>. %0.00;0.99%"); //NOT

        n.input("<Anna --> Smokes>. %1.00;0.99%");
        n.input("<Edward --> Smokes>. %1.00;0.99%");
        n.input("<Gary --> Smokes>. %0.00;0.99%");  //NOT
        n.input("<Frank --> Smokes>. %0.00;0.99%"); //NOT
        n.input("<Helen --> Smokes>. %0.00;0.99%"); //NOT
        n.input("<Bob --> Smokes>. %0.00;0.99%"); //NOT
    }

    static void axioms(NAR n) throws Narsese.NarseseException {

        // only positive side of => corresponds to ==> in NAL, so we have to
        // translate

        //n.input("<<{$1,$2} --> Friends> ==> <($2,$1) --> Friends>>. %1.00;0.99%");

        // !Smokes(a1) || cancer(a1) rewritten: Smokes(a1) => cancer(a1) or
        // !cancer(a1) => !Smokes(a1)
        n.input("(<$1 --> Smokes> ==> <$1 --> Cancer>). %1.00;0.90%");
        //n.input("((--,<$1 --> Smokes>) ==> (--, <$1 --> Cancer>)). %1.00;0.90%");
        //n.input("((--, <$1 --> Smokes>) ==> <$1 --> Cancer>). %1.00;0.50%");

        //n.input("<(--,<$1 --> Cancer>) ==> (--,<$1 --> Smokes>)>. %1.00;0.90%");

        // !Friends(a1,a2) || !Smokes(a1) || Smokes(a2)
        // rewritten:
        // Friends(a1,a2) => (!Smokes(a1) || Smokes(a2)) 1.
        //n.input("<<{$1,$2} --> Friends> <=> (||,<$2 --> Smokes>,(--,<$1 --> Smokes>))>. %1.00;0.40%");
        n.input("(< --(($1&$2) --> Friends)  ==> (($2-->Smokes) && (--,($1-->Smokes)))). %1.00;0.90%");


        // and contraposition:
        // !(!Smokes(a1) || Smokes(a2)) => !Friends(a1,a2)
        // (Smokes(a1) && !Smokes(a2)) => !Friends(a1,a2) 2.
        //n.input("<(&&,<$1 --> Smokes>,(--,<$2 --> Smokes>)) ==> (--,<{$1,$2} --> Friends>)>. %1.00;0.90%");

        // !Friends(a1,a2) || !Smokes(a2) || Smokes(a1)
        //n.input("<<{$1,$2} --> Friends> ==> (||,<$2 --> Smokes>,(--,<$1 --> Smokes>))>. %1.00;0.90%");

        // !Friends(a1,a2) || !Smokes(a1) || Smokes(a2)
        // rewritten2:
        // Smokes(a1) => (Smokes(a2) || !Friends(a1,a2))
        //n.input("<<$1 --> Smokes> ==> (||,<$2 --> Smokes>,(--,<{$1,$2} --> Friends>))>. %1.00;0.90%");

        // and contraposition:
        // !(Smokes(a2) || !Friends(a1,a2)) => !Smokes(a1)
        // (!Smokes(a2) && Friends(a1,a2)) => !Smokes(a1)
        //n.input("<(&&,(--,<$2 --> Smokes>),<{$1,$2} --> Friends>) ==> (--,<$1 --> Smokes>)>. %1.00;0.90%");

        // !Friends(a1,a2) || !Smokes(a1) || Smokes(a2)
        // rewritten2:
        // !Smokes(a2) => (!Friends(a1,a2) || !Smokes(a1))
        //n.input("<(--,<$2 --> Smokes>) ==> (||,(--,<{$1,$2} --> Friends>),(--,<$1 --> Smokes>))>. %1.00;0.90%");

        // and contraposition:
        // !(!Friends(a1,a2) || !Smokes(a1)) => Smokes(a2)
        // (Friends(a1,a2) && Smokes(a1)) => Smokes(a2)
        //n.input("<(&&,<{$1,$2} --> Friends>,<$1 -->  Smokes>) ==> <$2 --> Smokes>>.");

        // n.input("<#x --> Cancer>?");

    }

    // @Test
    // public void testReasonableAnswers() {
    public static void main(String[] args) throws Narsese.NarseseException {

        //Global.DEBUG = true;

        //this.activeTasks = activeTasks;
        n = new NARS().get();
        //n.conceptActivation.setValue(0.75f);
        //n.conceptRemembering.setValue(12);

        //n.cyclesPerFrame.set(8);
        n.logPriMin(System.out, 0f);

        //new PrologCore(n);

        //for (int i = 0; i < 10; i++) {
        System.err.println();

        axioms(n);
        input(n);
//        n.input(new LambdaQuestionTask($("(?x --> Cancer)"), '?', ETERNAL, 16, (q, a) -> {
//            System.out.println(a);
//        }).budgetSafe(1f,0.99f));
        n.run(250);  print();
        n.run(250);  print();
        n.run(250);  print();

        //}

        // NARide.loop(n, true);

        // n.log();
        // n.log(System.out, k -> { System.out.println(k); return
        // k.equals("eventAnswer"); } );
        // n.log(System.out, k -> k instanceof Twin);

        // n.run(5500);

        // Result:
        // Solved <Anna --> Cancer>. %1.00;0.50%
        // Solved <Edward --> Cancer>. %1.00;0.25%
        // Solved <Frank --> Cancer>. %1.00;0.03%
        // Solved <Bob --> Cancer>. %1.00;0.20%
        //
        // Which gives:
        //
        // Anna
        // Edward
        // Bob
        // Frank
        //
        // Tuffy gives:
        //
        // Edward
        // Anna
        // Bob
        // Frank
        //
        // but under AIKR the results may sometimes differ,
        // the smokes example is not that interesting
        // for attention-controlled systems.
        // Especially the predicate logic formulation is quite unnatural.


		/*
        http://i.stanford.edu/hazy/tuffy/doc/
		0.75	Cancer(Edward)
		0.65	Cancer(Anna)
		0.50	Cancer(Bob)
		0.45	Cancer(Frank)
		 */
    }
}
