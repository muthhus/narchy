package nars.guifx.demo.graph2;

import nars.guifx.demo.AbstractNARGraphDemo;
import nars.nar.Default;

/**
 * Created by me on 2/23/16.
 */
public class DemoNARGraph1 extends AbstractNARGraphDemo {

    public static void main(String[] args)  {

        Default n = new Default(1024,1,2,2);
        n.input("<a --> b>.");
        n.input("<b --> c>.");
        n.input("<c --> d>.");
        n.run(5);

        graphIDE(n);
    }
}
