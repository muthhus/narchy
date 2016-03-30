///*
// * Here comes the text of your license
// * Each line should be prefixed with  *
// */
//package nars.rover.world;
//
//import nars.rover.obj.Grid2D;
//import nars.rover.physics.gl.JoglAbstractDraw;
//
//import nars.rover.physics.j2d.LayerDraw;
//import nars.util.data.random.XorShift128PlusRandom;
//
//
//import java.util.Random;
//
///**
// *
// * @author me
// */
//public class GridSpaceWorld extends RoverWorld implements LayerDraw {
//
//
//
//    private final Grid2D grid;
//    private final int w;
//    private final int h;
//    private final float cw;
//    private final float ch;
//    private final float worldWidth;
//    private final float worldHeight;
//    private JoglAbstractDraw draw;
//
//
////    @Override
////    public void init(PhysicsModel p) {
////        super.init(p);
////
////        ((JoglAbstractDraw)p.draw()).addLayer(this);
////
////        cells((c, px, py) -> {
////            switch (c.material) {
////                case StoneWall:
////                    Body w1 = addWall(px, py, cw/2f, ch/2f, 0f);
////                    w1.setUserData(new SwingDraw.DrawProperty() {
////
////                        @Override
////                        public void before(Body b, SwingDraw d) {
////                            d.setFillColor(null);
////                            d.setStroke(null);
////                        }
////
////                        @Override public String toString() {
////                            return "wall";
////                        }
////
////                    });
////                    break;
////            }
////        }, false);
////
////    }
//
//
//
////    @Override
////    public void drawSky(JoglAbstractDraw draw, World2D w) {
////
////    }
////
////
////    public static Grid2D newMazePlanet() {
////        int w = 40;
////        int h = 40;
////        int water_threshold = 30;
////        Grid2D cells = new Grid2D(w, h);
////
////        cells.forEach(0, 0, w, h, c -> {
///////c.setHeight((int)(Math.random() * 12 + 1));
////            float smoothness = 20f;
////            c.material = Cell.Material.GrassFloor;
////            double n = SimplexNoise.noise(c.state.x / smoothness, c.state.y / smoothness);
////            if ((n * 64) > water_threshold) {
////                c.material = Cell.Material.Water;
////            }
////            c.setHeight((int) (Math.random() * 24 + 1));
////        });
////
////        Maze.buildMaze(cells, 3, 3, 23, 23);
////
////        return new Grid2D(cells);
////    }
// }
