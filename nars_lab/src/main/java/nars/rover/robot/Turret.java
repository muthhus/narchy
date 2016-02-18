package nars.rover.robot;


import nars.rover.Material;
import nars.rover.Sim;
import nars.rover.physics.gl.JoglAbstractDraw;
import nars.rover.physics.j2d.LayerDraw;
import nars.rover.util.Bodies;
import nars.rover.util.Explosion;
import nars.util.data.random.XorShift128PlusRandom;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Turret implements LayerDraw {

    final static Random rng = new XorShift128PlusRandom(1);

    final float fireProbability = 0.005f;
    private final Sim sim;

    public Turret(Sim sim) {
        this.sim = sim;
        ((JoglAbstractDraw)sim.draw()).addLayer(this);

    }


    public void step(int i) {

        for (Body b : removedBullets) {
            bullets.remove(b);
            sim.remove(b);

            final BulletData bd = (BulletData) b.getUserData();
            bd.explode();
            explosions.add(bd);
        }
        removedBullets.clear();


    }

    final int maxBullets = 32;
    final Deque<Body> bullets = new ArrayDeque(maxBullets);
    final Queue<Body> removedBullets = new ArrayDeque(maxBullets);
    final Collection<BulletData> explosions = new ConcurrentLinkedQueue();

    final static Vec2 zero = new Vec2(0, 0);

    /** returns if fired or not */
    public boolean fire(Body base /*float ttl*/, float power) {

//        final float now = sim.getTime();
//        Iterator<Body> ib = bullets.iterator();
//        while (ib.hasNext()) {
//            Body b = ib.next();
//            ((BulletData)b.getUserData()).diesAt
//
//        }


        if (bullets.size() >= maxBullets) {
            sim.remove( bullets.removeFirst() );
        }


        Vec2 start = base.getWorldPoint(new Vec2(6.5f, 0));
        Body b = sim.create(start,
                Bodies.rectangle(0.5f + (power * 0.3f), 0.2f), BodyType.DYNAMIC);
        b.m_mass= 0.05f + 0.01f * power;

        float angle = base.getAngle();
        Vec2 rayDir = new Vec2( (float)Math.cos(angle), (float)Math.sin(angle) );
        final float speed = 200f;
        rayDir.mulLocal(speed);


        //float diesAt = now + ttl;
        b.setUserData(new BulletData(b, power));
        bullets.add(b);


        b.applyForce(rayDir, zero);

        //recoil:
        base.applyForce(rayDir.mul(-0.5f), start);

//        float angle = (i / (float)numRays) * 360 * DEGTORAD;
//        b2Vec2 rayDir( sinf(angle), cosf(angle) );
//
//        b2BodyDef bd;
//        bd.type = b2_dynamicBody;
//        bd.fixedRotation = true; // rotation not necessary
//        bd.bullet = true; // prevent tunneling at high speed
//        bd.linearDamping = 10; // drag due to moving through air
//        bd.gravityScale = 0; // ignore gravity
//        bd.position = center; // start at blast center
//        bd.linearVelocity = blastPower * rayDir;
//        b2Body* body = m_world->CreateBody( &bd );
//
//        b2CircleShape circleShape;
//        circleShape.m_radius = 0.05; // very small
//
//        b2FixtureDef fd;
//        fd.shape = &circleShape;
//        fd.density = 60 / (float)numRays; // very high - shared across all particles
//        fd.friction = 0; // friction not necessary
//        fd.restitution = 0.99f; // high restitution to reflect off obstacles
//        fd.filter.groupIndex = -1; // particles should not collide with each other
//        body->CreateFixture( &fd );

        return true;
    }

    @Override
    public void drawGround(JoglAbstractDraw draw, World w) {

    }

    @Override
    public void drawSky(JoglAbstractDraw draw, World w) {
        if (!explosions.isEmpty()) {
            Iterator<BulletData> ii = explosions.iterator();
            while (ii.hasNext()) {
                BulletData bd = ii.next();
                if (bd.explosionTTL-- <= 0)
                    ii.remove();


                draw.drawSolidCircle(bd.getCenter(), bd.explosionTTL/8f +  rng.nextFloat() * 4f, new Vec2(),
                        new Color3f(1 - rng.nextFloat()/3f,
                                0.8f - rng.nextFloat()/3f,
                                0f));
            }
        }

    }

    /** depleted uranium */
    public class BulletData extends Material implements Collidable {

        //private final float diesAt;
        private final Body bullet;
        private final float power;
        public int explosionTTL;
        Color3f color = new Color3f();


        public BulletData(Body b, float power) {
            this.bullet = b;
            this.power = power;
            //this.diesAt = diesAt;
        }

        public void explode() {
            //System.out.println("expldoe " + bullet.getWorldCenter());
            float force = 5f + 50f * power;
            Explosion.explodeBlastRadius(bullet.getWorld(), bullet.getWorldCenter(), 50f,force);
            explosionTTL = (int)force/2;
        }

        public Vec2 getCenter() { return bullet.getWorldCenter(); }

        @Override public void onCollision(Contact c) {
            //System.out.println(bullet + " collided");
            removedBullets.add(bullet);
        }

        @Override
        public void before(Body b, JoglAbstractDraw d, float time) {
            color.set(
                    0.5f + (0.5f * ((float)Math.sin(time * (1f + power)))) * 0.4f
                    + 0.5f, 0.25f, 0.25f);
            d.setFillColor(color);
        }
    }

}
