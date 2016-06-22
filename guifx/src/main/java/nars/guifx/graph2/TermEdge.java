package nars.guifx.graph2;


//import scala.tools.nsc.doc.model.Object;


import nars.term.Termed;
import nars.util.Util;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * Created by me on 9/5/15.
 */
public abstract class TermEdge /*implements ChangeListener*/ {


    public static final TermEdge[] empty = new TermEdge[0];
    public final TermNode<Termed>
        aSrc, //source
        bSrc; //target

    private final int hash;



//    //public double len = 0.0;
//    public boolean visible = false;

    public final DescriptiveStatistics pri;
    public static final int PRI_WINDOW_SIZE = 6;



    public TermEdge(TermNode aSrc, TermNode bSrc) {

        pri = new DescriptiveStatistics();
        pri.setWindowSize(PRI_WINDOW_SIZE);


        //setAutoSizeChildren(true);

        this.hash = Util.hashCombine(aSrc.hashCode(), bSrc.hashCode());
        this.aSrc = aSrc;
        this.bSrc = bSrc;


        /*if (aSrc.term.term().compareTo(bSrc.term.term()) > 0) {
            throw new RuntimeException("invalid term order for TermEdge: " + aSrc + ' ' + bSrc);
        }*/


    }

    //public void delete() {
//            aSrc.localToSceneTransformProperty().removeListener(this);
//            bSrc.localToSceneTransformProperty().removeListener(this);
    //}

//        @Override
//        public void changed(ObservableValue observable, Object oldValue, Object newValue) {
//
//            changed.set(true);
//
//        }

    //        private void setA(TermNode aSrc) {
//            this.aSrc = aSrc;
//            a.setVisible(aSrc!=null);
//        }
//
//        private void setB(TermNode bSrc) {
//            this.bSrc = bSrc;
//            b.setVisible(bSrc!=null);
//        }

//        /** fx thread */
//        public boolean OLDrender() {
//
//            //changed.set(false);
//
//
//            if (!aSrc.isVisible() || !bSrc.isVisible()) {
//                setVisible(false);
//                return false;
//            }
//
//            double x1 = aSrc.x();// + fw / 2d;
//            double y1 = aSrc.y();// + fh / 2d;
//            double x2 = bSrc.x();// + tw / 2d;
//            double y2 = bSrc.y();// + th / 2d;
//            double dx = (x1 - x2);
//            double dy = (y1 - y2);
//            this.len = Math.sqrt(dx * dx + dy * dy);
//            //len-=fw/2;
//
//            //double rot = Math.atan2(dy, dx);
//            double rot = FastMath.atan2(dy, dx);
//            double cx = 0.5f * (x1 + x2);
//            double cy = 0.5f * (y1 + y2);
//
//
//            translate.setX(cx);
//            translate.setY(cy);
//            rotate.setAngle(FastMath.toDegrees(rot));
//            scale.setX(len);
//            scale.setY(len);
//
//
//            return a.update() || b.update();
//        }

//    public final TermNode otherNode(TermNode x) {
//        if (aSrc == x) return bSrc;
//        return aSrc;
//    }

    public abstract double getWeight();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        //if (o == null || getClass() != o.getClass()) return false;

        TermEdge termEdge = (TermEdge) o;

        return aSrc.equals(termEdge.aSrc) && bSrc.equals(termEdge.bSrc);

    }

    @Override
    public final int hashCode() {
        return hash;
    }
}
