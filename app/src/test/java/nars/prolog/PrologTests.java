package nars.prolog;

import alice.tuprolog.*;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by me on 3/3/16.
 */
public class PrologTests {

    @Test public void testHello() {
        new Agent("go :- write('hello, world!'), nl.\n",
                  ":-solve(go).").run();
    }


    @Test public void maze2() {

        String theory =
                "arc(a,b).\n"+
                "arc(a,d).\n"+
                "arc(b,e).\n"+
                "arc(d,g).\n"+
                "arc(g,h).\n"+
                "arc(e,f).\n"+
                "arc(f,i).\n"+
                "arc(e,h).\n"+
                "path(X,X,[X]).\n"+
                "path(X,Y,[X|Q]):-arc(X,Z),path(Z,Y,Q).";

        assertEquals("yes.\n" +
                     "X / [a,b,e]",
                     new Agent(theory, "path(a,e,X).").run().toString());

        assertEquals("yes.\n" +
                     "X / [a,b,e,f]",
                new Agent(theory, "path(a,f,X).").run().toString());

        assertEquals("no.", new Agent(theory, "path(z,f,X).").run().toString());


    }

    @Test public void testMisc() {
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


}



