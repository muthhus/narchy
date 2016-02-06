/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nars.nlp;

import javafx.scene.layout.VBox;
import nars.guifx.NARfx;
import nars.guifx.chart.Plot2D;

/**
 *
 * @author me
 */
public class GrammarTestGUI extends GrammarTest2 {

    private final Plot2D p, p2, p3;
    
    public GrammarTestGUI() throws Exception {
        super();
        
        
        p = new Plot2D(Plot2D.Line, 2000, 800, 200);
//        p.add("reward", () -> {
//            return reward;
//        });
        p.add("coherence", () -> {
            return tape.coherence;
        });
        
        p2 = new Plot2D(Plot2D.Line, 2000, 800, 200);
        p2.add("currentScore", () -> {
            return tape.prevScore;
        });

        p3 = new Plot2D(Plot2D.Line, 2000, 800, 200);
        p3.add("totalScore", () -> {
            return tape.totalScore;
        });
        
        NARfx.run(()-> {
            NARfx.newWindow("x",
                new VBox(p, p2, p3)
            );
        });

        n.onFrame(n -> {

        });
    }

    @Override
    protected void onStep() {
        super.onStep();
        updateGraphs();

    }

    public void updateGraphs() {
        p.update();
        p2.update();
        p3.update();
    }


    public static void main(String[] args) throws Exception {
        new GrammarTestGUI().run();
    }
    
}
