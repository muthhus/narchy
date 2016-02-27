package nars.nal;

import nars.Global;
import nars.Memory;
import nars.NAR;
import nars.concept.Concept;
import nars.nar.Default;
import nars.task.Task;

/**
 * Created by me on 1/28/16.
 */
public class TuffySmokesTest {

	static void question(NAR n) {
        for (String name : new String[] { "Edward", "Anna", "Bob", "Frank", "Gary", "Helen" }) {
            String t = "<" + name + " --> [Cancer]>";
            //n.input(t + "?");
            Concept c = n.concept(t);
            if (c == null)  {
                System.err.println(t + " unknown");
            } else {
                if (!c.hasBeliefs()) {
                    System.err.println(t + " no beliefs");
                } else {
                    Task b = c.beliefs().top(n.time());
                    System.err.println(t + " = " + b);
                    //System.err.println(b.getExplanation());
                    continue;
                }
            }
            n.input(t + "?");
        }

        //n.input("<?x --> [Cancer]>?");
    }
	static void input(NAR n) {
		n.input("<(Anna,Bob) --> Friends>. %1.00;0.99%");
		n.input("<(Anna,Edward) --> Friends>. %1.00;0.99%");
		n.input("<(Anna,Frank) --> Friends>. %1.00;0.99%");
		n.input("<(Edward,Frank) --> Friends>. %1.00;0.99%");
		n.input("<(Gary,Helen) --> Friends>. %1.00;0.99%");
		n.input("(--,<(Gary,Frank) --> Friends>). %1.00;0.99%");

		n.input("<Anna --> [Smokes]>. %1.00;0.99%");
		n.input("<Edward --> [Smokes]>. %1.00;0.99%");
		n.input("(--, <Gary --> [Smokes]>). %1.00;0.99%");
		n.input("(--, <Frank --> [Smokes]>). %1.00;0.99%");
		n.input("(--, <Helen --> [Smokes]>). %1.00;0.99%");
		n.input("(--, <Bob --> [Smokes]>). %1.00;0.99%");
	}

	static void axioms(NAR n) {

		// only positive side of => corresponds to ==> in NAL, so we have to
		// translate

		n.input("<<($1,$2) --> Friends> ==> <($2,$1) --> Friends>>. %1.00;0.99%");

		// !Smokes(a1) || cancer(a1) rewritten: Smokes(a1) => cancer(a1) or
		// !cancer(a1) => !Smokes(a1)
		n.input("<<$1 --> [Smokes]> ==> <$1 --> [Cancer]>>. %1.00;0.50%");
		n.input("<(--,<$1 --> [Cancer]>) ==> (--,<$1 --> [Smokes]>)>. %1.00;0.50%");

		// !Friends(a1,a2) || !Smokes(a1) || Smokes(a2)
		// rewritten:
		// Friends(a1,a2) => (!Smokes(a1) || Smokes(a2)) 1.
		n.input("<<($1,$2) --> Friends> ==> (||,<$2 --> [Smokes]>,(--,<$1 --> [Smokes]>))>. %1.00;0.40%");

		// and contraposition:
		// !(!Smokes(a1) || Smokes(a2)) => !Friends(a1,a2)
		// (Smokes(a1) && !Smokes(a2)) => !Friends(a1,a2) 2.
		n.input("<(&&,<$1 --> [Smokes]>,(--,<$2 --> [Smokes]>)) ==> (--,<($1,$2) --> Friends>)>. %1.00;0.40%");

		// !Friends(a1,a2) || !Smokes(a2) || Smokes(a1)
		n.input("<<($1,$2) --> Friends> ==> (||,<$2 --> [Smokes]>,(--,<$1 --> [Smokes]>))>. %1.00;0.40%");

		// !Friends(a1,a2) || !Smokes(a1) || Smokes(a2)
		// rewritten2:
		// Smokes(a1) => (Smokes(a2) || !Friends(a1,a2))
		n.input("<<$1 --> [Smokes]> ==> (||,<$2 --> [Smokes]>,(--,<($1,$2) --> Friends>))>. %1.00;0.40%");

		// and contraposition:
		// !(Smokes(a2) || !Friends(a1,a2)) => !Smokes(a1)
		// (!Smokes(a2) && Friends(a1,a2)) => !Smokes(a1)
		n.input("<(&&,(--,<$2 --> [Smokes]>),<($1,$2) --> Friends>) ==> (--,<$1 --> [Smokes]>)>. %1.00;0.40%");

		// !Friends(a1,a2) || !Smokes(a1) || Smokes(a2)
		// rewritten2:
		// !Smokes(a2) => (!Friends(a1,a2) || !Smokes(a1))
		n.input("<(--,<$2 --> [Smokes]>) ==> (||,(--,<($1,$2) --> Friends>),(--,<$1 --> [Smokes]>))>. %1.00;0.40%");

		// and contraposition:
		// !(!Friends(a1,a2) || !Smokes(a1)) => Smokes(a2)
		// (Friends(a1,a2) && Smokes(a1)) => Smokes(a2)
		n.input("<(&&,<($1,$2) --> Friends>,<$1 -->  [Smokes]>) ==> <$2 --> [Smokes]>>.");

		// n.input("<#x --> [Cancer]>?");

	}

	// @Test
	// public void testReasonableAnswers() {
	public static void main(String[] args) {

		Global.DEBUG = true;

		NAR n = new Default(1000, 3, 2, 3);
		((Memory) NAR.this).activationRate.setValue(0.5f);
		((Memory) NAR.this).conceptForgetDurations.setValue(5);

		// n.log();

		axioms(n);
		n.run(100);

		input(n);
		n.run(4000);
		question(n);

		n.run(4000);
		question(n);

		n.run(4000);
		question(n);

		// NARide.loop(n, true);

		// n.log();
		// n.log(System.out, k -> { System.out.println(k); return
		// k.equals("eventAnswer"); } );
		// n.log(System.out, k -> k instanceof Twin);

		// n.run(5500);

		// Result:
		// Solved <Anna --> [Cancer]>. %1.00;0.50%
		// Solved <Edward --> [Cancer]>. %1.00;0.25%
		// Solved <Frank --> [Cancer]>. %1.00;0.03%
		// Solved <Bob --> [Cancer]>. %1.00;0.20%
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

	}
}
