package nars.rover.physics.gl;


import com.jogamp.opengl.GL2;
import nars.util.JoglSpace;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Vec2;
import org.jbox2d.common.Vec3;
import org.jbox2d.dynamics.World2D;
import org.jbox2d.particle.ParticleColor;

/**
 *
 */
public class JoglDraw extends JoglAbstractDraw {
    private final JoglSpace panel;

    public JoglDraw(JoglSpace panel) {

        this.panel = panel;

    }

    @Override
    public GL2 gl() {
        return panel.gl();
    }

    @Override
    public void drawParticles(Vec2[] centers, float radius, ParticleColor[] colors, int count) {

    }

    @Override
    public void drawParticlesWireframe(Vec2[] centers, float radius, ParticleColor[] colors, int count) {

    }

    @Override
    public void drawSolidPolygon(Vec3[] vertices, int vertexCount, Color3f color) {
        GL2 gl = this.gl();
        //gl.glPushMatrix();
        //transformViewport(gl, zero);
        gl.glBegin(GL2.GL_TRIANGLE_FAN);
        gl.glColor3f(color.x, color.y, color.z);
        for (int i = 0; i < vertexCount; i++) {
            Vec3 v = vertices[i];
            gl.glVertex3f(v.x, v.y, v.z);
        }
        gl.glEnd();
    }

    @Override
    public void drawSolidPolygon(Vec2[] vertices, int vertexCount, Color3f color) {
        GL2 gl = this.gl();
        //gl.glPushMatrix();
        //transformViewport(gl, zero);
        gl.glBegin(GL2.GL_TRIANGLE_FAN);
        gl.glColor3f(color.x, color.y, color.z);
        for (int i = 0; i < vertexCount; i++) {
            Vec2 v = vertices[i];
            gl.glVertex3f(v.x, v.y, 0f);
        }
        gl.glEnd();

        //OUTLINE
        /*
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glColor4f(color.x, color.y, color.z, 1f);
        for (int i = 0; i < vertexCount; i++) {
            Vec2 v = vertices[i];
            gl.glVertex2f(v.x, v.y);
        }
        gl.glEnd();
        gl.glPopMatrix();
        */
    }

    @Override
    public void drawCircle(Vec2 center, float radius, Color3f color) {
        GL2 gl = this.gl();
        //gl.glPushMatrix();
        //transformViewport(gl, zero);
        float theta = 2 * MathUtils.PI / NUM_CIRCLE_POINTS;
        float c = MathUtils.cos(theta);
        float s = MathUtils.sin(theta);
        float x = radius;
        float cx = center.x;
        float cy = center.y;
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glColor3f(color.x, color.y, color.z);
        float y = 0;
        for (int i = 0; i < NUM_CIRCLE_POINTS; i++) {
            gl.glVertex3f(x + cx, y + cy, 0);
            // apply the rotation matrix
            float temp = x;
            x = c * x - s * y;
            y = s * temp + c * y;
        }
        gl.glEnd();
        //gl.glPopMatrix();
    }


    @Override
    public void drawCircle(Vec2 center, float radius, Vec2 axis, Color3f color) {
        GL2 gl = this.gl();
        //gl.glPushMatrix();
        //transformViewport(gl, zero);
        float theta = 2 * MathUtils.PI / NUM_CIRCLE_POINTS;
        float c = MathUtils.cos(theta);
        float s = MathUtils.sin(theta);
        float x = radius;
        float cx = center.x;
        float cy = center.y;
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glColor3f(color.x, color.y, color.z);
        float y = 0;
        for (int i = 0; i < NUM_CIRCLE_POINTS; i++) {
            gl.glVertex3f(x + cx, y + cy, 0);
            // apply the rotation matrix
            float temp = x;
            x = c * x - s * y;
            y = s * temp + c * y;
        }
        gl.glEnd();
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3f(cx, cy, 0);
        gl.glVertex3f(cx + axis.x * radius, cy + axis.y * radius, 0);
        gl.glEnd();
        //gl.glPopMatrix();
    }

    @Override
    public void drawSolidCircle(Vec2 center, float radius, Vec2 axis, Color3f color) {
        GL2 gl = this.gl();
        //gl.glPushMatrix();
        //transformViewport(gl, zero);
        float theta = 2 * MathUtils.PI / NUM_CIRCLE_POINTS;
        float c = MathUtils.cos(theta);
        float s = MathUtils.sin(theta);
        float x = radius;
        float cx = center.x;
        float cy = center.y;
        gl.glBegin(GL2.GL_TRIANGLE_FAN);
        gl.glColor3f(color.x, color.y, color.z);
        float y = 0;
        for (int i = 0; i < NUM_CIRCLE_POINTS; i++) {
            gl.glVertex3f(x + cx, y + cy, 0);
            // apply the rotation matrix
            float temp = x;
            x = c * x - s * y;
            y = s * temp + c * y;
        }
        gl.glEnd();
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glColor3f(color.x, color.y, color.z);
        for (int i = 0; i < NUM_CIRCLE_POINTS; i++) {
            gl.glVertex3f(x + cx, y + cy, 0);
            // apply the rotation matrix
            float temp = x;
            x = c * x - s * y;
            y = s * temp + c * y;
        }
        gl.glEnd();
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3f(cx, cy, 0);
        gl.glVertex3f(cx + axis.x * radius, cy + axis.y * radius, 0);
        gl.glEnd();
        //gl.glPopMatrix();
    }

    @Override
    public void drawSegment(Vec2 p1, Vec2 p2, Color3f color) {
        GL2 gl = this.gl();
        //gl.glPushMatrix();
        //transformViewport(gl, zero);
        gl.glBegin(GL2.GL_LINES);
        gl.glColor3f(color.x, color.y, color.z);
        gl.glVertex3f(p1.x, p1.y, 0);
        gl.glVertex3f(p2.x, p2.y, 0);
        gl.glEnd();
        //gl.glPopMatrix();
    }
    public void drawSegment(Vec2 p1, Vec2 p2, float r, float g, float b, float a, float width, float z) {
        GL2 gl = this.gl();
        //gl.glPushMatrix();
        //transformViewport(gl, zero);
        gl.glLineWidth(width);
        gl.glBegin(GL2.GL_LINES);
        gl.glColor4f(r, g, b, a);
        gl.glVertex3f(p1.x, p1.y, z);
        gl.glVertex3f(p2.x, p2.y, z);
        gl.glEnd();
        //gl.glPopMatrix();
    }



    @Override
    public void drawPoint(Vec2 argPoint, float argRadiusOnScreen, Color3f argColor) {
        Vec2 vec = getWorldToScreen(argPoint);
        GL2 gl = this.gl();
        gl.glPointSize(argRadiusOnScreen);
        gl.glBegin(GL2.GL_POINTS);
        gl.glVertex3f(vec.x, vec.y, 0);
        gl.glEnd();
    }

    public static final Vec2 zero = new Vec2();

    @Override
    public void drawPolygon(Vec2[] vertices, int vertexCount, Color3f color) {
        GL2 gl = this.gl();
        //gl.glPushMatrix();
        //transformViewport(gl, zero);
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glColor4f(color.x, color.y, color.z, 1f);
        for (int i = 0; i < vertexCount; i++) {
            Vec2 v = vertices[i];
            gl.glVertex3f(v.x, v.y, 0f);
        }
        gl.glEnd();
        //gl.glPopMatrix();
    }


    @Override
    public void drawString(float x, float y, String s, Color3f color) {
        throw new UnsupportedOperationException();
//        text.beginRendering(panel.getWidth(), panel.getHeight());
//        text.setColor(color.x, color.y, color.z, 1);
//        text.draw(s, (int) x, panel.getHeight() - (int) y);
//        text.endRendering();
    }
}


