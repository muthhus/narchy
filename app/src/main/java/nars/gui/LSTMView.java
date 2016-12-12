package nars.gui;

import jcog.Util;
import jcog.data.random.XorShift128PlusRandom;
import nars.learn.lstm.DistractedSequenceRecall;
import nars.learn.lstm.SimpleLSTM;
import spacegraph.space.layout.Grid;
import spacegraph.space.widget.MatrixView;

import java.util.Random;

import static spacegraph.SpaceGraph.window;

/**
 * Created by me on 11/22/16.
 */
public class LSTMView extends Grid {

    public static final MatrixView.ViewFunction1D colorize = (x, gl) -> {
        x = x/2f + 0.5f;
        //colorPolarized(gl, x);
        gl.glColor3f(0,x, x/2);
        return 0;
    };
    public static final MatrixView.ViewFunction1D colorize1 = (x, gl) -> {
        x = x/2f + 0.5f;
        gl.glColor3f(x,x/2, 0);
        return 0;
    };
    public static final MatrixView.ViewFunction1D colorize2 = (x, gl) -> {
        x = x/2f + 0.5f;
        gl.glColor3f(x,0,x/2);
        return 0;
    };
    public LSTMView(SimpleLSTM lstm) {
        super(
            new MatrixView(lstm.in, 8, colorize),
            new MatrixView(lstm.full_hidden, 4, colorize1),
            new MatrixView(lstm.out, 4, colorize2)
        );
    }

    public static void main(String[] arg) {


        Random r = new XorShift128PlusRandom(1234);

        DistractedSequenceRecall task = new DistractedSequenceRecall(r, 32, 8, 22, 100);

        int cell_blocks = 9;
        double learningRate = 0.05;
        SimpleLSTM lstm = task.lstm(cell_blocks);


        //initialize
        task.scoreSupervised(lstm);

        window(new LSTMView(lstm), 800, 800);

        int epochs = 5000;
        for (int epoch = 0; epoch < epochs; epoch++) {
            double fit = task.scoreSupervised(lstm);
            if (epoch % 10 == 0)
                System.out.println("["+epoch+"] error = " + (1 - fit));
            Util.sleep(1);
        }
        System.out.println("done.");
    }
}
