package nars.experiment;

import jcog.Util;
import nars.*;
import nars.concept.GoalActionAsyncConcept;
import nars.concept.ScalarConcepts;
import nars.gui.Vis;
import nars.op.video.Scale;
import nars.util.signal.CameraSensor;
import org.apache.commons.math3.util.MathUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import static spacegraph.SpaceGraph.window;

/**
 * Created by me on 3/21/17.
 */
public class FZero extends NAgentX {

    private final FZeroGame fz;

    float fwdSpeed = 9;
    float rotSpeed = 0.1f;

    public static void main(String[] args) {

        float fps = 20f;

        NAgentX.runRT((n) -> {

            FZero a = null;
            try {
                n.freqResolution.set(0.05);
                n.confResolution.set(0.01f);
                a = new FZero(n);
                a.happy.resolution(0.1f);

            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }
            a.trace = true;

            return a;

        }, fps);


    }

    public FZero(NAR nar) throws Narsese.NarseseException {
        super("fz", nar);

        this.fz = new FZeroGame();

        CameraSensor<Scale> c = senseCamera(id, new Scale(() -> fz.image,
                32, 24)/*.blur()*/).resolution(0.05f);
//        CameraSensor<Scale> c = senseCameraReduced(id, new Scale(() -> fz.image,
//                128, 64), 8, 8, 2, 2).resolution(0.1f);

//        PixelBag cc = PixelBag.of(()->fz.image, 32, 24);
//        cc.addActions($.the("fz"), this, false, false, true);
//        CameraSensor<PixelBag> sc = senseCamera("fz" /*"(nario,local)"*/, cc)
//                .resolution(0.05f);


        //initToggle();
        initBipolar();

//        actionUnipolar(p("left"), (r) -> {
//            //if (r > 0.5f)
//                fz.playerAngle -= (r) * rotSpeed;
//            return r;
//        });//.resolution.setValue(0.01f);
//        actionUnipolar(p("right"), (r) -> {
//            //if (r > 0.5f)
//                fz.playerAngle += (r) * rotSpeed;
//            return r;
//        });//.resolution.setValue(0.01f);


        //yaw stabilizer (eternal goal)
//        nar.goal(p($.the("x"), $.the("\"+\"")), 0.5f, 0.1f);
//        nar.goal(p($.the("x"), $.the("\"-\"")), 0.5f, 0.1f);

        //keyboard-ish controls:
//actionToggle($.inh(Atomic.the("fwd"),id), (b)-> fz.thrust = b );
//        actionTriState($.inh(Atomic.the("rot"), id ), (dh) -> {
//            switch (dh) {
//                case +1: fz.left = false; fz.right = true; break;
//                case 0: fz.left = fz.right = false; break;
//                case -1: fz.left = true; fz.right = false; break;
//            }
//        });

//        senseNumberDifference($.inh(the("joy"), id), happy).resolution.setValue(0.02f);
        senseNumberDifference($.inh($.the("angVel"), id), () -> (float) fz.playerAngle).resolution(0.02f);
        senseNumberDifference($.inh($.the("accel"), id), () -> (float) fz.vehicleMetrics[0][6]).resolution(0.02f);
        @NotNull ScalarConcepts ang = senseNumber($.the("ang"), () ->
                        (float) (0.5f + 0.5f * MathUtils.normalizeAngle(fz.playerAngle, 0) / (Math.PI)),
                11,
                ScalarConcepts.Needle
                //ScalarConcepts.Fluid
        ).resolution(1f);
        window(
                Vis.conceptBeliefPlots(this, ang, 16), 300, 300);

        //nar.mix.stream("Derive").setValue(1);

//        AgentService p = new AgentService.AgentBuilder(
//                //DQN::new,
//                HaiQAgent::new,
//                //() -> Util.tanhFast(a.dexterity())) //reward function
//                () -> dexterity() * Util.tanhFast(rewardCurrent) /* - lag */) //reward function
//
//                .in(this::dexterity)
//                .in(new FloatNormalized(() -> rewardCurrent).relax(0.01f))
//                .in(new FloatNormalized(
//                        ((Emotivation) nar.emotion).cycleDTRealMean::getValue)
//                        .relax(0.01f)
//                ).in(new FloatNormalized(
//                                () -> nar.emotion.busyVol.getSum()
//                        ).relax(0.01f)
//                ).out(
//                        new StepController((x) -> c.in.preAmp(x), 0, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f)
//                ).out(
//                        new StepController((x) -> ang.in.preAmp(x), 0, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f)
//                ).get(nar);


//        try {
//            new TaskRule("(%1 &&+0 fz:joy)", "(%1 ==>+0 fz:happy)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return task.isBelief();
//                }
//            };
//            new TaskRule("(%1 &&+5 %2)", "seq(%1,%2)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };
//            new TaskRule("(seq(%1,%2) &&+5 %3)", "seq(%1,%2,%3)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };
//            new TaskRule("((%1 &&+5 %2) &&+5 %3)", "seq(%1,%2,%3)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };
//            new TaskRule("(%1 &&+5 (--,%1))", "neg(%1)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };
//            new TaskRule("(%1 &&-5 (--,%1))", "pos(%1)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };
//            new TaskRule("(%1 &&+0 (--,(fz)))", "--good(%1)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };
//            new TaskRule("(%1 &&+0 (fz))", "good(%1)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };

//            new TaskRule("(%1 ==>+0 (fz))", "good(%1)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };

//            new TaskRule("(%1 &&+0 %2)", "par:{%1,%2}", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }            };
//            new TaskRule("((%1 &| %2) &| %3)", "par:{%1,%2,%3}", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }            };

//            final Term same = $.the("same");
//            new TaskRule("(%1 <-> %2)", "same:{%1,%2}", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task) && task.term().containsTermRecursively(same);
//                }
//            };
//        } catch (Narsese.NarseseException e) {
//            e.printStackTrace();
//        }


//        action( new BeliefActionConcept($.inh($.the("fwd"), $.the("fz")), nar, (b) -> {
//            if (b!=null) {
//                float f = b.freq();
//                if (f > 0.75f) {
//                    fz.thrust = true;
//                    return;
//                }
//            }
//            fz.thrust = false;
//        }));
//        action( new BeliefActionConcept($.inh($.the("rot"), $.the("fz")), nar, (b) -> {
//            if (b!=null) {
//                float f = b.freq();
//                if (f > 0.75f) {
//                    fz.left = false; fz.right = true;
//                    return;
//                } else if (f < 0.25f) {
//                    fz.left = true; fz.right = false;
//                    return;
//                }
//            }
//            fz.left = fz.right = false;
//        }));

//        actionBipolar($.inh($.the("rot"), $.the("fz")), (dh) -> {
//           fz.playerAngle += dh * 2f;
//           return true;
//        });
//        actionToggle($.inh($.the("left"), $.the("fz")), (b)->{ fz.left = b; });
//        actionToggle($.inh($.the("right"), $.the("fz")), (b)->{ fz.right = b; });

    }

    private void initToggle() {

        actionToggle($.inh($.the("left"), id), (b) -> {
            if (b && fz.right) {
                fz.left = fz.right = false;
            } else {
                fz.left = b;
            }
        });
        actionToggle($.inh($.the("right"), id), (b) -> {
            if (b && fz.left) {
                fz.left = fz.right = false;
            } else {
                fz.right = b;
            }
        });
        actionToggle($.inh($.the("fwd"), id), (b) -> {
            fz.thrust = b;
        });
        actionToggle($.inh($.the("brake"), id), () -> {
            //fz.left = fz.right = false;
            fz.vehicleMetrics[0][6] *= 0.9f;
        });

    }

    public void initBipolar() {
        GoalActionAsyncConcept[] f = actionBipolar($.the("fwd"), (a) -> {
            //if (f > 0) {
            //accelerator
            //if (f > 0.5f)
            if (a > 0)
                fz.vehicleMetrics[0][6] = /*+=*/ (a) * (fwdSpeed);
            else
                fz.vehicleMetrics[0][6] *= 1 - (-a);
//            else {
//                float brake = 0.5f - f;
//                fz.vehicleMetrics[0][6] *= (1f - brake);
//            }
            return a;
        });
//        //eternal bias to stop
//        nar.goal(f[0].term, Tense.Eternal, 0f, 0.01f);
//        nar.goal(f[1].term, Tense.Eternal, 0f, 0.01f);

        GoalActionAsyncConcept[] x = actionBipolar($.the("x"), (a) -> {
            fz.playerAngle += (a) * rotSpeed;
            return a;
        });
//        //eternal bias to stop
//        nar.goal(x[0].term, Tense.Eternal, 0f, 0.01f);
//        nar.goal(x[1].term, Tense.Eternal, 0f, 0.01f);
    }

    protected boolean polarized(@NotNull Task task) {
        if (task.isQuestOrQuestion())
            return true;
        float f = task.freq();
        return f <= 0.2f || f >= 0.8f;
    }

    double lastDistance;

    @Override
    protected float act() {

        fz.update();

        double distance = fz.vehicleMetrics[0][1];
        double deltaDistance;
        deltaDistance = (distance - lastDistance) / 35f;
        if (deltaDistance > 1f) deltaDistance = 1f;
        if (deltaDistance < -1f) deltaDistance = -1f;

        lastDistance = distance;

        //lifesupport
        fz.power = Math.max(FZeroGame.FULL_POWER * 0.5f, Math.min(FZeroGame.FULL_POWER, fz.power * 1.15f));

        //System.out.println("head=" + fz.playerAngle%(2*3.14f) + " pow=" + fz.power + " vel=" + fz.vehicleMetrics[0][6] + " deltaDist=" + deltaDistance);


        float ambientSadness = 0f;

        return Util.clamp(
                //-0.5f /* bias */ +
                (float) (-(FZeroGame.FULL_POWER - ((float) fz.power)) / FZeroGame.FULL_POWER +
                        //((float)fz.vehicleMetrics[0][6]/100f)+
                        deltaDistance), -1f, +1f) - ambientSadness;
    }


    static class FZeroGame extends JFrame {

        public static final int FULL_POWER = 80;
        public static final int MAX_VEL = 20;
        private final JPanel panel;
        public boolean thrust, left, right;
        public double playerAngle;
        public BufferedImage image = new BufferedImage(
                320, 240, BufferedImage.TYPE_INT_RGB);

        public final double[][] vehicleMetrics = new double[10][9];

        boolean[] K = new boolean[65535]; // pressed keys
        public double power;
        public int rank;
        double rotVel = 0.03;
        final double VIEWER_X = 159.5;
        final double VIEWER_Y = 32;
        final double VIEWER_Z = -128;
        final double GROUND_Y = 207;

        final int[] screenBuffer = new int[320 * 240];
        final int[][][] projectionMap = new int[192][320][2];
        final int[][][] wiresBitmap = new int[32][256][256];
        final int[][][] bitmaps = new int[6][32][32];
        final byte[][] raceTrack = new byte[512][512];
        // -1 = space
        // 0 = road
        // 1 = barrier
        // 2 = power
        // 3 = white circle
        // 4 = dark road
        // 5 = checkered road
        // 0 = x, 1 = y
        // 2 = stunned velocity x, 3 = stunned velocity y
        // 4 = projected x, 5 = projected z
        // 6 = velocity magnitude
        // 7 = vx, 8 = vy
        final int[] powerOvalY = new int[2];
        boolean onPowerBar = false;
        final boolean playing = true;

        final BufferedImage[] vehicleSprites = new BufferedImage[10];
        final int[] vehicleSpriteData = new int[64 * 32];

        final Color powerColor = new Color(0xFABEF1);
        final Color darkColor = new Color(0xA7000000, true);
        int wiresBitmapIndex = 0;
        double cos = 0;
        double sin = 0;
        int hitWallCount = 0;
        int paused = 1;
        private Graphics imageGraphics;
        private Font largeFont;

        public FZeroGame() {
                        powerOvalY[0] = -96;


            // -- GENERATE WIRES BITMAP BEGIN ----------------------------------------------

            //    for(int i = 0; i < 32; i++) {
            //      for(double t = 0; t < 2.0 * Math.PI; t += 0.001) {
            //        int X = 128 + (int)((256 + 64 * Math.cos(t * 3.0)) * Math.sin(t));
            //        int Y = 128 + (int)((256 + 64 * Math.sin(t * 3.0)) * Math.cos(t));
            //        int color = C(t + i * Math.PI / 16.0, 1, 1);
            //        for(int y = 0; y < 16; y++) {
            //          for(int x = 0; x < 16; x++) {
            //            wiresBitmap[i][0xFF & (Y + y)][0xFF & (X + x)] = color;
            //          }
            //        }
            //      }
            //    }

            // -- GENERATE WIRES BITMAP END ------------------------------------------------

            // -- GENERATE VEHICLE SPRITES BEGIN -------------------------------------------

            for (int spriteIndex = 0; spriteIndex < 10; spriteIndex++) {
                vehicleSprites[spriteIndex] = new BufferedImage(
                        64, 32, BufferedImage.TYPE_INT_ARGB_PRE);
                for (int y = 0, k = 0; y < 32; y++) {
                    for (int x = 0; x < 64; x++, k++) {
                        double dx = (x - 32.0) / 2, dy = y - 26;
                        double dist1 = dx * dx + dy * dy;
                        dx = (x - 31.5) / 2;
                        dy = y - 15.5;
                        double dist2 = dx * dx + dy * dy;
                        dy = y - 17.5;
                        dx = x - 32;
                        double dist3 = dx * dx + dy * dy;
                        if (Math.abs(dist3 - 320) <= 24 || Math.abs(dist3 - 480) <= 24) {
                            vehicleSpriteData[k] = C(
                                    Math.PI * spriteIndex / 1.9,
                                    dist1 / 256,
                                    1) | 0xff000000;
                        } else if (dist2 > 256) {
                            vehicleSpriteData[k] = 0;
                        } else {
                            vehicleSpriteData[k] = C(
                                    Math.PI * spriteIndex / 1.9,
                                    dist1 / 256,
                                    dist1 / 1024 + 1) | 0xff000000;
                        }
                    }
                }
                for (int x = 14; x < 49; x++) {
                    for (int y = 21; y < 27; y++) {
                        vehicleSpriteData[(y << 6) | x] = y == 21 || y == 26 || (x & 1) == 0
                                ? 0xFFCCCCCC : 0xFF000000;
                    }
                }
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        double dx = x - 7.5;
                        double dy = y - 7.5;
                        double dy2 = dy / 1.5;
                        double dist = dx * dx + dy * dy;
                        if (dx * dx + dy2 * dy2 < 64) {
                            dy = y - 4;
                            vehicleSpriteData[(y << 6) | (x + 24)] = C(
                                    3,
                                    dist / 256,
                                    y > 6 && x > 3 && x < 12
                                            || y > 7
                                            || dx * dx + dy * dy < 8 ? 2 : 1) | 0xff000000;
                        }
                        if (dist < 64 || y == 0) {
                            vehicleSpriteData[((16 + y) << 6) | (x + 48)] =
                                    vehicleSpriteData[((16 + y) << 6) | x] = C(
                                            Math.PI * spriteIndex / 1.9,
                                            dist / 64,
                                            1) | 0xff000000;
                        }
                    }
                }
                vehicleSprites[spriteIndex].setRGB(
                        0, 0, 64, 32, vehicleSpriteData, 0, 64);
            }

            // -- GENERATE VEHICLE SPRITES BEGIN -------------------------------------------

            // -- GENERATE RACE TRACK BEGIN ------------------------------------------------

            for (int y = 0; y < 512; y++) {
                for (int x = 0; x < 512; x++) {
                    raceTrack[y][x] = -1;
                }
            }

            for (int y = 0; y < 128; y++) {
                for (int x = 246; x < 261; x++) {
                    raceTrack[y][x] = 0;
                }
            }

            for (int y = 32; y < 96; y++) {
                for (int x = 239; x < 246; x++) {
                    raceTrack[y][x] = (byte) ((x < 244 && y > 33 && y < 94) ? 2 : 0);
                }
            }

            for (int y = 128; y < 512; y++) {
                for (int x = 243; x < 264; x++) {
                    double angle = y * Math.PI / 64;
                    raceTrack[y][x + (int) ((8 * Math.cos(angle) + 24) * Math.sin(angle))]
                            = 0;
                }
            }

            for (int y = 0; y < 512; y++) {
                for (int x = 0; x < 512; x++) {
                    if (raceTrack[y][x] >= 0) {
                        for (int i = -1; i < 2; i++) {
                            for (int j = -1; j < 2; j++) {
                                if (raceTrack[0x1FF & (i + y)][0x1FF & (j + x)] == -1) {
                                    raceTrack[y][x] = 1;
                                }
                            }
                        }
                    }
                }
            }

            // -- GENERATE RACE TRACK END --------------------------------------------------

            // -- GENERATE BITMAPS BEGIN --------------------------------------------

            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 32; x++) {
                    double dx = 15.5 - x;
                    double dy = 15.5 - y;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    bitmaps[0][y][x] = 0xFF98A8A8;
                    bitmaps[4][y][x] = 0xFF90A0A0;
                    bitmaps[5][y][x]
                            = (((x >> 3) + (y >> 3)) & 1) == 0 ? 0xFF000000 : 0xFFFFFFFF;
                    bitmaps[2][y][x] = C(4.5, Math.abs(dy) / 16, 1);
                    if (dist < 16) {
                        bitmaps[3][y][x] = 0xFFFFFFFF;
                        bitmaps[1][y][x] = C(5.3, dist / 16, 1 + dist / 256);
                    } else {
                        bitmaps[3][y][x] = bitmaps[1][y][x] = 0xFF98A8A8;
                    }
                }
            }

            // -- GENERATE BITMAPS END -----------------------------------------------------

            // -- COMPUTE PROJECTION MAP BEGIN ---------------------------------------------

            for (int y = 0; y < 192; y++) {
                for (int x = 0; x < 320; x++) {
                    double k = (GROUND_Y - VIEWER_Y) / (48 + y - VIEWER_Y);
                    projectionMap[y][x][0] = (int) (k * (x - VIEWER_X) + VIEWER_X);
                    projectionMap[y][x][1] = (int) (VIEWER_Z * (1 - k));
                }
            }

            // -- COMPUTE PROJECTION MAP END -----------------------------------------------

            setTitle("F-Zero 4K");
            setIconImage(vehicleSprites[0]);
            panel = (JPanel) getContentPane();
            panel.setPreferredSize(new Dimension(640, 480));
            panel.setIgnoreRepaint(true);

            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            pack();
            setLocationRelativeTo(null);
            show();



            imageGraphics = image.getGraphics();
            largeFont = getFont().deriveFont(100f);

//        long nextFrameStart = System.nanoTime();
//        while(true) {
//
//    // -- RENDER FRAME END ---------------------------------------------------------
//          long remaining = nextFrameStart - System.nanoTime();
//          if (remaining > 0) {
//            try {
//              Thread.sleep(remaining / 1000000);
//            } catch(Throwable t) {
//            }
//          }
//        }


        }

        public int C(double angle, double light, double dark) {
            return (D(angle, light, dark) << 16)
                    | (D(angle + 2 * Math.PI / 3, light, dark) << 8)
                    | (D(angle - 2 * Math.PI / 3, light, dark));
        }

        public void update() {
            // -- UPDATE MODEL BEGIN -------------------------------------------------------

            // rotate the background colors
            wiresBitmapIndex = 0x1F & (wiresBitmapIndex + 1);

            if (paused > 0) {
                paused--;
                if (paused == 0) {
                    for (int i = 0; i < 10; i++) {
                        for (int j = 0; j < 9; j++) {
                            vehicleMetrics[i][j] = 0;
                        }
                    }
                    for (int i = 0; i < 4; i++) {
                        vehicleMetrics[i][0] = 7984 + i * 80;
                    }
                    for (int i = 4; i < 10; i += 3) {
                        vehicleMetrics[i][0] = 7984;
                        vehicleMetrics[i][1] = 16384 * (i - 3);
                        vehicleMetrics[i + 1][0] = 8144;
                        vehicleMetrics[i + 1][1] = vehicleMetrics[i][1] + 2048;
                        vehicleMetrics[i + 2][0] = 8144;
                        vehicleMetrics[i + 2][1] = vehicleMetrics[i][1] + 3840;
                    }
                    power = FULL_POWER;
                    playerAngle = hitWallCount = 0;
                    imageGraphics.setFont(getFont().deriveFont(32f));
                    onPowerBar = false;
                }
            } else if (vehicleMetrics[0][1] < 81984 && power > 0) {

                // compute rank
                rank = 1;
                for (int i = 1; i < 4; i++) {
                    if (vehicleMetrics[0][1] < vehicleMetrics[i][1]) {
                        rank++;
                    }
                }

                // reduce power while hitting a wall
                if (hitWallCount > 0) {
                    hitWallCount--;
                    power -= 1;
                    if (power < 0) {
                        power = 0;
                    }
                }

                // process player input
                if (playing) {
                    if (left || K[KeyEvent.VK_LEFT]) {
                        playerAngle += rotVel;
                    } else if (right || K[KeyEvent.VK_RIGHT]) {
                        playerAngle -= rotVel;
                    }
                }
                cos = Math.cos(playerAngle);
                sin = Math.sin(playerAngle);
                vehicleMetrics[0][4] = 0;
                vehicleMetrics[0][5] = 0;
                if (thrust || K[KeyEvent.VK_D]) {

                    if (vehicleMetrics[0][6] < MAX_VEL) {
                        vehicleMetrics[0][6] += 0.2;
                    }
                } else {
                    vehicleMetrics[0][6] *= 0.99;
                }

                if (playing) {
                    // compute computer-controlled-vehicles velocities
                    for (int i = 1; i < 10; i++) {
                        if ((i < 4 && vehicleMetrics[i][6] < 20.5)
                                || vehicleMetrics[i][6] < 10) {
                            vehicleMetrics[i][6] += 0.2 + i * 0.2;
                        }
                        double targetZ = 11 + vehicleMetrics[i][1];
                        double tz = (targetZ / 32) % 512;
                        double targetX = 7984 + (i & 0x03) * 80;
                        if (i >= 4) {
                            targetX += 32;
                        }

                        if (tz >= 128) {
                            double angle = tz * Math.PI / 64;
                            targetX += ((8 * Math.cos(angle) + 24) * Math.sin(angle)) * 32;
                        }

                        double vx = targetX - vehicleMetrics[i][0];
                        double vz = targetZ - vehicleMetrics[i][1];
                        double mag = Math.sqrt(vx * vx + vz * vz);
                        vehicleMetrics[i][7]
                                = vehicleMetrics[i][2] + vehicleMetrics[i][6] * vx / mag;
                        vehicleMetrics[i][8]
                                = vehicleMetrics[i][2] + vehicleMetrics[i][6] * vz / mag;
                    }

                    // player on power bar?
                    onPowerBar = false;
                    if (raceTrack[0x1FF & (((int) vehicleMetrics[0][1]) >> 5)]
                            [0x1FF & (((int) vehicleMetrics[0][0]) >> 5)] == 2) {
                        onPowerBar = true;
                        for (int i = 0; i < 2; i++) {
                            powerOvalY[i] += 16;
                            if (powerOvalY[i] >= 192) {
                                powerOvalY[i] = -32;
                            }
                        }
                        if (power < 80) {
                            power += 0.2;
                        }
                    }

                    vehicleMetrics[0][7] = vehicleMetrics[0][2]
                            - vehicleMetrics[0][6] * sin;
                    vehicleMetrics[0][8] = vehicleMetrics[0][3]
                            + vehicleMetrics[0][6] * cos;

                    // vehicle hitting something?
                    for (int j = 0; j < 10; j++) {

                        // vehicle hitting another vehicle?
                        for (int i = 0; i < 10; i++) {
                            if (i != j) {
                                double normalX = (vehicleMetrics[j][0]
                                        - vehicleMetrics[i][0]) / 2;
                                double normalZ = vehicleMetrics[j][1]
                                        - vehicleMetrics[i][1];
                                double dist2 = normalX * normalX + normalZ * normalZ;
                                if (dist2 < 1200) {
                                    double dotProduct = normalX * vehicleMetrics[0][7]
                                            + normalZ * vehicleMetrics[0][8];
                                    if (dotProduct < 0) {
                                        double ratio = 2.0 * dotProduct / dist2;
                                        vehicleMetrics[j][7] = vehicleMetrics[j][2]
                                                = vehicleMetrics[0][7] - normalX * ratio;
                                        vehicleMetrics[j][8] = vehicleMetrics[j][3]
                                                = vehicleMetrics[0][8] - normalZ * ratio;

                                        vehicleMetrics[i][2] = -vehicleMetrics[j][2];
                                        vehicleMetrics[i][3] = -vehicleMetrics[j][3];
                                        if (i == 0) {
                                            power -= 10;
                                            if (power < 0) {
                                                power = 0;
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        }

                        // vehicle hitting a wall?
                        int vehicleX = ((int) vehicleMetrics[j][0]) >> 5;
                        int vehicleZ = ((int) vehicleMetrics[j][1]) >> 5;
                        for (int z = -2; z <= 2; z++) {
                            for (int x = -2; x <= 2; x++) {
                                if (Math.abs(raceTrack
                                        [0x1FF & (z + vehicleZ)][0x1FF & (x + vehicleX)]) == 1) {
                                    double normalX = vehicleMetrics[j][0]
                                            - (((x + vehicleX) << 5) + 16);
                                    double normalZ = vehicleMetrics[j][1]
                                            - (((z + vehicleZ) << 5) + 16);
                                    double dist2 = normalX * normalX + normalZ * normalZ;
                                    if (dist2 < 2304) {
                                        double dotProduct = normalX * vehicleMetrics[j][7]
                                                + normalZ * vehicleMetrics[j][8];
                                        if (dotProduct < 0) {
                                            double ratio = 2.0 * dotProduct / dist2;
                                            vehicleMetrics[j][7] = vehicleMetrics[j][2]
                                                    = vehicleMetrics[0][7] - normalX * ratio;
                                            vehicleMetrics[j][8] = vehicleMetrics[j][3]
                                                    = vehicleMetrics[0][8] - normalZ * ratio;
                                            vehicleMetrics[j][6] /= 2;
                                            if (j == 0) {
                                                hitWallCount = 5;
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        double velocityMag = vehicleMetrics[j][7] * vehicleMetrics[j][7]
                                + vehicleMetrics[j][8] * vehicleMetrics[j][8];
                        double velocityMaxMag = j == 0 ? 400 : 420;
                        if (velocityMag > velocityMaxMag) {
                            velocityMaxMag = Math.sqrt(velocityMaxMag);
                            velocityMag = Math.sqrt(velocityMag);
                            vehicleMetrics[j][7]
                                    = velocityMaxMag * vehicleMetrics[j][7] / velocityMag;
                            vehicleMetrics[j][8]
                                    = velocityMaxMag * vehicleMetrics[j][8] / velocityMag;
                        }

                        vehicleMetrics[j][0] += vehicleMetrics[j][7];
                        vehicleMetrics[j][1] += vehicleMetrics[j][8];
                        vehicleMetrics[j][2] *= 0.98;
                        vehicleMetrics[j][3] *= 0.98;
                    }
                }
            } else {
                paused = 175;
            }

//    // -- UPDATE MODEL END ---------------------------------------------------------
//            nextFrameStart += 1000000 * frameDelayMS;
//          } while(nextFrameStart < System.nanoTime());
//    // -- RENDER FRAME BEGIN -------------------------------------------------------

            // Draw sky
            double skyRed = 0x65;
            double skyGreen = 0x91;
            for (int y = 0, k = 0; y < 48; y++) {
                int skyColor = 0xFF000000
                        | (((int) skyRed) << 16) | (((int) skyGreen) << 8) | 0xF2;
                for (int x = 0; x < 320; x++, k++) {
                    screenBuffer[k] = skyColor;
                }
                skyRed += 1.75;
                skyGreen += 1.625;
            }

            // Draw earth
            for (int y = 0, k = 15360; y < 192; y++) {
                for (int x = 0; x < 320; x++, k++) {
                    double X = projectionMap[y][x][0] - VIEWER_X;
                    double Z = projectionMap[y][x][1];
                    int xr = (int) (X * cos - Z * sin + vehicleMetrics[0][0]);
                    int zr = (int) (X * sin + Z * cos + vehicleMetrics[0][1]);

                    int z = 0x1FF & (zr >> 5);
                    int tileIndex = raceTrack[z][0x1FF & (xr >> 5)];
                    if (hitWallCount > 0 && tileIndex == 1) {
                        tileIndex = 3;
                    }
                    if (tileIndex == 0 && z < 128 && (z & 1) == 0) {
                        tileIndex = (z == 2) ? 5 : 4;
                    }
                    if (tileIndex < 0) {
                        //screenBuffer[k] = 0;
                        screenBuffer[k]
                                = wiresBitmap[wiresBitmapIndex][0xFF & zr][0xFF & xr];
                    } else {
                        screenBuffer[k] = bitmaps[tileIndex][0x1F & zr][0x1F & xr];
                    }
                }
            }

            image.setRGB(0, 0, 320, 240, screenBuffer, 0, 320);

            // Draw vehicles
            for (int i = 0; i < 10; i++) {
                double X = vehicleMetrics[i][0] - vehicleMetrics[0][0];
                double Z = vehicleMetrics[i][1] - vehicleMetrics[0][1];
                vehicleMetrics[i][4] = X * cos + Z * sin;
                vehicleMetrics[i][5] = (int) (Z * cos - X * sin);
            }
            for (int z = 1200; z > -127; z--) {
                for (int i = 0; i < 10; i++) {
                    if (z == vehicleMetrics[i][5]) {
                        double k = VIEWER_Z / (VIEWER_Z - z);
                        double upperLeftX
                                = k * (vehicleMetrics[i][4] - 32) + VIEWER_X;
                        double upperLeftY
                                = k * (GROUND_Y - 32 - VIEWER_Y) + VIEWER_Y;
                        double lowerRightX
                                = k * (vehicleMetrics[i][4] + 32) + VIEWER_X;
                        double lowerRightY
                                = k * (GROUND_Y - VIEWER_Y) + VIEWER_Y;
                        imageGraphics.drawImage(vehicleSprites[i],
                                (int) upperLeftX, (int) upperLeftY,
                                (int) (lowerRightX - upperLeftX),
                                (int) (lowerRightY - upperLeftY), null);
                    }
                }
            }

            // Draw power bar
            imageGraphics.setColor(power < 20 && (wiresBitmapIndex & 8) == 0
                    ? Color.WHITE : powerColor);
            imageGraphics.fillRect(224, 20, (int) power, 10);
            imageGraphics.setColor(Color.WHITE);
            imageGraphics.drawRect(224, 20, 80, 10);

            // Draw recharge ovals
            if (onPowerBar) {
                imageGraphics.setColor(Color.GREEN);
                for (int i = 0; i < 2; i++) {
                    imageGraphics.fillOval(96, powerOvalY[i], 128, 32);
                }
            }


            if (power <= 0 || (vehicleMetrics[0][1] >= 81984 && rank > 3)) {
                // Draw fail message
                String failString = "FAIL";
                imageGraphics.setFont(largeFont);
                int width = imageGraphics.getFontMetrics().stringWidth(failString);
                int x = (320 - width) / 2;
                imageGraphics.setColor(darkColor);
                imageGraphics.fillRect(x, 65, width + 5, 90);
                imageGraphics.setColor(Color.RED);
                imageGraphics.drawString(failString, x, 145);
            } else if (vehicleMetrics[0][1] >= 81984) {
                // Display winning rank
                String rankString = Integer.toString(rank);
                imageGraphics.setFont(largeFont);
                int width = imageGraphics.getFontMetrics().stringWidth(rankString);
                int x = (320 - width) / 2;
                imageGraphics.setColor(darkColor);
                imageGraphics.fillRect(x - 5, 65, width + 15, 90);
                imageGraphics.setColor((wiresBitmapIndex & 4) == 0
                        ? Color.WHITE : Color.GREEN);
                imageGraphics.drawString(rankString, x, 145);
            } else {
                // Display racing rank
                imageGraphics.setColor((rank == 4) ? (wiresBitmapIndex & 8) == 0
                        ? Color.WHITE : Color.RED : Color.GREEN);
                imageGraphics.drawString(Integer.toString(rank), 16, 32);
            }

            Graphics panelGraphics = panel.getGraphics();
            if (panelGraphics != null) {
                panelGraphics.drawImage(image, 0, 0, 640, 480, null);
                panelGraphics.dispose();
            }

        }

        public int D(double angle, double light, double dark) {
            return (int) (255 * Math.pow((Math.cos(angle) + 1) / 2, light) / dark);
        }

        @Override
        protected void processKeyEvent(KeyEvent e) {
            K[e.getKeyCode()] = e.getID() == 401;
        }

//        public static void main(String[] args) {
//            new FZeroGame();
//        }
    }
}
