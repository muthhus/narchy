package jake2.render.opengl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class DummyGL implements QGL {
    
    private static final QGL self = new DummyGL();
    
    protected DummyGL() {
        // singleton
    }
    
    public static QGL getInstance() {
        return self;
    }
    
    @Override
    public void glAlphaFunc(int func, float ref) {
        // do nothing
    }

    @Override
    public void glBegin(int mode) {
        // do nothing
    }

    @Override
    public void glBindTexture(int target, int texture) {
        // do nothing
    }

    @Override
    public void glBlendFunc(int sfactor, int dfactor) {
        // do nothing
    }

    @Override
    public void glClear(int mask) {
        // do nothing
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        // do nothing
    }

    @Override
    public void glColor3f(float red, float green, float blue) {
        // do nothing
    }

    @Override
    public void glColor3ub(byte red, byte green, byte blue) {
        // do nothing
    }

    @Override
    public void glColor4f(float red, float green, float blue, float alpha) {
        // do nothing
    }

    @Override
    public void glColor4ub(byte red, byte green, byte blue, byte alpha) {
        // do nothing
    }

    @Override
    public void glColorPointer(int size, boolean unsigned, int stride,
                               ByteBuffer pointer) {
        // do nothing
    }
    
    @Override
    public void glColorPointer(int size, int stride, FloatBuffer pointer) {
        // do nothing
    }

    @Override
    public void glCullFace(int mode) {
        // do nothing
    }

    @Override
    public void glDeleteTextures(IntBuffer textures) {
        // do nothing
    }

    @Override
    public void glDepthFunc(int func) {
        // do nothing
    }

    @Override
    public void glDepthMask(boolean flag) {
        // do nothing
    }

    @Override
    public void glDepthRange(double zNear, double zFar) {
        // do nothing
    }

    @Override
    public void glDisable(int cap) {
        // do nothing
    }

    @Override
    public void glDisableClientState(int cap) {
        // do nothing
    }

    @Override
    public void glDrawArrays(int mode, int first, int count) {
        // do nothing
    }

    @Override
    public void glDrawBuffer(int mode) {
        // do nothing
    }

    @Override
    public void glDrawElements(int mode, ShortBuffer indices) {
        // do nothing
    }

    @Override
    public void glEnable(int cap) {
        // do nothing
    }

    @Override
    public void glEnableClientState(int cap) {
        // do nothing
    }

    @Override
    public void glEnd() {
        // do nothing
    }

    @Override
    public void glFinish() {
        // do nothing
    }

    @Override
    public void glFlush() {
        // do nothing
    }

    @Override
    public void glFrustum(double left, double right, double bottom,
                          double top, double zNear, double zFar) {
        // do nothing
    }

    @Override
    public int glGetError() {
        return GL_NO_ERROR;
    }

    @Override
    public void glGetFloat(int pname, FloatBuffer params) {
        // do nothing
    }

    @Override
    public String glGetString(int name) {
        switch (name) {
        case GL_EXTENSIONS:
            return "GL_ARB_multitexture GL_EXT_point_parameters";
        case GL_VERSION:
            return "2.0.0 Dummy";
        case GL_VENDOR:
            return "Dummy Cooperation";
        case GL_RENDERER:
            return "Dummy Renderer";
        default:
            return "";
        }
    }
    
    @Override
    public void glHint(int target, int mode) {
        // do nothing
    }

    @Override
    public void glInterleavedArrays(int format, int stride,
                                    FloatBuffer pointer) {
        // do nothing
    }

    @Override
    public void glLoadIdentity() {
        // do nothing
    }

    @Override
    public void glLoadMatrix(FloatBuffer m) {
        // do nothing
    }

    @Override
    public void glMatrixMode(int mode) {
        // do nothing
    }

    @Override
    public void glOrtho(double left, double right, double bottom,
                        double top, double zNear, double zFar) {
        // do nothing
    }

    @Override
    public void glPixelStorei(int pname, int param) {
        // do nothing
    }

    @Override
    public void glPointSize(float size) {
        // do nothing
    }

    @Override
    public void glPolygonMode(int face, int mode) {
        // do nothing
    }

    @Override
    public void glPopMatrix() {
        // do nothing
    }

    @Override
    public void glPushMatrix() {
        // do nothing
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height,
                             int format, int type, ByteBuffer pixels) {
        // do nothing
    }

    @Override
    public void glRotatef(float angle, float x, float y, float z) {
        // do nothing
    }

    @Override
    public void glScalef(float x, float y, float z) {
        // do nothing
    }

    @Override
    public void glScissor(int x, int y, int width, int height) {
        // do nothing
    }

    @Override
    public void glShadeModel(int mode) {
        // do nothing
    }

    @Override
    public void glTexCoord2f(float s, float t) {
        // do nothing
    }

    @Override
    public void glTexCoordPointer(int size, int stride, FloatBuffer pointer) {
        // do nothing
    }

    @Override
    public void glTexEnvi(int target, int pname, int param) {
        // do nothing
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat,
                             int width, int height, int border, int format, int type,
                             ByteBuffer pixels) {
        // do nothing
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat,
                             int width, int height, int border, int format, int type,
                             IntBuffer pixels) {
        // do nothing
    }

    @Override
    public void glTexParameterf(int target, int pname, float param) {
        // do nothing
    }

    @Override
    public void glTexParameteri(int target, int pname, int param) {
        // do nothing
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset,
                                int yoffset, int width, int height, int format, int type,
                                IntBuffer pixels) {
        // do nothing
    }

    @Override
    public void glTranslatef(float x, float y, float z) {
        // do nothing
    }

    @Override
    public void glVertex2f(float x, float y) {
        // do nothing
    }

    @Override
    public void glVertex3f(float x, float y, float z) {
        // do nothing
    }

    @Override
    public void glVertexPointer(int size, int stride, FloatBuffer pointer) {
        // do nothing
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        // do nothing
    }

    @Override
    public void glColorTable(int target, int internalFormat, int width,
                             int format, int type, ByteBuffer data) {
        // do nothing
    }

    @Override
    public void glActiveTextureARB(int texture) {
        // do nothing
    }

    @Override
    public void glClientActiveTextureARB(int texture) {
        // do nothing
    }

    @Override
    public void glPointParameterEXT(int pname, FloatBuffer pfParams) {
        // do nothing
    }

    @Override
    public void glPointParameterfEXT(int pname, float param) {
        // do nothing
    }

    @Override
    public void glLockArraysEXT(int first, int count) {
        // do nothing
    }

    @Override
    public void glArrayElement(int index) {
        // do nothing
    }

    @Override
    public void glUnlockArraysEXT() {
        // do nothing
    }

    @Override
    public void glMultiTexCoord2f(int target, float s, float t) {
        // do nothing
    }

    /*
     * util extensions
     */
    @Override
    public void setSwapInterval(int interval) {
        // do nothing
    }

}
