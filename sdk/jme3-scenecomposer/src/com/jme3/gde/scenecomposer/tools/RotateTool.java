/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer.tools;

import com.jme3.asset.AssetManager;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.sceneexplorer.nodes.JmeSpatial;
import com.jme3.gde.core.undoredo.AbstractUndoableSceneEdit;
import com.jme3.gde.scenecomposer.SceneComposerToolController;
import com.jme3.gde.scenecomposer.SceneEditTool;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

/**
 *
 * @author kbender
 */
public class RotateTool extends SceneEditTool {

    private Vector3f pickedMarker;
    private Vector2f lastScreenCoord;
    private Quaternion startRotate;
    private Quaternion lastRotate;
    private boolean wasDragging = false;
    private PickManager pickManager;

    public RotateTool() {
        axisPickType = AxisMarkerPickType.planeOnly;
        setOverrideCameraControl(true);
    }

    @Override
    public void activate(AssetManager manager, Node toolNode, Node onTopToolNode, Spatial selectedSpatial, SceneComposerToolController toolController) {
        super.activate(manager, toolNode, onTopToolNode, selectedSpatial, toolController);
        pickManager = Lookup.getDefault().lookup(PickManager.class);
        displayPlanes();
    }

    @Override
    public void actionPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject dataObject) {
        if (!pressed) {
            setDefaultAxisMarkerColors();
            pickedMarker = null; // mouse released, reset selection
            lastScreenCoord = null;
            if (wasDragging) {
                actionPerformed(new ScaleUndo(toolController.getSelectedSpatial(), startRotate, lastRotate));
                wasDragging = false;
            }
        }
    }

    @Override
    public void actionSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject dataObject) {

    }

    @Override
    public void mouseMoved(Vector2f screenCoord, JmeNode rootNode, DataObject currentDataObject, JmeSpatial selectedSpatial) {
        if (pickedMarker == null) {
            highlightAxisMarker(camera, screenCoord, axisPickType);
        } else {
            pickedMarker = null;
        }
    }

    @Override
    public void draggedPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {

        if (!pressed) {
            setDefaultAxisMarkerColors();
            pickedMarker = null; // mouse released, reset selection
            lastScreenCoord = null;

            if (wasDragging) {
                actionPerformed(new ScaleUndo(toolController.getSelectedSpatial(), startRotate, lastRotate));
                wasDragging = false;
            }
            return;
        }

        if (toolController.getSelectedSpatial() == null) {
            return;
        }

        if (pickedMarker == null) {
            pickedMarker = pickAxisMarker(camera, screenCoord, axisPickType);
            if (pickedMarker == null) {
                return;
            }

            if (pickedMarker.equals(QUAD_XY)) {
                pickManager.initiatePick(toolController.getSelectedSpatial(), PickManager.PLANE_XY,
                        PickManager.TransformationType.local, camera, screenCoord);
            } else if (pickedMarker.equals(QUAD_XZ)) {
                pickManager.initiatePick(toolController.getSelectedSpatial(), PickManager.PLANE_XZ,
                        PickManager.TransformationType.local, camera, screenCoord);
            } else if (pickedMarker.equals(QUAD_YZ)) {
                pickManager.initiatePick(toolController.getSelectedSpatial(), PickManager.PLANE_YZ,
                        PickManager.TransformationType.local, camera, screenCoord);
            }
            startRotate = toolController.getSelectedSpatial().getLocalRotation().clone();
        }
        if (!pickManager.updatePick(camera, screenCoord)) {
            return;
        }

        if (pickedMarker.equals(QUAD_XY) || pickedMarker.equals(QUAD_XZ) || pickedMarker.equals(QUAD_YZ)) {
            Quaternion rotation = startRotate.mult(pickManager.getLocalRotation());
            toolController.getSelectedSpatial().setLocalRotation(rotation);
            lastRotate = rotation;
        }
        updateToolsTransformation();
        wasDragging = true;
    }

    @Override
    public
            void draggedSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {

    }

    private class ScaleUndo extends AbstractUndoableSceneEdit {

        private Spatial spatial;
        private Quaternion before, after;

        ScaleUndo(Spatial spatial, Quaternion before, Quaternion after) {
            this.spatial = spatial;
            this.before = before;
            this.after = after;
        }

        @Override
        public void sceneUndo() {
            spatial.setLocalRotation(before);
            toolController.selectedSpatialTransformed();
        }

        @Override
        public void sceneRedo() {
            spatial.setLocalRotation(after);
            toolController.selectedSpatialTransformed();
        }
    }
}
