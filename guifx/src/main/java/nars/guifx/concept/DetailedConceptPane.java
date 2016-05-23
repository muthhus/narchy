package nars.guifx.concept;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import nars.NAR;
import nars.concept.Concept;
import nars.guifx.NARfx;
import nars.guifx.chart.Plot2D;
import nars.guifx.graph2.TermEdge;
import nars.guifx.graph2.impl.BlurCanvasEdgeRenderer;
import nars.guifx.graph2.impl.HexButtonVis;
import nars.guifx.graph2.source.ConceptNeighborhoodSource;
import nars.guifx.graph2.source.DefaultGrapher;
import nars.guifx.nars.TaskButton;
import nars.nar.Default;
import nars.task.Task;
import nars.term.Termed;


/**
 * Created by me on 8/10/15.
 */
public class DetailedConceptPane extends AbstractConceptPane {

    private final TaskTablePane beliefChart;
    private final Plot2D budgetGraph;
    private final BagView<Termed> termlinkView;
    private final BagView<Task> tasklinkView;

    public DetailedConceptPane(NAR nar, Termed conceptTerm) {
        super(nar, conceptTerm);


        beliefChart = new TaskTablePane(nar);

        int budgetGraphWidth = 0;
        int budgetGraphHeight = 32;

        budgetGraph = new BudgetGraph(nar, Plot2D.BarWave, 128, budgetGraphWidth, budgetGraphHeight, term);


        int maxDisplayedBagItems = 16;
        
        termlinkView = new BagView<>(
                () -> currentConcept != null ? currentConcept.termlinks() : null,
                //(t) -> SubButton.make(nar, t.term())
                (t) -> new TaskButton(nar, t),
                maxDisplayedBagItems
        );
        tasklinkView = new BagView<>(
                () -> currentConcept != null ? currentConcept.tasklinks() : null,
                (t) -> new TaskButton(nar, t),
                maxDisplayedBagItems
        );



        beliefChart.setMouseTransparent(true);
        beliefChart.setPickOnBounds(false);

        budgetGraph.setMouseTransparent(true);
        budgetGraph.setPickOnBounds(false);
        budgetGraph.setAlignment(Pos.BOTTOM_CENTER);

        beliefChart.setAlignment(Pos.TOP_RIGHT);
        termlinkView.setAlignment(Pos.CENTER_LEFT);
        tasklinkView.setAlignment(Pos.CENTER_RIGHT);

        DefaultGrapher neighborhood = ConceptNeighborhoodGraph(nar, conceptTerm);

        neighborhood.maxWidth(Double.MAX_VALUE);
        neighborhood.maxHeight(Double.MAX_VALUE);

        maxWidth(Double.MAX_VALUE);
        maxHeight(Double.MAX_VALUE);



        //setCenter(bbb);


        StackPane content = new StackPane(
                neighborhood,

                //overlay
                budgetGraph,
                beliefChart,

                termlinkView, tasklinkView
        );
        for (Node x : content.getChildren()) {
            if (x == neighborhood) continue;
            x.setMouseTransparent(true);
            x.setPickOnBounds(false);
            x.setOpacity(0.8f);
        }

        setTop(new ConceptMenu(nar, term));
        setCenter(content);


    }

    public static DefaultGrapher ConceptNeighborhoodGraph(NAR n, Termed c) {
            DefaultGrapher g = new DefaultGrapher(n,

                    //new ConceptsSource(n),
                    new ConceptNeighborhoodSource(n, c),

                    new HexButtonVis(n),
                    //new DefaultNodeVis.HexTermNode(),

                    (A, B) -> {
                        return new TermEdge(A, B) {
                            @Override
                            public double getWeight() {
                                //return ((Concept)A.term).getPriority();
                                return pri.getMean();
                            }
                        };
                        //return $.pro(A.getTerm(), B.getTerm());
                    },

                    new BlurCanvasEdgeRenderer()
            );
            //setCenter(g);
        return g;
        }

//        private static class SubButtonGraphNode extends DefaultNodeVis.LabeledCanvasNode {
//            private final NAR n;
//
//            public SubButtonGraphNode(NAR n, Termed term) {
//                super(n, term, 32, null, null);
//                this.n = n;
//            }
//
//            @Override
//            protected Node newBase() {
//                SubButton s = SubButton.make(n, (Concept) term);
//
//                s.setScaleX(0.02f);
//                s.setScaleY(0.02f);
//                s.shade(1f);
//
//                s.setManaged(false);
//                s.setCenterShape(false);
//
//                return s;
//            }
//        }




    @Override
    protected void update(Concept concept, long now) {
        //tasks.frame();
        /*taskLinkView.frame();
        termLinkView.frame();*/


        budgetGraph.update();

        beliefChart.update(concept);

        termlinkView.update();
        tasklinkView.update();

    }


    /* test example */
    public static void main(String[] args) {
        NAR n = new Default();
        n.input("<a-->b>. <b-->c>. <c-->a>. :|:");
        n.input("<a --> b>!");
        n.input("<a --> b>?");
        n.input("<a --> b>. %0.10;0.95%");
        n.input("<a --> b>! %0.35%");
        n.input("<a --> b>. :/: %0.75%");

        n.run(16);

        NARfx.run((a, s) -> {
            NARfx.newWindow(n, n.concept("<a-->b>"));
//            s.setScene(new ConsolePanel(n, n.concept("<a-->b>")),
//                    800,600);

            new Thread(() -> n.loop(10)).start();
        });




    }


// COLUMNS:
//        GridPane bags = new GridPane();
//        ColumnConstraints column1 = new ColumnConstraints();
//        column1.setPercentWidth(50);
//        ColumnConstraints column2 = new ColumnConstraints();
//        column2.setPercentWidth(50);
//        bags.getColumnConstraints().addAll(column1, column2); // each get 50% of width
//        bags.addRow(0,termlinkView, tasklinkView);
//        bags.maxWidth(Double.MAX_VALUE);

    //BorderPane bbb = new BorderPane();


//        //Label termlinks = new Label("Termlinks diagram");
//        //Label tasklinks = new Label("Tasklnks diagram");
//        tasks = new Scatter3D<Task>() {
//
//            @Override
//            Iterable<Task>[] get() {
//                return new Iterable[] { c.getBeliefs(),
//                        c.getGoals(),
//                        c.getQuestions(),
//                        c.getQuests() };
//            }
//
//            @Override
//            protected void update(Task tl, double[] position, double[] size, Consumer<Color> color) {
//
//                System.out.println("update: " + tl);
//                if (tl.isQuestOrQuestion()) {
//                    position[0] = -1;
//                    position[1] = tl.getBestSolution() != null ? tl.getBestSolution().getConfidence() : 0;
//                }
//                else {
//                    Truth t = tl.getTruth();
//                    position[0] = t.getFrequency();
//                    position[1] = t.getConfidence();
//                }
//                float pri = tl.getPriority();
//                position[2] = pri;
//
//                size[0] = size[1] = size[2] = 0.5f + pri;
//                color.accept(ca.get(pri));
//            }
//        };
//



}
