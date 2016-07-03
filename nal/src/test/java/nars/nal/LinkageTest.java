package nars.nal;

import com.google.common.collect.Lists;
import nars.$;
import nars.NAR;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nar.AbstractNAR;
import nars.nar.Default;
import nars.term.Term;
import nars.term.Termed;
import nars.util.graph.TermLinkGraph;
import nars.util.signal.TestNAR;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jgrapht.alg.ConnectivityInspector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

import static org.junit.Assert.*;

//don't touch this file - patham9

@RunWith(Parameterized.class)
public class LinkageTest extends AbstractNALTest {

    public static final int TERM_LINK_BAG_SIZE = 8;

    public LinkageTest(Supplier<NAR> b) { super(b); }

    @Parameterized.Parameters(name= "{0}")
    public static Iterable<Supplier> configurations() {
        return Lists.newArrayList(() -> {
            Default d = new Default(1000, 1, 1, 1) {

            };
            d.nal(6);
            return d;
        });
    }

    @Override
    public void start() {
        tester = new TestNAR(nar()) {
            @Override protected boolean requireConditions() {
                return false;
            }
        };
    }

    public void ProperlyLinkedTest(@NotNull String premise1, @NotNull String premise2) throws Exception {
        TestNAR tester = test();
        tester.believe(premise1); //.en("If robin is a type of bird then robin can fly.");
        tester.believe(premise2); //.en("Robin is a type of bird.");
        tester.run(1);

        Concept ret = tester.nar.concept(premise1);
        boolean passed = false;
        passed = isPassed2(premise2, ret, passed);

        Concept ret2 = tester.nar.concept(premise2);
        boolean passed2 = false;
        passed2 = isPassed2(premise1, ret2, passed2);

        assertTrue(passed && passed2);
//        if(passed && passed2) { //dummy to pass the test:
//            tester.believe("<a --> b>");
//        } else {
//            throw new Exception("failed");
//        }
//        tester.mustBelieve(10,"<a --> b>",0.9f);
    }

    public boolean isPassed2(String premise1, @Nullable Concept<?> ret2, boolean passed2) {
        if(ret2!=null){// && ret2.getTermLinks()!=null) {
            for (BLink<Term> entry : ret2.termlinks()) {
                Term w = entry.get().term();
                if (w.toString().equals(premise1)) {
                    passed2 = true;
                }
            }
        }
        return passed2;
    }

    public void ProperlyLinkedIndirectlyTest(@NotNull String spremise1, @NotNull String spremise2) throws Exception {
        ProperlyLinkedIndirectlyTest(spremise1, '.',  spremise2);
    }

    //interlinked with an intermediate concept, this is needed in order to select one as task and the other as belief
    public void ProperlyLinkedIndirectlyTest(@NotNull String spremise1, char punc, @NotNull String spremise2) throws Exception {

        int frames = 2;

        NAR nar = test().nar;

        //nar.log();

        Termed premise1 = nar.term(spremise1);
        assertEquals("reparsed", $.$(spremise1), premise1);
        assertNotNull(premise1);
        assertEquals(nar.term(spremise1), premise1);

        Termed premise2 = nar.term(spremise2);
        assertEquals("reparsed", $.$(spremise2), premise2);
        assertNotNull(premise2);
        assertEquals(nar.term(spremise2), premise2);

        nar.input(getTask(punc, premise1));
        nar.input(getTask(punc, premise2));

        nar.run(frames);

        //List<String> fails = new ArrayList();


        boolean passed = linksIndirectly(nar, premise2, nar.concept(premise1));

        boolean passed2 = linksIndirectly(nar, premise1, nar.concept(premise2));


        //System.err.println(premise1 + " not linked with " + premise2);
        TermLinkGraph g = new TermLinkGraph(nar);
        assertTrue(g.vertexSet().size() > 0);
        assertTrue(g.toString(), g.edgeSet().size() > 0);

        //g.print(System.out);
        //System.out.println(g.isConnected() + " " + g.vertexSet().size() + " " + g.edgeSet().size());
        if (!g.isConnected()) {
//        if (!g.isStronglyConnected()) {
//            StrongConnectivityInspector ci =
            ConnectivityInspector ci = new ConnectivityInspector(g);
//                    new StrongConnectivityInspector(g);

            System.out.println("premises: " + premise1 + " and " + premise2 + " termlink subgraph connectivity:");

            ci
                .connectedSets()
                //.stronglyConnectedSubgraphs()
                .forEach( s -> System.out.println("\t" + s));

            ((Default)nar).core.concepts.print();
            nar.forEachActiveConcept(Concept::print);

        }
        assertTrue(g.isConnected());

        assertTrue(passed);
        assertTrue(passed2);

    }

    @NotNull
    public String getTask(char punc, @NotNull Termed premise1) {
        if (punc=='?') {
            return premise1.toString() + String.valueOf('?');
        } else {
            return premise1.toString() + String.valueOf(punc) + " %1.0;0.9%";
        }
    }

    public boolean linksIndirectly(@NotNull NAR nar, @NotNull Termed premise2, @NotNull Concept<?> ret) {
        boolean passed = false;
        if (ret.termlinks()!=null) {
            for (BLink<Term> entry : ret.termlinks()) {
                Term p2Term = premise2.term();
                if(entry.get().term().equals(p2Term)) {
                    passed = true;
                    break;
                }

                Term w = entry.get().term();
                Concept<?> Wc = nar.concept(w);
                if(Wc != null) {
                    for (BLink<Term> entry2 : Wc.termlinks()) {
                        if(entry2.get().term().equals(p2Term)) {
                            passed = true;
                            break;
                        }
                    }
                }
            }
        }
        return passed;
    }


    //interlinked with an intermediate concept, this is needed in order to select one as task and the other as belief
    public void ProperlyLinkedIndirectlyLayer2Test(@NotNull String premise1, @NotNull String premise2) throws Exception {
        TestNAR tester = test();
        tester.believe(premise1); //.en("If robin is a type of bird then robin can fly.");
        tester.believe(premise2); //.en("Robin is a type of bird.");
        tester.nar.run(1);

        boolean passed = links(premise1, premise2, tester);
        boolean passed2 = links(premise2, premise1, tester);
        assertTrue(passed);
        assertTrue(passed2);


        //dummy
        tester.believe("<a --> b>");
        tester.mustBelieve(1,"<a --> b>",0.9f);
    }

    public boolean links(@NotNull String premise1, String premise2, @NotNull TestNAR tester) {
        Concept<?> ret = tester.nar.concept(premise1);
        boolean passed = false;
        if(ret!=null && ret.termlinks()!=null) {
            for (BLink<Term> entry : ret.termlinks()) {
                Term et1 = entry.get().term();
                if(et1.toString().equals(premise2)) {
                    passed = true;
                    break;
                }
                Concept<?> Wc = tester.nar.concept(et1);
                if(Wc != null) {
                    for (BLink<Term> entry2 : Wc.termlinks()) {
                        Term et2 = entry2.get().term();
                        if(et2.toString().equals(premise2)) {
                            passed = true;
                            break;
                        }
                        Concept<?> Wc2 = tester.nar.concept(et2);
                        if(Wc2 != null) {
                            for (BLink<Term> entry3 : Wc2.termlinks()) {
                                Term et3 = entry3.get().term();
                                if(et3.toString().equals(premise2)) {
                                    passed = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                /*if (w.toString().equals(premise2)) {
                    passed = true;
                }*/
            }
        }
        return passed;
    }


    @Test
    public void Linkage_NAL5_abduction() throws Exception {
        ProperlyLinkedTest("((robin-->bird)==>(robin-->animal))","(robin-->animal)");
    }


    @Test
    public void Linkage_NAL5_detachment() throws Exception {
        ProperlyLinkedTest("((robin-->bird)==>(robin-->animal))", "(robin-->bird)");
    }

    @Test
    public void Linkage_NAL6_variable_elimination2() throws Exception {
        ProperlyLinkedIndirectlyTest("<<$1-->bird>==><$1-->animal>>", "(tiger-->animal)");
    }

    //here the problem is: they should be interlinked by lock
    @Test
    public void Part_Indirect_Linkage_NAL6_multiple_variable_elimination4() throws Exception {
        ProperlyLinkedIndirectlyTest("<#1 --> lock>","<{lock1} --> lock>");
    }

    @Test
    public void Indirect_Linkage_NAL6_multiple_variable_elimination4() throws Exception {
        ProperlyLinkedIndirectlyTest(
                "(&&, <#1 --> (/, open, #2, _)>, <#1 --> lock>, <#2 --> key>)",
                "<{lock1} --> lock>");
    }

    @Test
    public void Indirect_Linkage_NAL6_abduction_with_variable_elimination_abduction() throws Exception {
        ProperlyLinkedIndirectlyTest(
                "<<lock1 --> (/, open, $1, _)> ==> <$1 --> key>>",
                "<(&&, <#1 --> (/, open, $2, _)>, <#1 --> lock>) ==> <$2 --> key>>"
        );
    }

    @Test
    public void Indirect_Linkage_NAL6_second_variable_introduction_induction() throws Exception {
        ProperlyLinkedIndirectlyTest("<<lock1 --> (/, open, $1, _)> ==> <$1 --> key>>", "<lock1 --> lock>");
    }

    @Test
    public void Indirect_Linkage_NAL6_multiple_variable_elimination() throws Exception {
        ProperlyLinkedIndirectlyTest("<(&&, <$1 --> lock>, <$2 --> key>) ==> <$1 --> (/, open, $2, _)>>",
                "<{lock1} --> lock>");
    }

    @Test
    public void Indirect_Linkage_NAL6_second_level_variable_unification2() throws Exception {
        ProperlyLinkedIndirectlyTest(
                "<<$1 --> lock> ==> (&&, <$1 --> (/, open, #2, _)>, <#2 --> key>)>",
                "<{key1} --> key>");
    }

    @Test
    public void Indirect_Linkage_NAL6_variable_elimination_deduction() throws Exception {
        ProperlyLinkedIndirectlyTest(
                "<lock1 --> lock>",
                "<(&&, <#1 --> (/, open, $2, _)>, <#1 --> lock>) ==> <$2 --> key>>");
    }

    @Test
    public void Indirect_Linkage_NAL6_variable_unification7() throws Exception {
        ProperlyLinkedIndirectlyTest(
                "<(&&, <$1 --> flyer>, <($1, worms) --> food>) ==> <$1 --> bird>>",
                "<<$1 --> flyer> ==> <$1 --> [withWings]>>");
    }

    @Test
    public void Indirect_Linkage_NAL6_variable_unification6() throws Exception {
        ProperlyLinkedIndirectlyTest(
                "<(&&, <$1 --> flyer>, <$1 --> [chirping]>, <($1, worms) --> food>) ==> <$1 --> bird>>",
                "<(&&, <$1 --> [chirping]>, <$1 --> [withWings]>) ==> <$1 --> bird>>");
    }

    @Test
    public void Indirect_Linkage_NAL6_second_level_variable_unification() throws Exception {
        ProperlyLinkedIndirectlyTest("(&&, <#1 --> lock>, <<$2 --> key> ==> <#1 --> (/, open, $2, _)>>)", "<{key1} --> key>");
    }

    @Test
    public void Indirect_Linkage_Basic() throws Exception {
        ProperlyLinkedIndirectlyTest("<a --> b>", "<b --> c>");
    }

    @Test
    public void Indirect_Linkage_Layer2_Basic() throws Exception {
        ProperlyLinkedIndirectlyTest("<a --> <b --> <k --> x>>>", "<k --> x>");
    }

    @Test
    public void Indirect_Linkage_Layer2_Basic_WithVar() throws Exception {
        ProperlyLinkedIndirectlyTest("<#1 --> <b --> <k --> x>>>", "<k --> x>");
    }

    @Test
    public void Indirect_Linkage_Layer2_Basic_WithVar2() throws Exception {
        ProperlyLinkedIndirectlyTest("<a --> <b --> <#1 --> x>>>", '.', "<k --> x>");
    }
    @Test
    public void Indirect_Linkage_Layer2_Basic_WithVar2_Goal() throws Exception {
        ProperlyLinkedIndirectlyTest("<a --> <b --> <#1 --> x>>>", '!', "<k --> x>");
    }
    @Test
    public void Indirect_Linkage_Layer2_Basic_WithVar2_Question() throws Exception {
        ProperlyLinkedIndirectlyTest("<a --> <b --> <#1 --> x>>>", '?', "<k --> x>");
    }

    public void testConceptFormed(@NotNull String s) throws Exception {
        TestNAR tester = test();
        tester.believe(s,1.0f,0.9f);
        tester.nar.run(1);
        Concept ret = tester.nar.concept(s);

        assertNotNull("Failed to create a concept for "+s, ret);
    }

    @Test
    public void Basic_Concept_Formation_Test() throws Exception {
        testConceptFormed("<a --> b>");
    }

    @Test
    public void Advanced_Concept_Formation_Test() throws Exception {
        testConceptFormed("<#1 --> b>");
    }

    @Test
     public void Advanced_Concept_Formation_Test2() throws Exception {
        testConceptFormed("<<$1 --> a> ==> <$1 --> b>>");
    }

    @Test
    public void Advanced_Concept_Formation_Test2_2() throws Exception {
        testConceptFormed("<<$1 --> bird> ==> <$1 --> animal>>");
    }

    @Test
    public void Advanced_Concept_Formation_Test3() throws Exception {
        testConceptFormed("(&&,<#1 --> lock>,<<$2 --> key> ==> <#1 --> (/, open, $2, _)>>)");
    }

    @Test
    public void Advanced_Concept_Formation_Test4() throws Exception {
        testConceptFormed("(&&,<#1 --> (/,open,#2,_)>,<#1 --> lock>,<#2 --> key>)");
    }


    @Test
    public void Variable_Normalization_1() throws Exception {
        AbstractNAR tester = new Default(100,1,1,1);
        String nonsense = "<(&&,<#1 --> M>,<#2 --> M>) ==> <#1 --> nonsense>>";
        tester.believe(nonsense); //.en("If robin is a type of bird then robin can fly.");
        tester.run(1);
        Concept c = tester.concept(nonsense);
        assertNotNull(c);
    }

}
