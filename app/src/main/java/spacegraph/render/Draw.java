/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2007 Erwin Coumans  http://continuousphysics.com/Bullet/
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from
 * the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package spacegraph.render;


import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.util.ImmModeSink;
import javafx.scene.paint.Color;
import spacegraph.EDraw;
import spacegraph.SimpleSpatial;
import spacegraph.math.AxisAngle4f;
import spacegraph.math.Matrix3f;
import spacegraph.math.Matrix4f;
import spacegraph.math.v3;
import spacegraph.phys.collision.broad.BroadphaseNativeType;
import spacegraph.phys.math.Transform;
import spacegraph.phys.math.VectorUtil;
import spacegraph.phys.shape.*;
import spacegraph.phys.util.BulletStack;

import static com.jogamp.opengl.util.gl2.GLUT.STROKE_MONO_ROMAN;
import static spacegraph.render.JoglSpace.glut;
import static spacegraph.test.Lesson14.renderString;

/**
 * @author jezek2
 */
public enum Draw {
    ;

    public final static GLSRT glsrt = new GLSRT(JoglSpace.glu);


	/*
    private static Map<CollisionShape,TriMeshKey> g_display_lists = new HashMap<CollisionShape,TriMeshKey>();
	
	private static int OGL_get_displaylist_for_shape(CollisionShape shape) {
		// JAVA NOTE: rewritten
		TriMeshKey trimesh = g_display_lists.get(shape);
		if (trimesh != null) {
			return trimesh.dlist;
		}

		return 0;
	}

	private static void OGL_displaylist_clean() {
		// JAVA NOTE: rewritten
		for (TriMeshKey trimesh : g_display_lists.values()) {
			glDeleteLists(trimesh.dlist, 1);
		}

		g_display_lists.clear();
	}
	*/

    public static void drawCoordSystem(GL gl) {
        ImmModeSink vbo = ImmModeSink.createFixed(3 * 4,
                3, GL.GL_FLOAT, // vertex
                4, GL.GL_FLOAT, // color
                0, GL.GL_FLOAT, // normal
                0, GL.GL_FLOAT, // texCoords
                GL.GL_STATIC_DRAW);
        vbo.glBegin(gl.GL_LINES);
        vbo.glColor4f(1f, 1f, 1f, 1f);
        vbo.glVertex3f(0f, 0f, 0f);
        vbo.glColor4f(1f, 1f, 1f, 1f);
        vbo.glVertex3f(1f, 0f, 0f);
        vbo.glColor4f(1f, 1f, 1f, 1f);
        vbo.glVertex3f(0f, 0f, 0f);
        vbo.glColor4f(1f, 1f, 1f, 1f);
        vbo.glVertex3f(0f, 1f, 0f);
        vbo.glColor4f(1f, 1f, 1f, 1f);
        vbo.glVertex3f(0f, 0f, 0f);
        vbo.glColor4f(1f, 1f, 1f, 1f);
        vbo.glVertex3f(0f, 0f, 1f);
        vbo.glEnd(gl);
    }

    private static final float[] glMat = new float[16];

    @Deprecated
    final static BulletStack stack = new BulletStack();
    private static final v3 a = new v3(), b = new v3();

    public static void translate(GL2 gl, Transform trans) {
        v3 o = trans;
        gl.glTranslatef(o.x, o.y, o.z);
    }

    public static void transform(GL2 gl, Transform trans) {
        gl.glMultMatrixf(trans.getOpenGLMatrix(glMat), 0);
    }


    public static void draw(GL2 gl, CollisionShape shape) {


        //System.out.println("shape="+shape+" type="+BroadphaseNativeTypes.forValue(shape.getShapeType()));


        //		if (shape.getShapeType() == BroadphaseNativeTypes.UNIFORM_SCALING_SHAPE_PROXYTYPE.getValue())
        //		{
        //			const btUniformScalingShape* scalingShape = static_cast<const btUniformScalingShape*>(shape);
        //			const btConvexShape* convexShape = scalingShape->getChildShape();
        //			float	scalingFactor = (float)scalingShape->getUniformScalingFactor();
        //			{
        //				btScalar tmpScaling[4][4]={{scalingFactor,0,0,0},
        //					{0,scalingFactor,0,0},
        //					{0,0,scalingFactor,0},
        //					{0,0,0,1}};
        //
        //				drawOpenGL( (btScalar*)tmpScaling,convexShape,color,debugMode);
        //			}
        //			return;
        //		}


        if (shape.getShapeType() == BroadphaseNativeType.COMPOUND_SHAPE_PROXYTYPE) {
            CompoundShape compoundShape = (CompoundShape) shape;
            Transform childTrans = new Transform();
            for (int i = compoundShape.getNumChildShapes() - 1; i >= 0; i--) {
                stack.transforms.get(
                        compoundShape.getChildTransform(i, childTrans)
                );
                CollisionShape colShape = compoundShape.getChildShape(i);


                gl.glPushMatrix();
                stack.pushCommonMath();
                draw(gl, colShape);
                stack.popCommonMath();
                gl.glPopMatrix();
            }
        } else {
            boolean useWireframeFallback = true;
            switch (shape.getShapeType()) {
                case BOX_SHAPE_PROXYTYPE: {
                    BoxShape boxShape = (BoxShape) shape;
                    boxShape.getHalfExtentsWithoutMargin(a);
                    //Vector3f halfExtent = stack.vectors.get();
                    gl.glScalef(2f * a.x, 2f * a.y, 2f * a.z);
                    //glsrt.drawCube(gl, 1f);
                    glut.glutSolidCube(1f);

                    useWireframeFallback = false;
                    break;
                }
                case TRIANGLE_SHAPE_PROXYTYPE:
                case TETRAHEDRAL_SHAPE_PROXYTYPE: {
                    //todo:
                    //					useWireframeFallback = false;
                    break;
                }
                case CONVEX_HULL_SHAPE_PROXYTYPE:
                    break;
                case SPHERE_SHAPE_PROXYTYPE: {
                    SphereShape sphereShape = (SphereShape) shape;
                    float radius = sphereShape.getMargin(); // radius doesn't include the margin, so draw with margin
                    // TODO: glutSolidSphere(radius,10,10);
                    //sphere.draw(radius, 8, 8);
                    glsrt.drawSphere(gl, radius);
                            /*
							glPointSize(10f);
							glBegin(gl.GL_POINTS);
							glVertex3f(0f, 0f, 0f);
							glEnd();
							glPointSize(1f);
							*/
                    useWireframeFallback = false;
                    break;
                }
                case CAPSULE_SHAPE_PROXYTYPE: {
                    CapsuleShape capsuleShape = (CapsuleShape) shape;
                    float radius = capsuleShape.getRadius();
                    float halfHeight = capsuleShape.getHalfHeight();
                    int upAxis = 1;

                    glsrt.drawCylinder(gl, radius, halfHeight, upAxis);

                    gl.glTranslatef(0f, -halfHeight, 0f);
                    //glutSolidSphere(radius,10,10);
                    //sphere.draw(radius, 10, 10);
                    glsrt.drawSphere(gl, radius);
                    gl.glTranslatef(0f, 2f * halfHeight, 0f);
                    //glutSolidSphere(radius,10,10);
                    //sphere.draw(radius, 10, 10);
                    glsrt.drawSphere(gl, radius);
                    useWireframeFallback = false;
                    break;
                }
                case MULTI_SPHERE_SHAPE_PROXYTYPE: {
                    break;
                }
                //				case CONE_SHAPE_PROXYTYPE:
                //					{
                //						const btConeShape* coneShape = static_cast<const btConeShape*>(shape);
                //						int upIndex = coneShape->getConeUpIndex();
                //						float radius = coneShape->getRadius();//+coneShape->getMargin();
                //						float height = coneShape->getHeight();//+coneShape->getMargin();
                //						switch (upIndex)
                //						{
                //						case 0:
                //							glRotatef(90.0, 0.0, 1.0, 0.0);
                //							break;
                //						case 1:
                //							glRotatef(-90.0, 1.0, 0.0, 0.0);
                //							break;
                //						case 2:
                //							break;
                //						default:
                //							{
                //							}
                //						};
                //
                //						glTranslatef(0.0, 0.0, -0.5*height);
                //						glutSolidCone(radius,height,10,10);
                //						useWireframeFallback = false;
                //						break;
                //
                //					}
                case CONVEX_TRIANGLEMESH_SHAPE_PROXYTYPE: {
                    useWireframeFallback = false;
                    break;
                }

                case CONVEX_SHAPE_PROXYTYPE:
                case CYLINDER_SHAPE_PROXYTYPE: {
                    CylinderShape cylinder = (CylinderShape) shape;
                    int upAxis = cylinder.getUpAxis();

                    float radius = cylinder.getRadius();
                    float halfHeight = VectorUtil.coord(cylinder.getHalfExtentsWithMargin(new v3()), upAxis);

                    glsrt.drawCylinder(gl, radius, halfHeight, upAxis);

                    break;
                }
                default: {
                }

            }


            if (useWireframeFallback) {
                // for polyhedral shapes
                if (shape.isPolyhedral()) {
                    PolyhedralConvexShape polyshape = (PolyhedralConvexShape) shape;

                    ImmModeSink vbo = ImmModeSink.createFixed(polyshape.getNumEdges() + 3,
                            3, GL.GL_FLOAT,  // vertex
                            0, GL.GL_FLOAT,  // color
                            0, GL.GL_FLOAT,  // normal
                            0, GL.GL_FLOAT, GL.GL_STATIC_DRAW); // texture

                    vbo.glBegin(gl.GL_LINES);

                    //Vector3f a = stack.vectors.get(), b = stack.vectors.get();
                    int i;
                    for (i = 0; i < polyshape.getNumEdges(); i++) {
                        polyshape.getEdge(i, a, b);

                        vbo.glVertex3f(a.x, a.y, a.z);
                        vbo.glVertex3f(b.x, b.y, b.z);
                    }
                    vbo.glEnd(gl);

                    //					if (debugMode==btIDebugDraw::DBG_DrawFeaturesText)
                    //					{
                    //						glRasterPos3f(0.0,  0.0,  0.0);
                    //						//BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),polyshape->getExtraDebugInfo());
                    //
                    //						glColor3f(1.f, 1.f, 1.f);
                    //						for (i=0;i<polyshape->getNumVertices();i++)
                    //						{
                    //							btPoint3 vtx;
                    //							polyshape->getVertex(i,vtx);
                    //							glRasterPos3f(vtx.x(),  vtx.y(),  vtx.z());
                    //							char buf[12];
                    //							sprintf(buf," %d",i);
                    //							BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),buf);
                    //						}
                    //
                    //						for (i=0;i<polyshape->getNumPlanes();i++)
                    //						{
                    //							btVector3 normal;
                    //							btPoint3 vtx;
                    //							polyshape->getPlane(normal,vtx,i);
                    //							btScalar d = vtx.dot(normal);
                    //
                    //							glRasterPos3f(normal.x()*d,  normal.y()*d, normal.z()*d);
                    //							char buf[12];
                    //							sprintf(buf," plane %d",i);
                    //							BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),buf);
                    //
                    //						}
                    //					}


                }
            }

            //		#ifdef USE_DISPLAY_LISTS
            //
            //		if (shape->getShapeType() == TRIANGLE_MESH_SHAPE_PROXYTYPE||shape->getShapeType() == GIMPACT_SHAPE_PROXYTYPE)
            //			{
            //				GLuint dlist =   OGL_get_displaylist_for_shape((btCollisionShape * )shape);
            //				if (dlist)
            //				{
            //					glCallList(dlist);
            //				}
            //				else
            //				{
            //		#else
            if (shape.isConcave())//>getShapeType() == TRIANGLE_MESH_SHAPE_PROXYTYPE||shape->getShapeType() == GIMPACT_SHAPE_PROXYTYPE)
            //		if (shape->getShapeType() == TRIANGLE_MESH_SHAPE_PROXYTYPE)
            {
                ConcaveShape concaveMesh = (ConcaveShape) shape;
                //btVector3 aabbMax(btScalar(1e30),btScalar(1e30),btScalar(1e30));
                //btVector3 aabbMax(100,100,100);//btScalar(1e30),btScalar(1e30),btScalar(1e30));

                //todo pass camera, for some culling
//                    Vector3f aabbMax = stack.vectors.get(1e30f, 1e30f, 1e30f);
//                    Vector3f aabbMin = stack.vectors.get(-1e30f, -1e30f, -1e30f);
                a.set(1e30f, 1e30f, 1e30f);
                b.set(-1e30f, -1e30f, -1e30f);

                GlDrawcallback drawCallback = new GlDrawcallback(gl);
                drawCallback.wireframe = false; //(debugMode & DebugDrawModes.DRAW_WIREFRAME) != 0;

                concaveMesh.processAllTriangles(drawCallback, b, a);
            }
        }
        //#endif

        //#ifdef USE_DISPLAY_LISTS
        //		}
        //	}
        //#endif

        //			if (shape->getShapeType() == CONVEX_TRIANGLEMESH_SHAPE_PROXYTYPE)
        //			{
        //				btConvexTriangleMeshShape* convexMesh = (btConvexTriangleMeshShape*) shape;
        //
        //				//todo: pass camera for some culling
        //				btVector3 aabbMax(btScalar(1e30),btScalar(1e30),btScalar(1e30));
        //				btVector3 aabbMin(-btScalar(1e30),-btScalar(1e30),-btScalar(1e30));
        //				TriangleGlDrawcallback drawCallback;
        //				convexMesh->getMeshInterface()->InternalProcessAllTriangles(&drawCallback,aabbMin,aabbMax);
        //
        //			}

        // TODO: error in original sources GL_DEPTH_BUFFER_BIT instead of GL_DEPTH_TEST
        //gl.glDisable(GL_DEPTH_TEST);
        //glRasterPos3f(0, 0, 0);//mvtx.x(),  vtx.y(),  vtx.z());
//				if ((debugMode & DebugDrawModes.DRAW_TEXT) != 0) {
//					// TODO: BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),shape->getName());
//				}
//
//				if ((debugMode & DebugDrawModes.DRAW_FEATURES_TEXT) != 0) {
//					//BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),shape->getExtraDebugInfo());
//				}
        //gl.glEnable(GL_DEPTH_TEST);


    }

    @Deprecated
    public static void line(GL2 gl, double x1, double y1, double x2, double y2) {
        line(gl, (float) x1, (float) y1, (float) x2, (float) y2);
    }

    public static void line(GL2 gl, float x1, float y1, float x2, float y2) {
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3f(x1, y1, 0);
        gl.glVertex3f(x2, y2, 0);
        gl.glEnd();
    }

    public static void rectStroke(GL2 gl, float x1, float y1, float w, float h) {
        line(gl, x1, y1, x1 + w, y1);
        line(gl, x1, y1, x1, y1 + h);
        line(gl, x1, y1 + h, x1 + w, y1 + h);
        line(gl, x1 + w, y1, x1 + w, y1 + h);
    }

    public static void rect(GL2 gl, float x1, float y1, float w, float h) {

        gl.glBegin(GL2.GL_QUADS);
        gl.glVertex3f(x1, y1, 0);
        gl.glVertex3f(x1 + w, y1, 0);
        gl.glVertex3f(x1 + w, y1 + h, 0);
        gl.glVertex3f(x1, y1 + h, 0);
        gl.glEnd();
    }

    public static void rect(GL2 gl, float x1, float y1, float w, float h, float z) {

        gl.glBegin(GL2.GL_QUADS);
        gl.glVertex3f(x1, y1, z);
        gl.glVertex3f(x1 + w, y1, z);
        gl.glVertex3f(x1 + w, y1 + h, z);
        gl.glVertex3f(x1, y1 + h, z);
        gl.glEnd();
    }

    /** thickness of font to avoid z-fighting */
    final static float zStep = 0.05f;

    static public void text(GL2 gl, float scaleX, float scaleY, String label, float dx, float dy, float dz) {
        text(gl, scaleX, scaleY, label, dx, dy, dz, null);
    }
    static public void text(GL2 gl, float scaleX, float scaleY, String label, float dx, float dy, float dz, float[] color) {
        gl.glPushMatrix();
        //gl.glNormal3f(0, 0, 1f);
        gl.glTranslatef(dx, dy, dz + zStep);

        float fontThick = 1f;
        gl.glLineWidth(fontThick);


        if (color!=null)
            gl.glColor3fv(color, 0);

        //float r = v.radius;
        renderString(gl, /*GLUT.STROKE_ROMAN*/ STROKE_MONO_ROMAN, label,
                scaleX, scaleY,
                0, 0, dz); // Print GL Text To The Screen
        gl.glPopMatrix();
    }

    static final float[] tmpV = new float[3];
    static final v3 ww = new v3();
    static final v3 vv = new v3();
    static final Quaternion tmpQ = new Quaternion();
    static final Matrix4f tmpM4 = new Matrix4f();
    static final Matrix3f tmpM3 = new Matrix3f();
    static final AxisAngle4f tmpA = new AxisAngle4f();

    static public void renderHalfTriEdge(GL2 gl, SimpleSpatial src, EDraw e, float width) {
        SimpleSpatial tgt = e.target;

        gl.glPushMatrix();

        src.transform().toAngleAxis(tmpQ, tmpA, ww);
        ww.normalize(); //used for the normal3f below
        float sx = src.x();
        float tx = tgt.x();
        float dx = tx - sx;
        float sy = src.y();
        float ty = tgt.y();
        float dy = ty - sy;
        float sz = src.z();
        float tz = tgt.z();
        float dz = tz - sz;

        vv.set(dx,dy,dz);
        vv.cross(ww, vv);
        vv.normalize();
        vv.scale(width);


        gl.glBegin(GL2.GL_TRIANGLES);


        gl.glVertex3f(sx+ vv.x, sy+ vv.y, sz+ vv.z); //right base
        gl.glVertex3f( //right base
                //sx+-vv.x, sy+-vv.y, sz+-vv.z
                sx, sy, sz
        );
        gl.glVertex3f(tx, ty, tz); //right base
        gl.glEnd();

        gl.glPopMatrix();

    }

    public static void renderLineEdge(GL2 gl, SimpleSpatial src, EDraw e, float width) {
        gl.glLineWidth(width);
        gl.glBegin(GL.GL_LINES);
        v3 s = src.transform();
        gl.glVertex3f(s.x, s.y, s.z);
        v3 t = e.target.transform();
        gl.glVertex3f(t.x, t.y, t.z);
        gl.glEnd();
    }


    public static void hsb(float hue, float saturation, float brightness, float a, float[] target) {
        float r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float)Math.floor(hue)) * 6.0f;
            float f = h - (float)Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0:
                    r =(brightness );
                    g =(t );
                    b =(p );
                    break;
                case 1:
                    r =(q );
                    g =(brightness );
                    b =(p );
                    break;
                case 2:
                    r =(p );
                    g =(brightness );
                    b =(t );
                    break;
                case 3:
                    r =(p );
                    g =(q );
                    b =(brightness );
                    break;
                case 4:
                    r =(t );
                    g =(p );
                    b =(brightness );
                    break;
                case 5:
                    r =(brightness );
                    g =(p );
                    b =(q );
                    break;
            }
        }
        target[0] = r;
        target[1] = g;
        target[2] = b;
        target[3] = a;
    }

//
//    public static void hsb(float h, float s, float b, float a, float[] target) {
//        //TODO use a LUT matrix instaed of this shitty Color function
//        Color c = Color.hsb(360*h, s, b);
//        target[0] = (float)c.getRed();
//        target[1] = (float)c.getGreen();
//        target[2] = (float)c.getBlue();
//        target[3] = a;
//    }

    ////////////////////////////////////////////////////////////////////////////

    private static class TriMeshKey {
        public CollisionShape shape;
        public int dlist; // OpenGL display list
    }

    private static class GlDisplaylistDrawcallback extends TriangleCallback {
        private final GL gl;

        private final v3 diff1 = new v3();
        private final v3 diff2 = new v3();
        private final v3 normal = new v3();

        public GlDisplaylistDrawcallback(GL gl) {
            this.gl = gl;
        }

        public void processTriangle(v3[] triangle, int partId, int triangleIndex) {
            diff1.sub(triangle[1], triangle[0]);
            diff2.sub(triangle[2], triangle[0]);
            normal.cross(diff1, diff2);

            normal.normalize();

            ImmModeSink vbo = ImmModeSink.createFixed(GL.GL_STATIC_DRAW, 3,
                    3, GL.GL_FLOAT,  // vertex
                    4, GL.GL_FLOAT,  // color
                    3, GL.GL_FLOAT,  // normal
                    0, GL.GL_FLOAT); // texture

            vbo.glBegin(gl.GL_TRIANGLES);
            vbo.glColor4f(0, 1f, 0, 1f);
            vbo.glNormal3f(normal.x, normal.y, normal.z);
            vbo.glVertex3f(triangle[0].x, triangle[0].y, triangle[0].z);

            vbo.glColor4f(0, 1f, 0, 1f);
            vbo.glNormal3f(normal.x, normal.y, normal.z);
            vbo.glVertex3f(triangle[1].x, triangle[1].y, triangle[1].z);

            vbo.glColor4f(0, 1f, 0, 1f);
            vbo.glNormal3f(normal.x, normal.y, normal.z);
            vbo.glVertex3f(triangle[2].x, triangle[2].y, triangle[2].z);
            vbo.glEnd(gl);

			/*glBegin(gl.GL_LINES);
			glColor3f(1, 1, 0);
			glNormal3d(normal.getX(),normal.getY(),normal.getZ());
			glVertex3d(triangle[0].getX(), triangle[0].getY(), triangle[0].getZ());
			glNormal3d(normal.getX(),normal.getY(),normal.getZ());
			glVertex3d(triangle[1].getX(), triangle[1].getY(), triangle[1].getZ());
			glColor3f(1, 1, 0);
			glNormal3d(normal.getX(),normal.getY(),normal.getZ());
			glVertex3d(triangle[2].getX(), triangle[2].getY(), triangle[2].getZ());
			glNormal3d(normal.getX(),normal.getY(),normal.getZ());
			glVertex3d(triangle[1].getX(), triangle[1].getY(), triangle[1].getZ());
			glColor3f(1, 1, 0);
			glNormal3d(normal.getX(),normal.getY(),normal.getZ());
			glVertex3d(triangle[2].getX(), triangle[2].getY(), triangle[2].getZ());
			glNormal3d(normal.getX(),normal.getY(),normal.getZ());
			glVertex3d(triangle[0].getX(), triangle[0].getY(), triangle[0].getZ());
			glEnd();*/
        }
    }

    private static class GlDrawcallback extends TriangleCallback {
        private final GL gl;
        public boolean wireframe = false;

        public GlDrawcallback(GL gl) {
            this.gl = gl;
        }

        public void processTriangle(v3[] triangle, int partId, int triangleIndex) {
            ImmModeSink vbo = ImmModeSink.createFixed(GL.GL_STATIC_DRAW, 10,
                    3, GL.GL_FLOAT,  // vertex
                    4, GL.GL_FLOAT,  // color
                    0, GL.GL_FLOAT,  // normal
                    0, GL.GL_FLOAT); // texture
            if (wireframe) {
                vbo.glBegin(gl.GL_LINES);
                vbo.glColor4f(1, 0, 0, 1);
                vbo.glVertex3f(triangle[0].x, triangle[0].y, triangle[0].z);
                vbo.glVertex3f(triangle[1].x, triangle[1].y, triangle[1].z);
                vbo.glColor4f(0, 1, 0, 1);
                vbo.glVertex3f(triangle[2].x, triangle[2].y, triangle[2].z);
                vbo.glVertex3f(triangle[1].x, triangle[1].y, triangle[1].z);
                vbo.glColor4f(0, 0, 1, 1);
                vbo.glVertex3f(triangle[2].x, triangle[2].y, triangle[2].z);
                vbo.glVertex3f(triangle[0].x, triangle[0].y, triangle[0].z);
                vbo.glEnd(gl);
            } else {
                vbo.glBegin(gl.GL_TRIANGLES);
                vbo.glColor4f(1, 0, 0, 1);
                vbo.glVertex3f(triangle[0].x, triangle[0].y, triangle[0].z);
                vbo.glColor4f(0, 1, 0, 1);
                vbo.glVertex3f(triangle[1].x, triangle[1].y, triangle[1].z);
                vbo.glColor4f(0, 0, 1, 1);
                vbo.glVertex3f(triangle[2].x, triangle[2].y, triangle[2].z);
                vbo.glEnd(gl);
            }
        }
    }

    private static class TriangleGlDrawcallback extends InternalTriangleIndexCallback {
        private final GL gl;

        public TriangleGlDrawcallback(GL gl) {
            this.gl = gl;
        }

        public void internalProcessTriangleIndex(v3[] triangle, int partId, int triangleIndex) {
            ImmModeSink vbo = ImmModeSink.createFixed(GL.GL_STATIC_DRAW, 10,
                    3, GL.GL_FLOAT,  // vertex
                    4, GL.GL_FLOAT,  // color
                    0, GL.GL_FLOAT,  // normal
                    0, GL.GL_FLOAT); // texture
            vbo.glBegin(gl.GL_TRIANGLES);//LINES);
            vbo.glColor4f(1, 0, 0, 1);
            vbo.glVertex3f(triangle[0].x, triangle[0].y, triangle[0].z);
            vbo.glVertex3f(triangle[1].x, triangle[1].y, triangle[1].z);
            vbo.glColor4f(0, 1, 0, 1);
            vbo.glVertex3f(triangle[2].x, triangle[2].y, triangle[2].z);
            vbo.glVertex3f(triangle[1].x, triangle[1].y, triangle[1].z);
            vbo.glColor4f(0, 0, 1, 1);
            vbo.glVertex3f(triangle[2].x, triangle[2].y, triangle[2].z);
            vbo.glVertex3f(triangle[0].x, triangle[0].y, triangle[0].z);
            vbo.glEnd(gl);
        }
    }

}
