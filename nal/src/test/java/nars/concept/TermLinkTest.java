package nars.concept;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Test;


public class TermLinkTest {

    @Test public void testTermLinkActivationOnConceptualization() throws Narsese.NarseseException {
        NAR n = new NARS().get();
        n.input("a:b.");
        n.input("b:c.");
        n.log();

        for (int i = 0; i < 55; i++) {
            System.out.println(n.time());
            n.cycle();
        }

    }

//    abstract static class Hebbian<V extends Term, E extends Compound> {
//
//        private final NAR nar;
//        private final int size;
//
//        public Hebbian(NAR n, int size, float initialActivationPerEdge) {
//            this.nar = n;
//            this.size = size;
//            for (int x = 0; x < size; x++) {
//                for (int y = 0; y < size; y++) {
//                    if ((x!=y) && (x < y))
//                        n.believe(initialActivationPerEdge, edge(x, y), Tense.Eternal, 1f, 0.9f);
//                }
//            }
//        }
//
//        abstract protected V vertex(int x);
//        abstract protected E edge(V x, V y);
//
//        public float pri(int x) {
//            return pri(vertex(x));
//        }
//
//        float pri(Term x) {
//            return nar.conceptPriority(x);
//        }
//
//        public float pri(int x, int y) {
//            BLink<Term> tl = concept(x).termlinks().get(vertex(y));
//            if (tl == null)
//                return 0f;
//            return tl.pri();
//        }
//
//        public void activate(int x) {
//
//            //Concept c = concept(x);
//            Concept c = nar.activate(vertex(x),
//                    /* concept factor */   /* link factor */  null);
//
//            //((CompoundConcept)c).linkPeers(b(a, 0.5f, 0.5f), 1f, nar, true);
//        }
//
//        public @Nullable Concept concept(int x) {
//            return concept(vertex(x));
//        }
//
//        public void activate(int x, int y, float a) {
//            //nar.conceptualize(edge(x, y), b(a, 0.5f, 0.5f));
//            //Concept c = nar.conceptualize(edge(x, y), b(a, 0.5f, 0.5f), 1f, 1f, null);
//            Budget b = b(a, 0.5f, 0.5f);
//            Concept c = nar.activate(vertex(x), null);
//            Concept.linkPeer(c.termlinks(), vertex(y), b, a);
//
//        }
//
//
//
//        final public Termed edge(int x, int y) {
//            return edge(vertex(x), vertex(y));
//        }
//
//        public Stream<Term> vertices() {
//            return IntStream.range(0, size).mapToObj(i -> vertex(i));
//        }
//
//        public void commit() {
//            vertices().forEach(v -> {
//                @NotNull Bag<Term> tl = concept(v).termlinks();
//                tl.commit();
//                System.out.println("commit: " + v + " " + tl);
//            });
//        }
//
//        public @Nullable Concept concept(Term v) {
//            return nar.concept(v);
//        }
//
//        public void print() {
//            commit();
//
//            System.out.println("Vertex: " + Joiner.on(" ").join(
//                vertices().map(v -> v.toString() + '=' + pri(v)).collect(toList())
//            ));
//            System.out.println("Edges:");
//            for (int y = 0; y < size; y++) {
//                for (int x = 0; x < size; x++) {
//                    System.out.print(priChar(pri(x, y)));
//                    System.out.print(' ');
//                }
//                System.out.println();
//            }
//        }
//
//        static char priChar(float pri) {
//            int i = (int) Math.floor(pri * 9.5f);
//            return (char)(i + '0');
//        }
//
//    }
//
//    @Test public void testTermLinkHebbianLearning() {
//        Default n = new Default();
//        n.log();
//        Hebbian h = new Hebbian<Compound,Compound>(n, 4, 0.5f) {
//
//            @Override
//            public Compound vertex(int x) {
//                return $.p(the(x));
//            }
//
//            @Override
//            protected Compound edge(Compound x, Compound y) {
//                return $.sim(x, y);
//            }
//
//
//        };
//        n.next(); //builder network
//        n.core.concepts.print();
//
////        //activate neurons 1 and 3
////        h.activate(1, 1f);
////        h.activate(3, 1f);
//        h.activate(1, 3, 1f);
//        h.activate(1, 2, 1f);
//
//        n.run(10);
//
//        h.print();
//
//    }
//

//
//    @Test public void testTermLinkActivationOnConceptualization() {
//        //when a concept is conceptualized, it should have all its templates activated into its TermLink bag
//        String t = "<a --> b>";
//
//        NAR n = new SingleStepNAR();
//        n.believe(t);
//        n.frame(1);
//
//        Concept c =  n.concept(t);
//        assertNotNull(c);
//
//        int numTemplates = c.getTermLinkTemplates().size();
//        assertTrue(numTemplates > 1);
//
//        assertEquals( numTemplates, c.getTermLinks().size());
//
//
//    }
//
//    @Test
//    public void termlinkBidirectionality() {
//
//        AbstractNAR n = new Default();
//        n.core.conceptsFiredPerCycle.set(0);
//        n.believe("<x --> y>");
//        n.frame(1);
//        TermLinkGraph g = new TermLinkGraph(n);
//        assertEquals(3, g.vertexSet().size());
//        assertEquals(2+1+1, g.edgeSet().size());
//        assertEquals(
//                "[[<x-->y>, x, y], [(<x-->y>,x), (<x-->y>,y), (x,<x-->y>), (y,<x-->y>)]]",
//                g.toString());
//    }
//
//    @Test
//    public void testConjunctionTermLinks() {
//
//        Bag<TermLinkKey, TermLink> cj0 = getTermLinks("(&&,a,b)", false);
//        assertEquals("[b, a]", cj0.keySet().toString());
//
//        assertEquals(2, cj0.size());
//
//        Bag<TermLinkKey, TermLink> cj1 = getTermLinks("(&&,<#1 --> lock>,<<$2 --> key> ==> <#1 --> (/,open,$2,_)>>)", false);
//        //System.out.println(cj1.keySet());
//
//        assertEquals(7, cj1.size());
//        //NOTE: cj1.size() will equal 5 if termlinks are normalized in TermLinkBuilder
//    }
//
//    @Test
//    public void testImplicatedConjunctionWithVariablesTermLinks() {
//        Bag<TermLinkKey, TermLink> cj1 = getTermLinks("<<$1 --> lock> ==> (&&,<#2 --> key>,<$1 --> (/,open,#2,_)>)>", false);
//        //System.out.println(cj1.keySet());
//        // [Dba:<#1 --> key>, Dbb:<$1 --> (/,open,#2,_)>, Da:<$1 --> lock>, Db:(&&,<#1 --> key>,<$2 --> (/,open,#1,_)>), Dab:lock]
//        assertEquals(7, cj1.size());
//
//    }
//
//
//    @Test
//    public void testImplicationTermLinks() {
//        Bag<TermLinkKey, TermLink> cj3 = getTermLinks("<d ==> e>", true);
//        assertEquals(2, cj3.size());
//        List<TermLinkTemplate> tj3 = getTermLinkTemplates("<d ==> e>");
//        assertEquals(2, tj3.size());
//
//
//        List<TermLinkTemplate> tj2 = getTermLinkTemplates("<(c,d) ==> e>");
//        assertEquals(4, tj2.size()); //4 templates: [<(*,c,d) ==> e>:Ea|Da:(*,c,d), <(*,c,d) ==> e>:Iaa|Haa:c, <(*,c,d) ==> e>:Iab|Hab:d, <(*,c,d) ==> e>:Eb|Db:e]
//        Bag<TermLinkKey, TermLink> cj2 = getTermLinks("<(c,d) ==> e>", true);
//        //cj2.printAll();
//        assertTrue(3 <= cj2.size());
//        //assertEquals("2 of the links are transform and will not appear in the bag", 2, cj2.size());
//
//
//        /*Bag<TermLinkKey, TermLink> cj2 = getTermLinks("<<lock1 --> (/,open,$1,_)> ==> <$1 --> key>>");
//        cj2.printAll(System.out);
//
//        System.out.println();
//
//        Bag<TermLinkKey, TermLink> cj3 = getTermLinks("<(&&,<#1 --> lock>,<#1 --> (/,open,$2,_)>) ==> <$2 --> key>>");
//        cj3.printAll(System.out);
//        */
//    }
//
//    private static AbstractNAR nar(String term, boolean firing) {
//        AbstractNAR n = new Default();
//        if (!firing)
//            n.core.conceptsFiredPerCycle.set(0);
//        n.believe(term);
//        n.frame(1);
//        return n;
//    }
//
//    private List<TermLinkTemplate> getTermLinkTemplates(String term) {
//        NAR n = nar(term, false);
//        Concept c = n.concept(term);
//        assertNotNull(c);
//
//        return c.getTermLinkTemplates();
//    }
//
//    public static Bag<TermLinkKey, TermLink> getTermLinks(String term, boolean firing) {
//        AbstractNAR n = nar(term, firing);
//
//        //note: this method also seems to work
//        //Concept c = n.memory.conceptualize(n.term(term), new Budget(1f, 1f, 1f) );
//
//        //TextOutput.out(n);
//        n.input(term + '.');
//        n.frame(1);
//
//        assertTrue(n.core.concepts().iterator().hasNext());
//
//        Concept c = n.concept(term);
//
//        //System.out.println(c.getTermLinkBuilder().templates());
//        assertNotNull(c);
//
//        return c.getTermLinks();
//    }
//
//    public Set<String> getTermLinks(Bag<TermLinkKey, TermLink> t) {
//        Set<String> s = new HashSet();
//        t.forEach(l -> s.add(l.toString()));
//        return s;
//    }
//
//    @Test
//    public void testStatementComponent() {
//        NAR n = new Default();
//        n.input("<a --> b>.");
//        n.frame(1);
//
//        Set<String> tl = getTermLinks(n.concept("<a --> b>").getTermLinks());
//        assertEquals(2, tl.size());
//    }
//
//    @Test
//    public void testIdentifier() {
//        NAR n = new Default(100, 1, 1, 3);
//        n.input("<a --> b>.");
//        n.input("<<a --> b> --> d>.");
//        n.input("<<a --> b> --> e>.");
//        n.input("<c --> <a --> b>>.");
//        n.input("<c --> d>.");
//        n.input("<f --> <a --> b>>.");
//        n.frame(6);
//
//
//        Set<String> ainhb = getTermLinks(n.concept("<a --> b>").getTermLinks());
//
//        assertTrue(6 <= ainhb.size());
//        assertTrue(ainhb.contains("a"));
//        assertTrue(ainhb.contains("b"));
//        //assertTrue("not necessary to include the term's own name in component links because its index will be unique within the term anyway", !ainhb.contains("Da:a"));
//
//        Set<String> atl = getTermLinks(n.concept("a").getTermLinks());
//        //System.out.println(ainhb);
//        //System.out.println(atl);
//
////        System.out.println();
////
////        n.concept("d").termLinks.forEach(x -> System.out.println(x));
////
////        System.out.println();
////
////        n.concept("c").termLinks.forEach(x -> System.out.println(x));
////
////        System.out.println();
////
//        Set<String> f = getTermLinks(n.concept("f").getTermLinks());
//        assertTrue(f.toString(), f.size() >= 1);
//        String fAB = "<f--><a-->b>>";
//        assertTrue(f.contains(fAB));
//
//
//        //this compound involving f has no incoming links, all links are internal
//        Set<String> fc = getTermLinks(n.concept("<f --> <a --> b>>").getTermLinks());
//        assertEquals(4, fc.size());
//        assertTrue(fc.contains("f"));
//        assertTrue(fc.contains("<a-->b>"));
//        assertTrue(fc.contains("a"));
//        assertTrue(fc.contains("b"));
//
//
//    }
//
//    @Test
//    public void termlinkConjunctionImplicationFullyConnected() {
//        //from nal6.4
//        String c = "<(&&,<$x --> flyer>,<$x --> [chirping]>) ==> <$x --> bird>>";
//
//        String d = "<<$y --> [withwings]> ==> <$y --> flyer>>";
//        Bag<TermLinkKey, TermLink> x = getTermLinks(d, true);
////        for (TermLink t : x.values()) {
////            assertEquals(t.type, 3); //all component_statement links
////        }
//
//
//        assertEquals(5, getTermLinkTemplates(d).size());
//
//
//        NAR n = new Default();
//
//        n.input(c + '.');
//        n.input(d + '.');
//
//        //in each of the first two cycles (for each of the two inputs),
//        //check that termlink connectivity is complete
//        for (int i = 0; i < 2; i++) {
//            n.frame(1);
//
//            TermLinkGraph g = new TermLinkGraph(n);
//
//            ConnectivityInspector<Term, TermLink> ci = new ConnectivityInspector(g);
//            assertTrue("termlinks between the two input concepts form a fully connected graph",
//                    ci.isGraphConnected());
//
//            /*
//            int set = 0;
//            for (Set<Term> s : ci.connectedSets()) {
//                for (Term v : s)
//                    System.out.println(set + ": " + v);
//                set++;
//            }
//            */
//
//
//
//        }
//
//    }
//
////    @Test
////    public void termlinksSetAndElement() {
////        //from nal6.4
////        String c = "<{x} --> y>.";
////
////
////        NAR n = new Default().nal(6);
////        n.input(c);
////        n.frame(1); //allow sufficient time for all subterms to be processed
////
////        TermLinkGraph g = new TermLinkGraph(n);
////        assertTrue("termlinks between the two input concepts form a fully connected graph:\n" + g.toString(),
////                g.isConnected());
////
////
////        assertEquals(2, n.concept("{x}").getTermLinkBuilder().templates().size());
////
////        //assertEquals(8, g.vertexSet().size());
////        //assertEquals(9, g.edgeSet().size());
////
////        TermLinkGraph h = new TermLinkGraph().add(n.concept("{x}"), true);
////        //System.out.println(h);
////        String baix = "({x},x)";
////        assertTrue(h.toString() + " must contain " + baix, h.toString().contains(baix));
////
////        TermLinkGraph i = new TermLinkGraph().add(n.concept("x"), true);
////        //System.out.println(i);
////
////        assertTrue(i.toString(), i.toString().contains("Ba:{x}=(x,{x})"));
////
////    }
//

}
