package nars.op.sys.prolog;


/**
 * Created by me on 1/29/16.
 */
public class PrologMainTest {

//    @Test
//    public void testAsk() {
//        AXR p = new AXR();
//        assertEquals("the(eq(V_0,V_0))", p.ask("eq(X,X)").pprint());
//        assertEquals("the(eq(1,1))", p.ask("eq(X,1)").pprint());
//        assertEquals("the(eq(1,1))", p.ask("eq(1,1)").pprint());
//        assertEquals("the(compute('-',3,2,1))", p.ask("compute('-',3,2,N)").pprint());
//
//    }
//
//    @Test public void testDynamicAssertionKB() {
//        AXR p = new AXR();
//        assertEquals(PTerm.NO, p.ask("sth(a,b)"));
//        assertEquals(PTerm.YES, p.add("sth(a,b)"));
//        assertEquals("the(sth(a,b))", p.ask("sth(a,b)").pprint());
//
//
//        p.ask("assert(':-'(hts(X,Y),sth(Y,X)))");
//        assertEquals(PTerm.NO, p.ask("hts(a,b)"));
//        assertEquals("the(hts(b,a))", p.ask("hts(b,a)").pprint());
//        assertEquals("the(hts(b,a))", p.ask("hts(X,Y)").pprint());
//        assertNotEquals("the(hts(b,c))", p.ask("hts(X,Y)").pprint());
//        assertNotEquals("the(hts(a,b))", p.ask("hts(X,Y)").pprint());
//
////        p.ask("all(the(sth(X,Y)), S).", (R) -> {
////            if (R == null) return false;
////            System.out.println(R);
////            Fun NamedR = (Fun) R.numbervars();
////            System.out.println(NamedR);
//////              for (int j = 0; j < Names.arity(); j++) {
//////                  IO.println(((Fun) Names).arg(j) + "=" + NamedR.arg(j));
//////              }
////            return true;
////        });
////        //assertEquals("the(sth(a,b))", );
//    }
//
//    @Test public void testMazeSolve() {
//        //https://courses.cs.vt.edu/~cs1104/TowerOfBabel/Prolog.examples.1.html
//        AXR p = new AXR();
//
//        /*
//        a maze in which we are asked "is there a route from point X to point Y?"
//        We start by defining the database of facts which describe the paths between points (squares in the above diagram):
//
//        path(a,b).
//        path(b,c).
//        path(c,d).
//        path(f,c).
//        path(b,e).
//        path(d,e).
//        path(e,f).
//        route(X,X).
//        route(X,Y) :- path(X,Z), route(Z,Y).
//         */
//
//        p.addAll(
//            "route(X,X)",
//
//            //assert(':-'(a(X),','(b(X),c(X))))
//            "assert(':-'(route(X,Y),','(path(X,Z),path(Z,Y))))",
//
//            "path(a,b)",
//            "path(b,c)",
//            "path(c,d)",
//            "path(f,c)",
//            "path(b,e)",
//            "path(d,e)",
//            "path(e,f)"
//        );
//        p.ask("':-'(why(the(route(a,e)),X))");
//        p.goal("':-'(why(route(a,e),X))");
//        p.ask("write(why(route(a,e)))");
//        p.evalIO( p.goal("route(a,b)") );
//        p.evalIO( p.goal("route(f,a)") );
//        p.evalIO( p.goal("route(a,b)") );
//
//        System.out.println( p.ask("route(a,e)") ); //yes
//        System.out.println( p.ask("route(a,b)") ); //yes
//        System.out.println( p.ask("route(f,a)") ); //no
//
//        //p.print(System.out);
//
//
////        assertEquals(PTerm.YES, p.add("sth(a,b)"));
////        assertEquals("the(sth(a,b))", p.ask("sth(a,b)").pprint());
//    }
//
////    @Test
////    public void test1() {
////        PrologMain p = new PrologMain();
////        p.ask("['/tmp/x'].");
////        PTerm r = p.ask("mid.");
////        System.out.println(r);
////
////        //assertEquals("the(['/tmp/x'])", p.goal("['/tmp/x'].").pprint());
////        //assertEquals(null, p.ask("small.").pprint());
////
////    }
}