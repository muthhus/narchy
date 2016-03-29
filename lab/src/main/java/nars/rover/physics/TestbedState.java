///*******************************************************************************
// * Copyright (c) 2013, Daniel Murphy
// * All rights reserved.
// *
// * Redistribution and use in source and binary forms, with or without modification,
// * are permitted provided that the following conditions are met:
// *  * Redistributions of source code must retain the above copyright notice,
// *    this list of conditions and the following disclaimer.
// *  * Redistributions in binary form must reproduce the above copyright notice,
// *    this list of conditions and the following disclaimer in the documentation
// *    and/or other materials provided with the distribution.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
// * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
// * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
// * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
// * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// * POSSIBILITY OF SUCH DAMAGE.
// ******************************************************************************/
//package nars.rover.physics;
//
//import nars.rover.PhysicsModel;
//import nars.rover.physics.gl.AbstractJoglPanel;
//import org.jbox2d.callbacks.DebugDraw;
//import org.jbox2d.common.IViewportTransform;
//import org.jbox2d.common.Vec2;
//
//import java.util.List;
//import java.util.Vector;
//
///**
// * Model for the testbed
// *
// * @author Daniel
// */
//@Deprecated public class TestbedState {
//  //private final DefaultComboBoxModel tests = new DefaultComboBoxModel();
//  public final TestbedSettings settings = new TestbedSettings();
//  public PhysicsModel model;
//
//  private final boolean[] keys = new boolean[512];
//  private final boolean[] codedKeys = new boolean[512];
//  private float calculatedFps;
//  private int currTestIndex = -1;
//  private PhysicsModel runningTest;
//  private List<String> implSpecificHelp;
//  private AbstractJoglPanel panel;
//  private WorldCreator worldCreator = new DefaultWorldCreator();
//
//  public TestbedState() {}
//
//  public WorldCreator getWorldCreator() {
//    return worldCreator;
//  }
//
//  public void setWorldCreator(WorldCreator worldCreator) {
//    this.worldCreator = worldCreator;
//  }
//
//  public void setPanel(AbstractJoglPanel panel) {
//    this.panel = panel;
//  }
//
//  public AbstractJoglPanel getPanel() {
//    return panel;
//  }
//
//  public void setImplSpecificHelp(List<String> implSpecificHelp) {
//    this.implSpecificHelp = implSpecificHelp;
//  }
//
//  public List<String> getImplSpecificHelp() {
//    return implSpecificHelp;
//  }
//
//  public void setCalculatedFps(float calculatedFps) {
//    this.calculatedFps = calculatedFps;
//  }
//
//  public float getCalculatedFps() {
//    return calculatedFps;
//  }
//
////  public void setViewportTransform(IViewportTransform transform) {
////    draw.getViewportTranform().setExtents(transform.getExtents());
////    draw.getViewportTranform().setCenter(transform.getCenter());
////  }
//
//
//
//  public PhysicsModel getCurrTest() {
//    return model;
//  }
//
//  /**
//   * Gets the array of keys, index corresponding to the char value.
//   *
//   * @return
//   */
//  public boolean[] getKeys() {
//    return keys;
//  }
//
//
//
//  public int getCurrTestIndex() {
//    return currTestIndex;
//  }
//
//
//
//
//  public TestbedSettings getSettings() {
//    return settings;
//  }
//
//
//
//
//}
