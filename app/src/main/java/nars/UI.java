package nars;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import jcog.Util;
import nars.gui.Vis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.SpaceGraph;
import spacegraph.widget.console.ConsoleTerminal;

import java.io.IOException;

import static spacegraph.SpaceGraph.window;

/**
 * main UI entry point
 */
public class UI {

    static final Logger logger = LoggerFactory.getLogger(UI.class);

    public static void main(String[] args) throws IOException {

        NAR nar = NARchy.ui();

//        try {
//            //new NoteFS("/tmp/nal", nar);
//
//            InterNAR i = new InterNAR(nar, 8, 0);
//            i.recv.preAmp(0.1f);
//            i.runFPS(2);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


        nar.startFPS(5f);

        try {
            //1. try to open a Spacegraph openGL window
            logger.info("Starting Spacegraph UI");
            SpaceGraph.window(
                window(Vis.reflect(nar), 700, 600)
            );
            ConsoleTerminal ct = new ConsoleTerminal(
                    new TextUI(nar).session(8f)
            );
            SpaceGraph.window(
                    ct,800, 600);

            Util.pause(50); ct.term.addInput(KeyStroke.fromString("<pageup>")); //HACK trigger redraw

        } catch (Throwable t) {
            //2. if that fails:
            logger.info("Fallback to Terminal UI");
            DefaultTerminalFactory tf = new DefaultTerminalFactory();
            Terminal tt = tf.createTerminal();
            new TextUI(nar, tt, 8f);
        }



    }


}
