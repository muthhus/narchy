package nars.experiment.asteroids.gui.backgrounds;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import nars.experiment.asteroids.audio.MinimWrapper;
import nars.experiment.asteroids.gui.Background;
import nars.experiment.asteroids.gui.Viewer;
import java.lang.Math;

public class BlankBackground extends Background{
    public void drawBackground(Graphics g, Graphics2D g2d, double focusX, double focusY) {
        g.setColor(new Color(0,0,0));
        g.fillRect(0, 0, width, height);
    }
}
