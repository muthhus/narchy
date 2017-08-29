package nars.rdfowl;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Param;
import nars.concept.Concept;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import static junit.framework.TestCase.assertTrue;

/**
 * Created by me on 9/13/16.
 */
public class NQuadsRDFTest {



    @Test
    public void test1() throws Exception {
        final NAR n = NARS.tmp();
        n.log();
        NQuadsRDF.input(n, "<http://example.org/#spiderman> <http://xmlns.com/foaf/0.1/name> \"Человек-паук\"@ru .");
        n.run(1);
        assertTrue(n.terms.size() > 2);
    }

    @Ignore
    @Test
    public void testSchema1() throws Exception {
        final NAR n = NARS.tmp();
        File output = new File("/tmp/onto.nal");
        PrintStream pout = new PrintStream(new BufferedOutputStream(new FileOutputStream(output), 512 * 1024));

        n.input(
                NQuadsRDF.stream(n, new File(
                        //"/tmp/all-layers.nq"
                        "/home/me/Downloads/nquad"
                )).peek(t -> {
                    pout.println(t.term().toString() + t.punc());
                    //t.budget(0, 0.5f);
                })
        );

        pout.close();

//        n.forEachActiveConcept(c -> {
//            c.print();
//        });

        n.run(1);
        //n.focus.active.clear();
        n.log();
        n.input("$0.9$ (Bacteria <-> Pharmacy)?");


        //Param.DEBUG = true;


        n.run(128);

//        n.index.forEach(c -> {
//            System.out.println(c);
//        });
    }

    @Ignore
    @Test
    public void testSchema2() throws Exception {

        final NAR n = NARS.tmp();

        Param.DEBUG = true;
        String input = "/home/me/d/finance/money.orig.n3";
        File output = new File(input + ".nal");
        PrintStream pout = new PrintStream(new BufferedOutputStream(new FileOutputStream(output), 512 * 1024));

        n.log();

        n.input(
                NQuadsRDF.stream(n, new File(
                    input
                )).peek(t -> {
                    t.pri(n.priorityDefault(t.punc()));
                    pout.println(t + ".");
                })
        );

        pout.close();

//        n.forEachActiveConcept(c -> {
//            c.print();
//        });
//        n.run(512);

        n.concepts().forEach(Concept::print);
        //n.concept($.the("Buyer")).print();

//        n.input("(I-->Seller).");
//        n.input("(Them-->Buyer).");
//        n.input("(?x-->Value)?");
//        n.run(512);

    }
}