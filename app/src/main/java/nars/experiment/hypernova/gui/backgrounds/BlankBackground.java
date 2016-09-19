package nars.experiment.hypernova.gui.backgrounds;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import nars.experiment.hypernova.gui.Background;

public class BlankBackground extends Background{
    public void drawBackground(Graphics g, Graphics2D g2d, double focusX, double focusY) {
        g.setColor(new Color(0,0,0));
        g.fillRect(0, 0, width, height);
    }
}
