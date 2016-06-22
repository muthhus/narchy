/*
 * gleem -- OpenGL Extremely Easy-To-Use Manipulators.
 * Copyright (C) 1998-2003 Kenneth B. Russell (kbrussel@alum.mit.edu)
 *
 * Copying, distribution and use of this software in source and binary
 * forms, with or without modification, is permitted provided that the
 * following conditions are met:
 *
 * Distributions of source code must reproduce the copyright notice,
 * this list of conditions and the following disclaimer in the source
 * code header files; and Distributions of binary code must reproduce
 * the copyright notice, this list of conditions and the following
 * disclaimer in the documentation, Read me file, license file and/or
 * other materials provided with the software distribution.
 *
 * The names of Sun Microsystems, Inc. ("Sun") and/or the copyright
 * holder may not be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS," WITHOUT A WARRANTY OF ANY
 * KIND. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, NON-INTERFERENCE, ACCURACY OF
 * INFORMATIONAL CONTENT OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. THE
 * COPYRIGHT HOLDER, SUN AND SUN'S LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL THE
 * COPYRIGHT HOLDER, SUN OR SUN'S LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGES. YOU ACKNOWLEDGE THAT THIS SOFTWARE IS NOT
 * DESIGNED, LICENSED OR INTENDED FOR USE IN THE DESIGN, CONSTRUCTION,
 * OPERATION OR MAINTENANCE OF ANY NUCLEAR FACILITY. THE COPYRIGHT
 * HOLDER, SUN AND SUN'S LICENSORS DISCLAIM ANY EXPRESS OR IMPLIED
 * WARRANTY OF FITNESS FOR SUCH USES.
 */

package gleem;

import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import gleem.linalg.Vec2f;
import gleem.linalg.Vec3f;

import java.util.*;

/**
 * The ManipManager handles making manipulators visible in a
 * window.
 */

public class ManipManager {

    private final GLAutoDrawable gl;
    private final WindowInfo info;

    // Screen-to-ray mapping
    private ScreenToRayMapping mapping;
    // Maps GLDrawables to WindowInfos
    //private final Map windowToInfoMap = new HashMap();
    // Maps Manips to Set<GLAutoDrawable>
    //private final Map manipToWindowMap = new HashMap();

    // MouseAdapter for this
    private final MouseAdapter mouseListener = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            ManipManager.this.mousePressed(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            ManipManager.this.mouseReleased(e);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            ManipManager.this.mouseDragged(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            ManipManager.this.mouseMoved(e);
        }
    };
    private final WindowUpdateListener defaultWindowListener = new WindowUpdateListener() {
        @Override
        public void update(GLAutoDrawable window) {
            //window.display(); //repaint();
        }
    };
    private WindowUpdateListener windowListener;

    public void render(CameraParameters c) {
        updateCameraParameters(c);
        render();
    }

    class WindowInfo {
        /**
         * Set<Manip>
         */
        final Set<Manip> manips = new LinkedHashSet();
        final CameraParameters params = new CameraParameters();
        Manip curHighlightedManip;
        ManipPart curHighlightedManipPart;
        // Current manip
        Manip active;
        // Dragging?
        boolean dragging;
    }

    public ManipManager(GLAutoDrawable gl) {
        super();
        this.gl = gl;
        this.info = new WindowInfo();
        mapping = new RightTruncPyrMapping();
        setWindowUpdateListener(null);
    }

//    /**
//     * This class is a singleton. Get the sole instance of the
//     * ManipManager.
//     */
//    public static synchronized ManipManager getManipManager() {
//        if (soleInstance == null) {
//            soleInstance = new ManipManager();
//        }
//        return soleInstance;
//    }

    /**
     * Make the ManipManager aware of the existence of a given
     * window. This causes mouse and mouse motion listeners to be
     * installed on this window; see setupMouseListeners, below.
     */
    public synchronized void registerWindow(GLAutoDrawable window) {
        setupMouseListeners(window);
    }

//    /**
//     * Remove all references to a given window, including removing all
//     * manipulators from it.
//     */
//    public synchronized void unregisterWindow(GLAutoDrawable window) {
//        if (window == null) {
//            return;
//        }
//        WindowInfo info = (WindowInfo) windowToInfoMap.get(window);
//        if (info != null) {
//            Object[] manips = info.manips.toArray();
//            for (int i = 0; i < manips.length; i++) {
//                removeManipFromWindow((Manip) manips[i], window);
//            }
//            windowToInfoMap.remove(window);
//            removeMouseListeners(window);
//        }
//    }

    /**
     * Make a given manipulator visible and active in a given
     * window. The window must be registered.
     */
    public synchronized void showManipInWindow(Manip manip) {

        info.manips.add(manip);
//        Set windows = (Set) manipToWindowMap.get(manip);
//        if (windows == null) {
//            windows = new HashSet();
//            manipToWindowMap.put(manip, windows);
//        }
//        windows.add(window);
    }

    /**
     * Remove a given manipulator from a given window. The window must
     * be registered.
     */
    public synchronized void removeManipFromWindow(Manip manip, GLAutoDrawable window) {
        //WindowInfo info = (WindowInfo) windowToInfoMap.get(window);
        if (info == null) {
            throw new RuntimeException("Window not registered");
        }
        if (!info.manips.remove(manip)) {
            throw new RuntimeException("Manip not registered in window");
        }
        //Set windows = (Set) manipToWindowMap.get(manip);
        //assert windows != null;
        //windows.remove(window);
    }

    /**
     * This must be called for a registered window every time the
     * camera parameters of the window change.
     */
    public synchronized void updateCameraParameters(CameraParameters params) {
        info.params.set(params);
    }

    /**
     * Allows changing of the screen-to-ray mapping. Default is a
     * RightTruncPyrMapping.
     */
    public synchronized void setScreenToRayMapping(ScreenToRayMapping mapping) {
        this.mapping = mapping;
    }

    /**
     * Returns the current screen-to-ray mapping.
     */
    public synchronized ScreenToRayMapping getScreenToRayMapping() {
        return mapping;
    }

    /**
     * Sets the WindowUpdateListener the ManipManager uses to force
     * repainting of windows in which manipulators have moved. The
     * default implementation, which can be restored by passing a null
     * listener argument to this method, calls repaint() on the
     * GLAutoDrawable if it is not a GLRunnable instance (i.e., a
     * GLAnimCanvas or GLAnimJPanel, which redraw themselves
     * automatically).
     */
    public synchronized void setWindowUpdateListener(WindowUpdateListener listener) {
        if (listener != null) {
            windowListener = listener;
        } else {
            windowListener = defaultWindowListener;
        }
    }

    /**
     * Cause the manipulators for a given window to be drawn. The
     * drawing occurs immediately; this routine must be called when an
     * OpenGL context is valid, i.e., from within the display() method
     * of a GLEventListener.
     */
    public synchronized void render() {
        info.manips.forEach( this::render );
    }

    public void render( Manip m) {
        m.render(gl.getGL().getGL2());
    }

    /**
     * Sets up a MouseListener and MouseMotionListener for the given
     * window. Since an application-level MouseListener or
     * MouseMotionListener might want to intercept events and not pass
     * them on to the ManipManager without relying on the ordering of
     * listeners for the canvas (see the ExaminerViewer class), the
     * setupMouseListeners and removeMouseListeners routines, as well
     * as the appropriate delegate routines, are made public here.
     */
    public synchronized void setupMouseListeners(GLAutoDrawable window) {
        //window.addMouseMotionListener(mouseMotionListener);
        ((GLWindow) window).addMouseListener(mouseListener);
    }

    /**
     * Removes the automatically-installed mouse listeners for the
     * given window. This allows application code to determine the
     * policy for intercepting mouse events.
     */
    public synchronized void removeMouseListeners(GLAutoDrawable window) {
        //((GLWindow)window).removeMouseMotionListener(mouseMotionListener);
        ((GLWindow) window).removeMouseListener(mouseListener);
    }

    /**
     * The ManipManager watches for the following events: mouseMoved,
     * mouseDragged, mousePressed, and mouseReleased. These routines
     * are exposed so application-level code can intercept events when
     * certain modifier keys are depressed.
     */
    public synchronized void mouseMoved(MouseEvent e) {
        passiveMotionMethod(gl, e.getX(), e.getY());
    }

    /**
     * The ManipManager watches for the following events: mouseMoved,
     * mouseDragged, mousePressed, and mouseReleased. These routines
     * are exposed so application-level code can intercept events when
     * certain modifier keys are depressed.
     */
    public synchronized void mouseDragged(MouseEvent e) {
        motionMethod(gl, e.getX(), e.getY());
    }

    /**
     * The ManipManager watches for the following events: mouseMoved,
     * mouseDragged, mousePressed, and mouseReleased. These routines
     * are exposed so application-level code can intercept events when
     * certain modifier keys are depressed.
     */
    public synchronized boolean mousePressed(MouseEvent e) {
        return mouseMethod(gl, e.getModifiers(),
                true, e.getX(), e.getY());
    }

    /**
     * The ManipManager watches for the following events: mouseMoved,
     * mouseDragged, mousePressed, and mouseReleased. These routines
     * are exposed so application-level code can intercept events when
     * certain modifier keys are depressed.
     */
    public synchronized boolean mouseReleased(MouseEvent e) {
        return mouseMethod(gl, e.getModifiers(),
                false, e.getX(), e.getY());
    }

    //----------------------------------------------------------------------
    // Internals only below this point
    //


    private void motionMethod(GLAutoDrawable window, int x, int y) {
        if (info.dragging) {
            final WindowInfo info = this.info;

            // Compute ray in 3D
            Vec3f rayStart = new Vec3f();
            Vec3f rayDirection = new Vec3f();
            computeRay(info.params, x, y, rayStart, rayDirection);
            info.active.drag(rayStart, rayDirection);
            fireUpdate(info.active);
        }
    }

    private void passiveMotionMethod(GLAutoDrawable window, int x, int y) {
        final WindowInfo info = this.info;

        // Compute ray in 3D
        Vec3f rayStart = new Vec3f();
        Vec3f rayDirection = new Vec3f();
        computeRay(info.params, x, y, rayStart, rayDirection);
        // Compute all hits
        List hits = new ArrayList();
        for (Iterator iter = info.manips.iterator(); iter.hasNext(); ) {
            ((Manip) iter.next()).intersectRay(rayStart, rayDirection, hits);
        }
        // Find closest one
        HitPoint hp = null;
        for (Iterator iter = hits.iterator(); iter.hasNext(); ) {
            HitPoint cur = (HitPoint) iter.next();
            if ((hp == null) || (cur.intPt.t < hp.intPt.t)) {
                hp = cur;
            }
        }
        if (info.curHighlightedManip != null &&
                (hp == null ||
                        hp.manipulator != info.curHighlightedManip ||
                        hp.manipPart != info.curHighlightedManipPart)) {
            info.curHighlightedManip.clearHighlight();
            fireUpdate(info.curHighlightedManip);
        }
        if (hp != null) {
            if (hp.manipulator != info.curHighlightedManip ||
                    hp.manipPart != info.curHighlightedManipPart) {
                // Highlight manip
                info.curHighlightedManip = hp.manipulator;
                info.curHighlightedManipPart = hp.manipPart;
                info.curHighlightedManip.highlight(hp);
                fireUpdate(info.curHighlightedManip);
            }
        } else {
            info.curHighlightedManip = null;
        }
    }

    private boolean mouseMethod(GLAutoDrawable window, int modifiers,
                             boolean isPress, int x, int y) {
        /*if ((modifiers & InputEvent.BUTTON1_MASK) != 0)*/

        final WindowInfo info = this.info;
            if (isPress) {
                // Compute ray in 3D
                Vec3f rayStart = new Vec3f();
                Vec3f rayDirection = new Vec3f();
                computeRay(info.params, x, y, rayStart, rayDirection);
                // Compute all hits
                List hits = new ArrayList();
                for (Iterator iter = info.manips.iterator(); iter.hasNext(); ) {
                    ((Manip) iter.next()).intersectRay(rayStart, rayDirection, hits);
                }
                // Find closest one
                HitPoint hp = null;
                for (Iterator iter = hits.iterator(); iter.hasNext(); ) {
                    HitPoint cur = (HitPoint) iter.next();
                    if ((hp == null) || (cur.intPt.t < hp.intPt.t)) {
                        hp = cur;
                    }
                }
                if (hp != null) {
                    if (info.curHighlightedManip != null) {
                        info.curHighlightedManip.clearHighlight();
                        fireUpdate(info.curHighlightedManip);
                        info.curHighlightedManip = null;
                    }

                    if ((modifiers & InputEvent.SHIFT_MASK) != 0) {
                        hp.shiftDown = true;
                    }

                    hp.manipulator.makeActive(hp);
                    info.active = hp.manipulator;
                    info.dragging = true;
                    fireUpdate(info.active);
                }
            } else {
                if (info.active != null) {
                    info.active.makeInactive();
                    info.dragging = false;
                    fireUpdate(info.active);
                    info.active = null;
                    // Check to see where mouse is
                    passiveMotionMethod(window, x, y);
                }
            }

        return info.active != null;
    }

    private Vec2f screenToNormalizedCoordinates(CameraParameters params,
                                                int x, int y) {
        // 's origin is upper left
        return new Vec2f(
                (((float) x / (float) (params.xSize - 1)) - 0.5f) * 2.0f,
                (0.5f - ((float) y / (float) (params.ySize - 1))) * 2.0f
        );
    }

    private void computeRay(CameraParameters params, int x, int y,
                            Vec3f raySource, Vec3f rayDirection) {
        if (mapping == null) {
            throw new RuntimeException("Screen to ray mapping was unspecified");
        }
        mapping.mapScreenToRay(screenToNormalizedCoordinates(params, x, y),
                params,
                raySource,
                rayDirection);
    }

    private void fireUpdate(Manip manip) {
//        Set windows = (Set) manipToWindowMap.get(manip);
//        assert windows != null;
//        for (Iterator iter = windows.iterator(); iter.hasNext(); ) {
//            windowListener.update((GLAutoDrawable) iter.next());
//        }
        windowListener.update(gl);
    }
}
