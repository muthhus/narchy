package nars.guifx.graph2.impl;

import com.github.sarxos.webcam.WebcamPanel;
import javafx.scene.DepthTest;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import nars.guifx.graph2.EdgeRenderer;
import nars.guifx.graph2.TermEdge;
import nars.guifx.graph2.TermNode;
import nars.guifx.graph2.scene.DefaultNodeVis;
import nars.guifx.graph2.source.SpaceGrapher;
import nars.guifx.util.ColorMatrix;
import nars.term.Termed;

/** TODO not finished */
public class MeshEdgeRenderer implements EdgeRenderer<TermEdge> {


    //    ColorArray colors = new ColorArray(
//            32,
//            Color.BLUE,
//            Color.GREEN
//    );
    public static final ColorMatrix colors = DefaultNodeVis.colors; /*new ColorMatrix(24,24,

        (pri,termTaskBalance) -> {
            return Color.hsb(30 + 120.0 * termTaskBalance, 0.75, 0.35 + 0.5 * pri);
        }
    );*/

    private double tx;
    private double ty;
    private double s;



//    //for iterative auto-normalization
//    public double maxPri = 1;
//    public double minPri = 0;

    double minWidth = 7;
    double maxWidth = 15;
    private MeshView floorMesh;
    private MeshRectangleDemo.Shape3DRectangle mesh;

    @Override
    public void accept(TermEdge i) {

        TermNode aSrc = i.aSrc;
        if (!aSrc.visible()) {
            //i.visible = false;
            return;
        }

        TermNode bSrc = i.bSrc;
        if (!bSrc.visible()) {
            //i.visible = false;
            return;
        }

        /*boolean aVis = a.update(), bVis = b.update();
        visible = (aVis || bVis);
        if (!visible) return false;*/


        double tx = this.tx;
        double s = this.s;
        double x1 = s*(tx+aSrc.x());// + fw / 2d;
        double ty = this.ty;
        double y1 = s*(ty+aSrc.y());// + fh / 2d;
        double x2 = s*(tx+bSrc.x());// + tw / 2d;
        double y2 = s*(ty+bSrc.y());// + th / 2d;

        draw(i, aSrc, bSrc, x1, y1, x2, y2);

    }

    public void draw(TermEdge i, TermNode<Termed> aSrc, TermNode<Termed> bSrc, double x1, double y1, double x2, double y2) {

    }


    public class EdgeTri extends TriangleMesh {

        public EdgeTri(float Width, float Height) {
            float[] points = {
                    -Width/2,  Height/2, 0, // idx p0
                    -Width/2, -Height/2, 0, // idx p1
                    Width/2,  Height/2, 0, // idx p2
                    Width/2, -Height/2, 0  // idx p3
            };
            float[] texCoords = {
                    1, 1, // idx t0
                    1, 0, // idx t1
                    0, 1, // idx t2
                    0, 0  // idx t3
            };
            /**
             * points:
             * 1      3
             *  -------   texture:
             *  |\    |  1,1    1,0
             *  | \   |    -------
             *  |  \  |    |     |
             *  |   \ |    |     |
             *  |    \|    -------
             *  -------  0,1    0,0
             * 0      2
             *
             * texture[3] 0,0 maps to vertex 2
             * texture[2] 0,1 maps to vertex 0
             * texture[0] 1,1 maps to vertex 1
             * texture[1] 1,0 maps to vertex 3
             *
             * Two triangles define rectangular faces:
             * p0, t0, p1, t1, p2, t2 // First triangle of a textured rectangle
             * p0, t0, p2, t2, p3, t3 // Second triangle of a textured rectangle
             */

// if you use the co-ordinates as defined in the above comment, it will be all messed up
//            int[] faces = {
//                    0, 0, 1, 1, 2, 2,
//                    0, 0, 2, 2, 3, 3
//            };

// try defining faces in a counter-clockwise order to see what the difference is.
            int[] faces = {
                    2, 2, 1, 1, 0, 0,
                    2, 2, 3, 3, 1, 1
            };

// try defining faces in a clockwise order to see what the difference is.
//            int[] faces = {
//                    2, 3, 0, 2, 1, 0,
//                    2, 3, 1, 0, 3, 1
//            };

            this.getPoints().setAll(points);
            this.getTexCoords().setAll(texCoords);
            this.getFaces().setAll(faces);
        }
    }

//    public double normalize(final double p) {
//        double maxPri = this.maxPri, minPri = this.minPri;
//
//        if (minPri > p)
//            this.minPri = minPri = p;
//
//        if (maxPri < p)
//            this.maxPri = maxPri = p;
//
//        if (maxPri == minPri) return p;
//
//        return (p - minPri) / (maxPri - minPri);
//    }

    @Override
    public synchronized void reset(SpaceGrapher g) {

        Scene scene = g.getScene();
        if (scene == null) return;

        if (floorMesh == null) {

            mesh = new MeshRectangleDemo.Shape3DRectangle(600, 600);
            floorMesh = new MeshView(mesh);//g.widthProperty(), g.heightProperty());
            floorMesh.setMaterial(new PhongMaterial(Color.GRAY));
            //floorMesh.setRotationAxis(Rotate.Y_AXIS);

            floorMesh.setManaged(false);

            //floorMesh.setTranslateX(250);
            //floorMesh.setTranslateY(250);
// try commenting this line out to see what it's effect is . . .
            floorMesh.setCullFace(CullFace.BACK);
            floorMesh.setDepthTest(DepthTest.DISABLE);
            //floorMesh.setDrawMode(DrawMode.FILL);

            System.out.println(floorMesh.getLocalToSceneTransform());
            System.out.println(floorMesh.getBoundsInParent());


//            floorCanvas.setCacheHint(CacheHint.SPEED);
//            floorCanvas.setCache(true);

//            PerspectiveCamera perspectiveCamera = new PerspectiveCamera(false);
//            scene.setCamera(perspectiveCamera);

            g.getChildren().
                    add(0, floorMesh); //underneath, background must be transparent to see


            g.getChildren().remove(1); //wtf?
            System.out.println(g.getChildren());

        }
        else {

        }




        //double w = scene.getWidth(); //g.getWidth();
        //double h = scene.getHeight();

        tx = g.translate.getX();
        ty = g.translate.getY();
        s = g.scale.getX();

        floorMesh.setLayoutX(tx*s);
        floorMesh.setLayoutY(ty*s);
        floorMesh.setScaleX(s);
        floorMesh.setScaleY(s);
        //floorMesh.setScaleZ(s);

        clear(g.getWidth(), g.getHeight());

        //unnormalize(0.1);
    }

    protected void clear(double w, double h) {
        clearTotally(w, h);
    }

    protected final void clearTotally(double w, double h) {

    }

}
