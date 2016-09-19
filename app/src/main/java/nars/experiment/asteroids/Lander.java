//package nars.experiment.asteroids;
//
//import nars.NAR;
//import nars.NAgent;
//import nars.nar.Default;
//
///**
// * Created by me on 9/19/16.
// */
//public class Lander extends NAgent {
//
//    private final LanderFrame window;
//
//    float dt = 0.001f;
//
//    public Lander(NAR nar) {
//        super(nar, 4);
//
//        this.window = new LanderFrame();
//    }
//
//    @Override
//    protected float act() {
//
//        float leftright = (float) Math.random() - 0.5f;
//        float up = (float) Math.random();
//        window.act(dt, leftright, up);
//
//        return 0;
//    }
//
//    public static void main(String[] args) {
//        Default n = new Default();
//
//        Lander l = new Lander(n);
//        l.run(100000, 1);
//    }
//}
