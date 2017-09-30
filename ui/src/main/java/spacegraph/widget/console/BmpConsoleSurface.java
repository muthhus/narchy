package spacegraph.widget.console;

import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.jogamp.opengl.GL2;
import spacegraph.video.TextureSurface;

abstract public class BmpConsoleSurface extends AbstractConsoleSurface {

    final TextureSurface ts = new TextureSurface();

    public BmpConsoleSurface(int columns, int rows) {
        super();
        resize(columns, rows);
    }

    @Override
    public void paint(GL2 gl) {

    }

}
