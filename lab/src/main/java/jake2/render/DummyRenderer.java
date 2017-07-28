/*
 * DummyRenderer.java
 * Copyright (C) 2003
 *
 * $Id: DummyRenderer.java,v 1.2 2005-02-07 22:37:55 cawe Exp $
 */
 
package jake2.render;

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.newt.MonitorMode;
import jake2.client.refdef_t;
import jake2.client.refexport_t;
import jake2.qcommon.xcommand_t;
import jake2.sys.KBD;

import java.util.ArrayList;
import java.util.List;

/**
 * DummyRenderer
 * 
 * @author cwei
 */
public class DummyRenderer implements refexport_t {

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#Init(int, int)
	 */
	@Override
    public boolean Init(int vid_xpos, int vid_ypos) {
		return false;
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#Shutdown()
	 */
	@Override
    public void Shutdown() {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#BeginRegistration(java.lang.String)
	 */
	@Override
    public void BeginRegistration(String map) {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#RegisterModel(java.lang.String)
	 */
	@Override
    public model_t RegisterModel(String name) {
		return null;
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#RegisterSkin(java.lang.String)
	 */
	@Override
    public image_t RegisterSkin(String name) {
		return null;
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#RegisterPic(java.lang.String)
	 */
	@Override
    public image_t RegisterPic(String name) {
		return null;
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#SetSky(java.lang.String, float, float[])
	 */
	@Override
    public void SetSky(String name, float rotate, float[] axis) {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#EndRegistration()
	 */
	@Override
    public void EndRegistration() {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#RenderFrame(jake2.client.refdef_t)
	 */
	@Override
    public void RenderFrame(refdef_t fd) {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#DrawGetPicSize(Dimension, java.lang.String)
	 */
	@Override
    public void DrawGetPicSize(Dimension dim, String name) {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#DrawPic(int, int, java.lang.String)
	 */
	@Override
    public void DrawPic(int x, int y, String name) {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#DrawStretchPic(int, int, int, int, java.lang.String)
	 */
	@Override
    public void DrawStretchPic(int x, int y, int w, int h, String name) {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#DrawChar(int, int, int)
	 */
	@Override
    public void DrawChar(int x, int y, int num) {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#DrawTileClear(int, int, int, int, java.lang.String)
	 */
	@Override
    public void DrawTileClear(int x, int y, int w, int h, String name) {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#DrawFill(int, int, int, int, int)
	 */
	@Override
    public void DrawFill(int x, int y, int w, int h, int c) {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#DrawFadeScreen()
	 */
	@Override
    public void DrawFadeScreen() {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#DrawStretchRaw(int, int, int, int, int, int, byte[])
	 */
	@Override
    public void DrawStretchRaw(int x, int y, int w, int h, int cols, int rows, byte[] data) {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#CinematicSetPalette(byte[])
	 */
	@Override
    public void CinematicSetPalette(byte[] palette) {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#BeginFrame(float)
	 */
	@Override
    public boolean BeginFrame(float camera_separation) {
	    return true;
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#EndFrame()
	 */
	@Override
    public void EndFrame() {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#AppActivate(boolean)
	 */
	@Override
    public void AppActivate(boolean activate) {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#updateScreen(jake2.qcommon.xcommand_t)
	 */
	@Override
    public void updateScreen(xcommand_t callback) {
	    callback.execute();
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#apiVersion()
	 */
	@Override
    public int apiVersion() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#getModeList()
	 */
	@Override
    public List<MonitorMode> getModeList() {
		return new ArrayList<>();
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#getKeyboardHandler()
	 */
	@Override
    public KBD getKeyboardHandler() {
		return null;
	}

}
