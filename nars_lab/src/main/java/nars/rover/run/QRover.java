package nars.rover.run;

import nars.op.meta.hai;
import nars.rover.Material;
import nars.rover.obj.VisionRay;
import nars.rover.robot.AbstractPolygonBot;
import nars.util.data.Util;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;

import static nars.rover.Material.*;

/**
 * Created by me on 1/31/16.
 */
public class QRover extends AbstractPolygonBot {

    final int retinaPixels = 16;
    final int motionPixels = 2;

    final int inputsPerPixel = 3;
    final int inputs = retinaPixels * inputsPerPixel + motionPixels;

    final int actions = 6;
    private final hai hai;
    private final float[] in;
    float rew = 0;

    public QRover(String id) {
        super(id);
        this.hai = new hai(inputs, actions*2, actions);
        this.hai.setQ(0.13f, 0.7f, 0.9f); //0.1 0.5 0.9

        this.in = new float[inputs];
    }

    @Override
    public RoboticMaterial getMaterial() {
        return new RoboticMaterial(this);
    }

    @Override
    protected Body newTorso() {
        Body b = newTriangleTorso(getWorld(), 1f);



        int retinaRaysPerPixel = 2;
        int L = 25;
        double mouthArc = Math.PI / 6f; //in radians

        for (int i = 0; i < retinaPixels; i++) {
            float aStep = (float) (Math.PI * 2f) / retinaPixels;
            final float angle = aStep * i;
            Vec2 mouthPoint = new Vec2(2.7f, 0); //0.5f);

            final int I = i;
            VisionRay v = new VisionRay(/*eats ?*/ mouthPoint /*: new Vec2(0,0)*/,
                    angle, aStep, this, L, retinaRaysPerPixel) {

                @Override
                protected void perceiveDist(Body hit, float conf, float dist) {

                    super.perceiveDist(hit, conf, dist);

                    int j = I * 2;
                    in[j++] = dist;

                    float v;
                    switch (material(hit)) {
                        case "food": v = 1f; break;
                        case "poison": v = -1f; break;
                        default: v = 0; break;
                    }
                    in[j] = v;

                }

                @Override
                protected void updateColor(Color3f rayColor) {
                    rayColor.x = Util.clamp(in[I*2]);
                    rayColor.y = Util.clamp(in[I*2+1]);
                    rayColor.z = 0.5f;
                }
            };
            v.setEats(((angle < mouthArc / 2f) || (angle > (Math.PI * 2f) - mouthArc / 2f)));


            draw.addLayer(v);
            senses.add(v);
        }
        return b;
    }

    @Override
    protected void onEat(Body eaten, Material m) {
        super.onEat(eaten, m);
        if (m == food) {
            rew += 0.5f;
        } else if (m == poison) {
            rew -= 0.5f;
        } else if (m == wall) {
            rew -= 0.05f;
        }

        rew = Util.sigmoid(rew)-0.5f;

    }

    @Override
    public void step(int time) {
        super.step(time);


        rew *= 0.95f; //decay to zero (neutral)

        switch (hai.act(in, rew)) {
            case 0: break; //reserved until actions are balanced
            case 1: thrustRelative(1f); break;
            case 2: thrustRelative(-1f); break;
            case 3: rotateRelative(-1f); break;
            case 4: rotateRelative(1f); break;
            case 5: stop(); break;
            default: throw new RuntimeException("unknown action");
        }
    }

    @Override
    protected void feelMotion() {
        int o = retinaPixels * inputsPerPixel;
        in[o + 0] = (float)Math.cos(torso.getAngle());
        in[o + 1] = (float)Math.sin(torso.getAngle());

        //System.out.println( Arrays.toString(in) + " "  + rew);
    }
}
