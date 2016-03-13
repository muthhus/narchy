package nars.rover;

import nars.Global;
import nars.rover.physics.Display;
import nars.rover.physics.TestbedSetting;
import nars.rover.physics.TestbedSettings;
import nars.rover.physics.TestbedState;
import nars.rover.physics.gl.JoglAbstractDraw;
import nars.rover.physics.j2d.LayerDraw;
import nars.rover.robot.Being;
import nars.time.SimulatedClock;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;

import java.util.List;

/**
 * NARS Rover
 *
 * @author me
 */
public class Sim extends PhysicsModel {


    final static int angleBits = 3;
    final static int angleResolution = 1 << angleBits;

    private static final double TWO_PI = 2 * Math.PI;
    static String[] angleTerms = new String[angleResolution];
    public final SimulatedClock clock;
    public final List<Being> robots = Global.newArrayList();
    /* how often to input mission, in frames */
    public int missionPeriod = 32;
    public World world;
    //PhysicsRun phy = new PhysicsRun(10, this);


//        //new NARPrologMirror(nar,0.75f, true).temporal(true, true);
//        //ItemCounter removedConcepts = new ItemCounter(nar, Events.ConceptForget.class);
//        // RoverWorld.world= new RoverWorld(rv, 48, 48);
//        new NARPhysics<Rover2>(1.0f / framesPerSecond, theRover ) {
//
//            @Override
//            public void init() {
//                super.init();
//
//
//            }
//
//            @Override
//            public void frame() {
//                super.frame();
//
//
//
//            }
//
//
//            @Override
//            public void keyPressed(KeyEvent e) {
//
////                if (e.getKeyChar() == 'm') {
////                    theRover.mission = (theRover.mission + 1) % 2;
////                    System.out.println("Mission: " + theRover.mission);
////                } else if (e.getKeyChar() == 'g') {
////                    System.out.println(nar.memory.cycle);
////                    //removedConcepts.report(System.out);
////                }
//
////                if (e.getKeyCode() == KeyEvent.VK_UP) {
////                    if(!Rover2.allow_imitate) {
////                        nar.addInput("motor(linear,1). :|:");
////                    } else {
////                        nar.addInput("motor(linear,1)!");
////                    }
////                }
////                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
////                    if(!Rover2.allow_imitate) {
////                        nar.addInput("motor(linear,-1). :|:");
////                    } else {
////                        nar.addInput("motor(linear,-1)!");
////                    }
////                }
////                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
////                    if(!Rover2.allow_imitate) {
////                        nar.addInput("motor(turn,-1). :|:");
////                    } else {
////                        nar.addInput("motor(turn,-1)!");
////                    }
////                }
////                if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
////                    if(!Rover2.allow_imitate) {
////                        nar.addInput("motor(turn,1). :|:");
////                    } else {
////                        nar.addInput("motor(turn,1)!");
////                    }
////                }
//            }
//
//
//
//
//        };
    private long delayMS;
    private float fps;
    private boolean running = false;
    final PhysicsRun runner;

    public Sim(World world, SimulatedClock clock) {
        this.clock = clock;
        this.world = world;


        runner = new PhysicsRun(world, 30f, this);


        init(world);

//        cycle();
//        protected final void cycle() {
//            phy.cycle(fps);
//        }

    }

//    public static double normalizeAngle(final double theta) {
//        double normalized = theta % TWO_PI;
//        normalized = (normalized + TWO_PI) % TWO_PI;
//        if (normalized > Math.PI) {
//            normalized -= TWO_PI;
//        }
//        if (normalized < 0) {
//            normalized += TWO_PI;
//        }
//        return normalized;
//    }

//    public static String angleTerm(final float a) {
//        float h = (float) normalizeAngle(a);
//        h /= MathUtils.PI * 2.0f;
//        int i = (int) (h * angleResolution / 1f);
//        if (i == angleResolution) i = 0; //wraparound and eliminate i=angleResolution case
//
//        //final int ha = angleResolution;
//
////        if (i == 0) {
////            t = "forward";
////        } else if (i == angleResolution / 4) {
////            t = "left";
////        } else if (i == -angleResolution / 4) {
////            t = "right";
////        } else if ((i == (angleResolution / 2 - 1)) || (i == -(angleResolution / 2 - 1))) {
////            t = "reverse";
////        } else {
//
//
//        if (angleTerms[i] == null) {
//            angleTerms[i] = "ang" + i;
//
//            //angleTerms[i] = intToBitSet.the(i).toString();
//
////
////                String s;
////
////                if (i == 0) s = "(forward, 0)"; //center is special
////                else {
////                    if (i > angleResolution/2) i = -(angleResolution/2 - i);
////                    s = "(" + ((i < 0) ? "left" : "right") + ',' + Math.abs(i) + ")";
////                }
////
////                angleTerms[i+ha] = s;
//        }
//
//        //}
//
//        return angleTerms[i];
//    }
//
//    /**
//     * maps a value (which must be in range 0..1.0) to a term name
//     */
//    public static String f5(double p) {
//        if (p < 0) {
//            throw new RuntimeException("Invalid value for: " + p);
//        }
//        if (p > 0.99f) {
//            p = 0.99f;
//        }
//        int i = (int) (p * 10f);
//        switch (i) {
//            case 9:
//                return "5";
//            case 8:
//            case 7:
//                return "4";
//            case 6:
//            case 5:
//                return "3";
//            case 4:
//            case 3:
//                return "2";
//            case 2:
//            case 1:
//                return "1";
//            default:
//                return "0";
//        }
//    }

    public static String f(double p) {
        if (p < 0) {
            throw new RuntimeException("Invalid value for: " + p);
            //p = 0;
        }
        if (p > 0.99f) {
            p = 0.99f;
        }
        int i = (int) (p * 10f);
        return String.valueOf(i);
    }

    public void setFPS(float f) {
        this.fps = f;

        delayMS = (long) (1000f / fps);
    }

    public void run(float fps) {
        setFPS(fps);


        float dt = 1f/fps;

        running = true;
        while (running) {

            cycle(dt);

            try {
                Thread.sleep(delayMS);
            } catch (InterruptedException e) {
            }
        }

    }

    public void stop() {
        running = false;
    }

    public void add(Being r) {
        r.init(this);
        robots.add(r);
    }

    public void remove(Body r) {
        getWorld().destroyBody(r);
    }

    @Override
    public BodyDef bodyDefCallback(BodyDef body) {
        return body;
    }

    @Override
    public FixtureDef fixDefCallback(FixtureDef fixture) {
        return fixture;
    }

//    @Deprecated @Override
//    public void step(float timeStep, TestbedSettings settings, Display panel) {
//
//
//        super.step(timeStep, settings, panel);
//
//        cycle();
//
//    }

    public void cycle(float dt) {

        TestbedSettings settings = runner.model.settings;
        world.step(dt,
                settings.getSetting(TestbedSettings.VelocityIterations).value,
                settings.getSetting(TestbedSettings.PositionIterations).value);



        for (int i = 0, robotsSize = robots.size(); i < robotsSize; i++) {
            Being r = robots.get(i);
            r.step(1);
        }

        clock.add(1);
    }

    @Override
    public void initTest(boolean deserialized) {


        getWorld().setGravity(new Vec2());
        getWorld().setAllowSleep(false);

    }


    @Override
    public String getTestName() {
        return "Disposabuild";
    }



//    public class RoverPanel extends JPanel {
//
//        public class InputButton extends JButton implements ActionListener {
//
//            private final String command;
//
//            public InputButton(String label, String command) {
//                super(label);
//                addActionListener(this);
//                //this.addKeyListener(this);
//                this.command = command;
//            }
//
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                nar.input(command);
//            }
//
//        }
//
//        public RoverPanel(RoverModel rover) {
//            super(new BorderLayout());
//
//            {
//                JPanel motorPanel = new JPanel(new GridLayout(0, 2));
//
////                motorPanel.add(new InputButton("Stop", "motor(stop). :|:"));
////                motorPanel.add(new InputButton("Forward", "motor(forward). :|:"));
////                motorPanel.add(new InputButton("TurnLeft", "motor(turn,left). :|:"));
////                motorPanel.add(new InputButton("TurnRight", "motor(turn,right). :|:"));
////                motorPanel.add(new InputButton("Backward", "motor(backward). :|:"));
//                add(motorPanel, BorderLayout.SOUTH);
//            }
//        }
//
//    }
//

    public interface Edible {

    }

    public static class FoodMaterial extends Material implements Edible {

        static final Color3f foodFill = new Color3f(0.15f, 0.15f, 0.6f);

        @Override
        public void before(Body b, JoglAbstractDraw d, float time) {

            d.setFillColor(foodFill);
        }

        @Override
        public String toString() {
            return "food";
        }
    }

    public static class WallMaterial extends Material {
        static final Color3f wallFill = new Color3f(0.5f, 0.5f, 0.5f);

        @Override
        public void before(Body b, JoglAbstractDraw d, float time) {

            d.setFillColor(wallFill);
        }

        @Override
        public String toString() {
            return "wall";
        }
    }

    public static final Color3f poisonFill = new Color3f(0.45f, 0.15f, 0.15f);

    public static class PoisonMaterial extends Material implements Edible {


        @Override
        public void before(Body b, JoglAbstractDraw d, float time) {
            d.setFillColor(poisonFill);
        }

        @Override
        public String toString() {
            return "poison";
        }
    }

}
