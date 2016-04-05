//package nars.op.java;
//
//import nars.NAR;
//import nars.nar.Default;
//import nars.util.Texts;
//import nars.util.data.Util;
//import nars.util.signal.MotorConcept;
//import nars.util.signal.SensorConcept;
//import objenome.goal.Observation;
//
//import javax.swing.*;
//import java.awt.*;
//import java.awt.geom.AffineTransform;
//import java.util.Observable;
//import java.util.Random;
//import java.util.concurrent.atomic.AtomicInteger;
//
///**
// * Created by me on 4/3/16.
// */
//public class MountainCar {
//
//
//    /**
//     * This class manages all of the problem parameters, current state variables,
//     * and state transition and reward dynamics.
//     *
//     * @author btanner
//     */
//    public static class MountainCarState {
//        private final MotorConcept motor;
////	Current State Information
//
//        private double position;
//        private double velocity;
//        private double reward;
//        private double rewardDelta;
//
//
//        public MountainCarState(NAR n) {
//
//
//            long randomSeed = 1;
//            boolean randomStartStates = false;
//
//            this.randomStarts = randomStartStates;
//            this.transitionNoise = 0;
//
//            if (randomSeed == 0) {
//                this.randomGenerator = new Random();
//            } else {
//                this.randomGenerator = new Random(randomSeed);
//            }
//
//            //Throw away the first few because they first bits are not that random.
//            randomGenerator.nextDouble();
//            randomGenerator.nextDouble();
//            reset();
//
//            new SensorConcept("car:position", n, () -> (float)position);
//            new SensorConcept("car:velocityPos", n, () -> (float)(velocity > 0 ? velocity : 0));
//            new SensorConcept("car:velocityNeg", n, () -> (float)(velocity < 0 ? -velocity : 0));
//
//            this.motor = new MotorConcept("car(motor)", n, mm -> {
//                return mm/2f + 0.5f;
//            });
//            new SensorConcept("car:good", n, () -> (float)rewardDelta);
//
//            CarOnMountainVizComponent viz = new CarOnMountainVizComponent();
//
//            n.onFrame(nn -> {
//                update();
//                viz.repaint();
//            });
//        }
//
//        public void update() {
//
//            //discretize motivation
//            float m = motor.motivation(nar);
//
//            int a;
//            if (m < -0.33f)
//                a = -1;
//            else if (m > 0.33f)
//                a = 1;
//            else
//                a = 0;
//
//            update(a);
//
//        }
//
//        //Some of these are fixed.  This environment would be easy to parameterize further by changing these.
//        final public double minPosition = -1.2;
//        final public double maxPosition = 0.6;
//        final public double minVelocity = -0.1; //0.07
//        final public double maxVelocity = 0.1;
//        final public double goalPosition = 0.5;
//        final public double accelerationFactor = 0.001;
//        final public double gravityFactor = -0.0025;
//        final public double hillPeakFrequency = 3.0;
//        //This is the middle of the valley (no slope)
//        final public double defaultInitPosition = -0.5d;
//        final public double defaultInitVelocity = 0.0d;
//        final public double rewardPerStep = -1.0d;
//        final public double rewardAtGoal = 1.0d;
//        final private Random randomGenerator;
//        //These are configurable
//        private boolean randomStarts = false;
//        private double transitionNoise = 0.0d;
//        private int lastAction = 0;
//
//
//        public double getPosition() {
//            return position;
//        }
//
//        public double getVelocity() {
//            return velocity;
//        }
//
//        /**
//         * Calculate the reward for the
//         * @return
//         */
//        public double getReward() {
//            if (inGoalRegion()) {
//                return rewardAtGoal;
//            } else {
//                //return rewardPerStep;
//                return getHeightAtPosition(this.position)/2f;
//            }
//        }
//
//        /**
//         * IS the agent past the goal marker?
//         * @return
//         */
//        public boolean inGoalRegion() {
//            return position >= goalPosition;
//        }
//
//        protected void reset() {
//            position = defaultInitPosition;
//            velocity = defaultInitVelocity;
//            if (randomStarts) {
//                //Dampened starting values
//                double randStartPosition = defaultInitPosition+.25d*(randomGenerator.nextDouble()-.5d);
//                position = randStartPosition;
//                double randStartVelocity = defaultInitVelocity+.025d*(randomGenerator.nextDouble()-.5d);
//                velocity = randStartVelocity;
//            }
//
//        }
//
//        /**
//         * Update the agent's velocity, threshold it, then
//         * update position and threshold it.
//         * @param a Should be in {0 (left), 1 (neutral), 2 (right)}
//         */
//        void update(int a) {
//            lastAction = a;
//            double acceleration = accelerationFactor;
//
//            //Noise should be at most
//            double thisNoise=2.0d*accelerationFactor*transitionNoise*(randomGenerator.nextDouble()-.5d);
//
//            velocity += (thisNoise+((a - 1)) * (acceleration)) + getSlope(position) * (gravityFactor);
//            if (velocity > maxVelocity) {
//                velocity = maxVelocity;
//            }
//            if (velocity < minVelocity) {
//                velocity = minVelocity;
//            }
//            position += velocity;
//            if (position > maxPosition) {
//                position = maxPosition;
//            }
//            if (position < minPosition) {
//                position = minPosition;
//            }
//            if (position == minPosition && velocity < 0) {
//                velocity = 0;
//            }
//
//            double currentReward = getReward();
//            rewardDelta = currentReward - reward;
//            this.reward = currentReward;
//
//        }
//
//        public int getLastAction() {
//            return lastAction;
//        }
//
//        /**
//         * Get the height of the hill at this position
//         * @param queryPosition
//         * @return
//         */
//        public double getHeightAtPosition(double queryPosition) {
//            return -Math.sin(hillPeakFrequency * (queryPosition));
//        }
//
//        /**
//         * Get the slop of the hill at this position
//         * @param queryPosition
//         * @return
//         */
//        public double getSlope(double queryPosition) {
//        /*The curve is generated by cos(hillPeakFrequency(x-pi/2)) so the
//         * pseudo-derivative is cos(hillPeakFrequency* x)
//         */
//            return Math.cos(hillPeakFrequency * queryPosition);
//        }
//
//        public void print() {
////            System.out.println(getReward() + "\t" +
////                    //"pos=" + position +
////                    "vel=" + velocity + "\tmotivation: " + motor.motivation(nar));
//        }
//
////        Observation makeObservation() {
////            Observation currentObs = new Observation(0, 2);
////
////            currentObs.doubleArray[0] = getPosition();
////            currentObs.doubleArray[1] = getVelocity();
////
////            return currentObs;
////
////        }
//
//
//        public class CarOnMountainVizComponent extends Canvas {
//
//            private boolean showAction = true;
////        private Image carImageNeutral = null;
////        private Image carImageLeft = null;
////        private Image carImageRight = null;
//
//            public CarOnMountainVizComponent() {
//                JFrame f = new JFrame("x");
//                JPanel bb = new JPanel(new BorderLayout());
//                bb.add(this, BorderLayout.CENTER);
//                f.setContentPane(bb);
//                f.setSize(500,500);
//                f.setVisible(true);
//
////            URL carImageNeutralURL = CarOnMountainVizComponent.class.getResource("/images/auto.png");
////            URL carImageLeftURL = CarOnMountainVizComponent.class.getResource("/images/auto_left.png");
////            URL carImageRightURL = CarOnMountainVizComponent.class.getResource("/images/auto_right.png");
////            try {
////                carImageNeutral = ImageIO.read(carImageNeutralURL);
////                carImageNeutral = carImageNeutral.getScaledInstance(50, 50, Image.SCALE_SMOOTH);
////                carImageLeft = ImageIO.read(carImageLeftURL);
////                carImageLeft = carImageLeft.getScaledInstance(50, 50, Image.SCALE_SMOOTH);
////                carImageRight = ImageIO.read(carImageRightURL);
////                carImageRight = carImageRight.getScaledInstance(50, 50, Image.SCALE_SMOOTH);
////            } catch (IOException ex) {
////                System.err.println("ERROR: Problem getting car image.");
////            }
//
//            }
//
//
//            @Override
//            public void paint(Graphics g) {
//                g.setColor(Color.RED);
//
////                double transX = UtilityShop.normalizeValue(this.mcv.getCurrentStateInDimension(0), minPosition, maxPosition);
//
////                //need to get he actual height ranges
////                double transY = UtilityShop.normalizeValue(
////                        this.mcv.getHeight(),
////                        mcv.getMinHeight(),
////                        mcv.getMaxHeight());
//                float transX = (float)Util.normalize(getPosition(), minPosition, maxPosition);
//                float transY = (float)Util.normalize(getHeightAtPosition(getPosition()), -1, 1);
//
//
//                transX *= 200.0d;
//                transY *= 200.0d;
//
//
//                //double theta = getSlope(getPs)* 1.25;
//
////                Image whichImageToDraw = carImageNeutral;
////                if (lastAction == 0) {
////                    whichImageToDraw = carImageLeft;
////                }
////                if (lastAction == 2) {
////                    whichImageToDraw = carImageRight;
////                }
//                /*AffineTransform theTransform = AffineTransform.getTranslateInstance(transX - whichImageToDraw.getWidth(null) / 2.0d, transY - whichImageToDraw.getHeight(null) / 2.0d);
//                theTransform.concatenate(AffineTransform.getRotateInstance(theta, whichImageToDraw.getWidth(null) / 2, whichImageToDraw.getHeight(null) / 2));*/
//                g.fillOval((int)transX, (int)transY, 10, 10);
//                //g.drawImage(whichImageToDraw, theTransform, null);
//
//
//
//            }
//
//
//        }
//
//    }
//
//    public static void main(String[] rags) {
//        Default n = new Default(1024, 10, 2, 4);
//
//
//        MountainCarState car = new MountainCarState(n);
//
//        n.log();
//
//        n.input("car:good! :|: %1.0;0.99%");
//        int cycles = 15000;
//        int trainTime = 32;
//        for (int i = 0; i < cycles; i++) {
//            if (i < trainTime) {
//                float f = (float)Math.random();
//                n.input("car(motor)! :|: %" + Texts.n2(f) + ";0.5%");
//            }
//            car.print();
//            n.step();
//        }
//
//    }
//}
