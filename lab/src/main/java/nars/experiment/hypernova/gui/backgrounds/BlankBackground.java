package nars.experiment.hypernova.gui.backgrounds;

import nars.experiment.hypernova.gui.Background;

import java.awt.*;

public class BlankBackground extends Background{
    public void drawBackground(Graphics g, Graphics2D g2d, double focusX, double focusY) {
        g.setColor(new Color(0,0,0));
        g.fillRect(0, 0, width, height);
    }
}
