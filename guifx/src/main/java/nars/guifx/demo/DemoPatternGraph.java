package nars.guifx.demo;


import nars.Global;
import nars.bag.BLink;
import nars.concept.Concept;
import nars.guifx.highdim.AEConcept1;
import nars.guifx.highdim.HighDim;
import nars.guifx.util.Animate;
import nars.guifx.util.TabX;
import nars.nal.Deriver;
import nars.nar.Default;

public class DemoPatternGraph {


    public static void main(String[] args) {
        Global.DEBUG = true;

        Default n = new Default(1024, 2, 3, 3);
        //n.core.activationRate.setValue(0.75f);

        Deriver.getDefaultDeriver().rules.reifyTo(n);
        n.logSummaryGT(System.out,0.25f);

        NARide.show(n.loop(), ide -> {

                    HighDim<BLink<Concept>> dim = new HighDim<>(64,
                            new AEConcept1()
                            //new HighDim.ScatterPlot1()
                    );

                    n.onFrame(N -> {
                        dim.commit(((Default) N).core.active);
                        //System.out.println(dim.node + " free=" + dim.free.size());
                    });
                    new Animate(30, (a) -> dim.update()).start();


                    //ide.addView(new IOPane(n));
                    ide.content.getTabs().setAll(
                            new TabX("Graph",
                                    //newGraph(n)
                                    dim
                            ));


                    //ide.setSpeed(25);
                    //n.frame(5);

                }


        );
    }
}