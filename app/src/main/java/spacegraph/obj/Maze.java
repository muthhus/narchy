package spacegraph.obj;

import nars.util.data.random.XorShift128PlusRandom;
import spacegraph.Spatial;
import spacegraph.phys.Collidable;
import spacegraph.phys.constraint.TypedConstraint;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by me on 8/12/16.
 */
public class Maze extends Spatial {

    boolean[][] cells;

    public Maze(int x, int y) {
        cells = new boolean[x][y];
        build(0,0,x-1,y-1);
        for (boolean[] c : cells) {
            for (boolean cc : c) {
                System.out.print(cc ? 'X' : ' ');
            }
            System.out.println();
        }
    }

    public static void main(String[] ags) {
        new Maze(10,75);
    }

    public static int irand(int x) {
        return (int)(Math.random() * x);
    }

        public void build(int x1, int y1, int x2, int y2) {

            for (int x = x1; x < x2; x++)
                for (int y = y1; y < y2; y++)
                    cells[x][y] = true;

            int w = x2 - x1 + 1;
            int rw = (w + 1) / 2;
            int h = y2 - y1 + 1;
            int rh = (h + 1) / 2;

            int sx = x1 + 2 * irand(rw);
            int sy = y1 + 2 * irand(rh);
            cells[sx][sy] = false; //start point

            int finishedCount = 0;
            for (int i = 1; (i < (rw * rh * 1000)) && (finishedCount < (rw * rh)); i++) {
                int x = x1 + 2 * irand(rw);
                int y = y1 + 2 * irand(rh);
                if (cells[x][y] != true)
                    continue;

                int dx = (irand(2) == 1) ? (irand(2) * 2 - 1) : 0;
                int dy = (dx == 0) ? (irand(2) * 2 - 1) : 0;
                int lx = x + dx * 2;
                int ly = y + dy * 2;
                if ((lx >= x1) && (lx <= x2) && (ly >= y1) && (ly <= y2)) {
                    if (!cells[lx][ly]) {
                        cells[x][y] = false;
                        cells[x+dx][y+dy] = false;
                        finishedCount++;
                    }
                }
            }
        }

    @Override
    public List<Collidable> bodies() {
        return null;
    }

    @Override
    public List<TypedConstraint> constraints() {
        return null;
    }

    @Override
    public void accept(Object o, Object o2) {

    }
}
