package nars.prolog;

import alice.tuprolog.*;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.Test;

import java.util.List;

import static org.apache.commons.collections4.IteratorUtils.toList;
import static org.junit.Assert.assertEquals;

/**
 * Created by me on 3/3/16.
 */
public class PrologTests {

    @Test
    public void testHello() {
        new Agent("go :- write('hello, world!'), nl.\n",
                ":-solve(go).").run();
    }

    @Test
    public void testAnd() {
        assertEquals(
                "yes.",
                new Agent("t(a).\nt(b).\nt(c).\n",
                        "t(a), t(b), t(c).").run().toString());
    }

    @Test
    public void testAnd2() {
        assertEquals(
                "yes.",
                new Agent("a.\nb.\nc.\n",
                        "a, b, c.").run().toString());
    }

    @Test
    public void testInequality() {
        assertEquals(
                "no.",
                new Agent("a.\nb.",
                        "a=b.").run().toString());
        assertEquals(
                "yes.",
                new Agent("a.\nb.",
                        "a=a.").run().toString());
        assertEquals(
                "yes.",
                new Agent("a.\nb.",
                        "not(a=b).").run().toString());
    }

    @Test
    public void maze2() {

        String theory =
                "arc(a,b).\n" +
                        "arc(a,d).\n" +
                        "arc(b,e).\n" +
                        "arc(d,g).\n" +
                        "arc(g,h).\n" +
                        "arc(e,f).\n" +
                        "arc(f,i).\n" +
                        "arc(e,h).\n" +
                        "path(X,X,[X]).\n" +
                        "path(X,Y,[X|Q]):-arc(X,Z),path(Z,Y,Q).";

        assertEquals("yes.\n" +
                        "X / [a,b,e]",
                new Agent(theory, "path(a,e,X).").run().toString());

        assertEquals("yes.\n" +
                        "X / [a,b,e,f]",
                new Agent(theory, "path(a,f,X).").run().toString());

        assertEquals("no.", new Agent(theory, "path(z,f,X).").run().toString());


    }

    @Test
    public void testMisc() {
        Var varX = new Var("X"), varY = new Var("Y");
        Struct atomP = new Struct("p");
        Struct list = new Struct(atomP, varY);    // should be [p|Y]
        System.out.println(list); // prints the list [p|Y]
        Struct fact = new Struct("p", new Struct("a"), new Int(5));
        Struct goal = new Struct("p", varX, new Var("Z"));
        Prolog engine = new Prolog();
        boolean res = goal.unify(engine, fact);    // should be X/a, Y/5
        System.out.println(goal);  // prints the unified term p(a,5)
        System.out.println(varX);  // prints the variable binding X / a
        Var varW = new Var("W");
        res = varW.unify(engine, varY);    // should be Z=Y
        System.out.println(varY);        // prints just Y, since it is unbound
        System.out.println(varW);        // prints the variable binding W / Y

    }

    /**
     * http://www.cs.unm.edu/~luger/ai-final2/CH8_Natural%20Language%20Processing%20in%20Prolog.pdf
     */
    @Test
    public void testCFG1() throws MalformedGoalException, NoSolutionException, NoMoreSolutionException {

        String theory =
                "utterance(X) :- sentence(X, [ ]).\n" +
                "sentence(Start, End) :- nounphrase(Start, Rest), verbphrase(Rest, End).\n" +
                "nounphrase([Noun | End], End) :- noun(Noun).\n" +
                "nounphrase([Article, Noun | End], End) :- article(Article), noun(Noun).\n" +
                "verbphrase([Verb | End], End) :- verb(Verb).\n" +
                "verbphrase([Verb | Rest], End) :- verb(Verb), nounphrase(Rest, End).\n" +
                "article(a).\n" +
                "article(the).\n" +
                "noun(man).\n" +
                "noun(dog).\n" +
                "verb(likes).\n" +
                "verb(bites).";

        List<Term> solutions = toList(
            new Agent(theory).iterate("utterance([the, man, likes, X]).")
        );

        assertEquals(
                "[utterance([the,man,likes,man]), utterance([the,man,likes,dog])]",
                solutions.toString());

    }

}



