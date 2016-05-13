package nars.rover.obj;

import com.artemis.Component;
import nars.rover.physics.gl.JoglAbstractDraw;
import nars.rover.physics.j2d.LayerDraw;
import nars.rover.util.SimplexNoise;
import nars.rover.world.Cell;
import nars.util.data.random.XorShift128PlusRandom;
import org.jbox2d.dynamics.World2D;

import java.lang.reflect.Array;
import java.util.Random;

/**
 * Created by me on 3/30/16.
 */
public class Grid2D extends Component implements LayerDraw {

    Cell[][] cells;
    private int w, h;
    private JoglAbstractDraw draw;
    float cw, ch;

    public Grid2D() {

    }
    public Grid2D(int w, int h, float scale /* meters per cell */) {
        this.cells = (Cell[][]) Array.newInstance(Cell.class, w, h);
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                cells[i][j] = new Cell(new Cell.CellState(i, j));
            }
        }
        this.w = w;
        this.h = h;
        cw = ch = scale;
    }

    @FunctionalInterface
    public interface CellVisitor {
        void cell(Cell c, float px, float py);
    }

    public void forEach(int x0, int y0, int x1, int y1, CellVisitor v, boolean onlyVisible) {
        final float worldWidth = cw * w;
        final float worldHeight = ch * w;
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                Cell c = cells[x][y];
                if (c!=null) {
                    float px = x * cw - worldWidth / 2f;
                    float py = -(y * ch) + worldHeight / 2f;
                    v.cell(c, px, py);
                }
            }
        }
    }
    public void forEach(CellVisitor v, boolean onlyVisible) {
        forEach(0, 0, w, h, v, onlyVisible);
    }

    public static Grid2D newMazePlanet(int w, int h, float scale) {
        int water_threshold = 30;
        Grid2D cells = new Grid2D(w, h, scale);

        cells.forEach(0, 0, w, h, (c,px,py) -> {
///c.setHeight((int)(Math.random() * 12 + 1));
            float smoothness = 20f;

            double n = SimplexNoise.noise(c.state.x / smoothness, c.state.y / smoothness);
            if ((n * 64) > water_threshold) {
                c.material = Cell.Material.Water;
            } else {
                c.material = Cell.Material.GrassFloor;
            }
            c.setHeight((int) (Math.random() * 24 + 1));
        }, false);


        //Maze.buildMaze(cells, 3, 3, 23, 23);

        return cells;
    }


    final CellVisitor groundDrawer = new CellVisitor() {

        final Random rng = new XorShift128PlusRandom(1);

        @Override public void cell(Cell c, float px, float py) {

            float h = c.height;


            float z = h/5f - 5;

            switch (c.material) {
                case DirtFloor:
                    draw.drawSolidRect(px, py, cw, ch, z, 0.5f,c.height*0.01f, c.height*0.01f);
                    break;
                case StoneWall:
                    draw.drawSolidRect(px, py, cw, ch, z, 0.75f, 0.75f, 0.75f);
                    break;
                case GrassFloor:
                    draw.drawSolidRect(px, py, cw, ch, z, 0.1f,0.5f + c.height*0.005f, 0.1f);
                    break;
                case Water:
                    float db = rng.nextFloat() *0.04f;


                    float b = 0.5f + h*0.01f - db;
                    if (b < 0) b = 0; if (b > 1.0f) b = 1.0f;


                    draw.drawSolidRect(px, py, cw, ch, z, 0.1f,0.1f,b);
                    break;
            }
        }
    };


    @Override
    public void drawGround(JoglAbstractDraw draw, World2D w) {

    }

    @Override
    public void drawSky(JoglAbstractDraw draw, World2D w) {
        this.draw = draw;
        //this.graphics = draw.getGraphics();
        forEach(groundDrawer, true);
    }

    /** https://github.com/opennars/opennars/tree/bigpicture2/nars_lab/src/main/java/nars/testchamber/map*/
    public static class Maze {

//
//        public static void buildMaze(Grid2D m, int x1, int y1, int x2, int y2) {
//            m.forEach(x1, y1, x2, y2, (c,px,py)->{
//                //new SetMaterial(Material.StoneWall)
//            }, false);
//            buildInnerMaze(m, x1 + 1, y1 + 1, x2 - 1, y2 - 1);
//            //m.copyReadToWrite();
//        }
//
//        public static void buildInnerMaze(Grid2D m, int x1, int y1, int x2, int y2) {
//
//            m.forEach(x1, y1, x2, y2, (c, x, y) -> {
//                //new SetMaterial(Material.StoneWall)
//
//            }, false);
//
//            int w = x2 - x1 + 1;
//            int rw = (w + 1) / 2;
//            int h = y2 - y1 + 1;
//            int rh = (h + 1) / 2;
//
//            int sx = x1 + 2 * irand(rw);
//            int sy = y1 + 2 * irand(rh);
//            m.cells[sx][sy].
//            m.at(sx, sy, new SetMaterial(Material.DirtFloor));
//
//            int finishedCount = 0;
//            for (int i = 1; (i < (rw * rh * 1000)) && (finishedCount < (rw * rh)); i++) {
//                int x = x1 + 2 * irand(rw);
//                int y = y1 + 2 * irand(rh);
//                if (m.at(x, y).material != Material.StoneWall)
//                    continue;
//
//                int dx = (irand(2) == 1) ? (irand(2) * 2 - 1) : 0;
//                int dy = (dx == 0) ? (irand(2) * 2 - 1) : 0;
//                int lx = x + dx * 2;
//                int ly = y + dy * 2;
//                if ((lx >= x1) && (lx <= x2) && (ly >= y1) && (ly <= y2)) {
//                    if (m.at(lx, ly).material != Material.StoneWall) {
//                        m.at(x, y, new SetMaterial(Material.DirtFloor));
//                        m.at(x + dx, y + dy, new SetMaterial(Material.DirtFloor));
//                        m.readCells[x][y].setHeight((int) (Math.random() * 24 + 1));
//                        m.writeCells[x][y].setHeight((int) (Math.random() * 24 + 1));
//                        finishedCount++;
//                    }
//                }
//            }
//        }
    }
}
