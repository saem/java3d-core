/*
 * $RCSfile$
 *
 * Copyright 1997-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 *
 * $Revision$
 * $Date$
 * $State$
 */

package javax.media.j3d;

import javax.vecmath.Color3f;
import java.util.Enumeration;

/**
 * Color interpolation behavior.  This class defines a behavior that
 * modifies the ambient, emissive, diffuse, or specular color of its
 * target material object by linearly interpolating between a pair of
 * specified colors, using the value generated by the specified Alpha
 * object.
 * The behavior modifies the color specified by the
 * Material's colorTarget attribute, one of: AMBIENT, EMISSIVE,
 * DIFFUSE, SPECULAR, or AMBIENT_AND_DIFFUSE.
 * The ALLOW_COMPONENT_READ bit must be set in the  Material object in
 * order for the Material's colorTarget to be read.
 * If the Material object's ALLOW_COMPONENT_READ bit is <i>not</i> set, the
 * diffuse component will be modified.
 *
 * @see Material
 */

public class ColorInterpolator extends Interpolator {

    Material target;
    Color3f startColor = new Color3f();
    Color3f endColor = new Color3f();
    Color3f newColor = new Color3f();

    // We can't use a boolean flag since it is possible 
    // that after alpha change, this procedure only run
    // once at alpha.finish(). So the best way is to
    // detect alpha value change.
    private float prevAlphaValue = Float.NaN;
    private int prevColorTarget = -1;
    private WakeupCriterion passiveWakeupCriterion = 
	(WakeupCriterion) new WakeupOnElapsedFrames(0, true);

    // non-public, no parameter constructor used by cloneNode
    ColorInterpolator() {
    }

    /**
     * Constructs a trivial color interpolator with a specified target,
     * a starting color of black, and an ending color of white.
     * @param alpha the alpha object for this interpolator
     * @param target the material component object whose
     * color is affected by this color interpolator
     */
    public ColorInterpolator(Alpha alpha,
			     Material target) {

	super(alpha);

	this.target = target;
	this.startColor.set(0.0f, 0.0f, 0.0f);
	this.endColor.set(1.0f, 1.0f, 1.0f);
    }

    /**
     * Constructs a color interpolator with the specified target,
     * starting color, and ending color.
     * @param alpha the alpha object for this interpolator
     * @param target the material component object whose
     * color is affected by this color interpolator
     * @param startColor the starting color
     * @param endColor the ending color
     */
    public ColorInterpolator(Alpha alpha,
			     Material target,
			     Color3f startColor,
			     Color3f endColor) {

	super(alpha);

	this.target = target;
	this.startColor.set(startColor);
	this.endColor.set(endColor);
    }

    /**
      * This method sets the startColor for this interpolator.
      * @param color the new start color
      */
    public void setStartColor(Color3f color) {
	startColor.set(color);
	prevAlphaValue = Float.NaN;
    }

    /**
      * This method retrieves this interpolator's startColor.
      * @param color the vector that will receive the interpolator's start color
      */
    public void getStartColor(Color3f color) {
	color.set(startColor);
    }

    /**
      * This method sets the endColor for this interpolator.
      * @param color the new end color
      */
    public void setEndColor(Color3f color) {
	endColor.set(color);
	prevAlphaValue = Float.NaN;
    }

    /**
      * This method retrieves this interpolator's endColor.
      * @param color the vector that will receive the interpolator's end color
      */
    public void getEndColor(Color3f color) {
	color.set(endColor);
    }

    /**
     * This method sets the target material component object for 
     * this interpolator.
     * @param target the material component object whose
     * color is affected by this color interpolator
     */
    public void setTarget(Material target) {
	this.target = target;
	prevAlphaValue = Float.NaN;
    }

    /**
      * This method retrieves this interpolator's target material
      * component object.
      * @return the interpolator's target material component object
      */
    public Material getTarget() {
	return target;
    }

    // The ColorInterpolator's initialize routine uses the default 
    // initialization routine.

    /**
     * This method is invoked by the behavior scheduler every frame.
     * It maps the alpha value that corresponds to the current time
     * into a color value and updates the ambient, emissive, diffuse,
     * or specular color (or both the ambient and diffuse color) of
     * the specified target Material object with this new color value.
     *
     * @param criteria an enumeration of the criteria that caused the
     * stimulus
     */
    public void processStimulus(Enumeration criteria) {

	// Handle stimulus
	WakeupCriterion criterion = passiveWakeupCriterion;

	if (alpha != null) {
	    float value = alpha.value();

	    int colorTarget = Material.DIFFUSE;
	    if (target.getCapability(Material.ALLOW_COMPONENT_READ))
		colorTarget = target.getColorTarget();

	    if (value != prevAlphaValue || colorTarget != prevColorTarget) {
		newColor.x = (1.0f-value)*startColor.x + value*endColor.x;
		newColor.y = (1.0f-value)*startColor.y + value*endColor.y;
		newColor.z = (1.0f-value)*startColor.z + value*endColor.z;

		switch (colorTarget) {
		case Material.AMBIENT:
		    target.setAmbientColor(newColor);
		    break;
		case Material.AMBIENT_AND_DIFFUSE:
		    target.setAmbientColor(newColor);
		    // fall through
		case Material.DIFFUSE:
		    target.setDiffuseColor(newColor);
		    break;
		case Material.EMISSIVE:
		    target.setEmissiveColor(newColor);
		    break;
		case Material.SPECULAR:
		    target.setSpecularColor(newColor);
		    break;
		}

		prevAlphaValue = value;
		prevColorTarget = colorTarget;
	    }

	    if (!alpha.finished() && !alpha.isPaused()) {
		criterion = defaultWakeupCriterion;
	    }
	}
	wakeupOn(criterion);
    }

    /**
     * Used to create a new instance of the node.  This routine is called
     * by <code>cloneTree</code> to duplicate the current node.
     * @param forceDuplicate when set to <code>true</code>, causes the
     *  <code>duplicateOnCloneTree</code> flag to be ignored.  When
     *  <code>false</code>, the value of each node's
     *  <code>duplicateOnCloneTree</code> variable determines whether
     *  NodeComponent data is duplicated or copied.
     *
     * @see Node#cloneTree
     * @see Node#cloneNode
     * @see Node#duplicateNode
     * @see NodeComponent#setDuplicateOnCloneTree
     */
    public Node cloneNode(boolean forceDuplicate) {
        ColorInterpolator ci = new ColorInterpolator();
        ci.duplicateNode(this, forceDuplicate);
        return ci;
    }
   

   /**
     * Copies all ColorInterpolator information from
     * <code>originalNode</code> into
     * the current node.  This method is called from the
     * <code>cloneNode</code> method which is, in turn, called by the
     * <code>cloneTree</code> method.<P> 
     *
     * @param originalNode the original node to duplicate.
     * @param forceDuplicate when set to <code>true</code>, causes the
     *  <code>duplicateOnCloneTree</code> flag to be ignored.  When
     *  <code>false</code>, the value of each node's
     *  <code>duplicateOnCloneTree</code> variable determines whether
     *  NodeComponent data is duplicated or copied.
     *
     * @exception RestrictedAccessException if this object is part of a live
     *  or compiled scenegraph.
     *
     * @see Node#duplicateNode
     * @see Node#cloneTree
     * @see NodeComponent#setDuplicateOnCloneTree
     */
    void duplicateAttributes(Node originalNode, boolean forceDuplicate) {
        super.duplicateAttributes(originalNode, forceDuplicate);
	
	ColorInterpolator ci = (ColorInterpolator) originalNode;

        ci.getStartColor(startColor);
        ci.getEndColor(endColor);

	// this reference will be updated in updateNodeReferences()
        setTarget(ci.getTarget());
    }

    /**
     * Callback used to allow a node to check if any scene graph objects
     * referenced
     * by that node have been duplicated via a call to <code>cloneTree</code>.
     * This method is called by <code>cloneTree</code> after all nodes in
     * the sub-graph have been duplicated. The cloned Leaf node's method
     * will be called and the Leaf node can then look up any object references
     * by using the <code>getNewObjectReference</code> method found in the
     * <code>NodeReferenceTable</code> object.  If a match is found, a
     * reference to the corresponding object in the newly cloned sub-graph
     * is returned.  If no corresponding reference is found, either a
     * DanglingReferenceException is thrown or a reference to the original
     * object is returned depending on the value of the
     * <code>allowDanglingReferences</code> parameter passed in the
     * <code>cloneTree</code> call.
     * <p>
     * NOTE: Applications should <i>not</i> call this method directly.
     * It should only be called by the cloneTree method.
     *
     * @param referenceTable a NodeReferenceTableObject that contains the
     *  <code>getNewObjectReference</code> method needed to search for
     *  new object instances.
     * @see NodeReferenceTable
     * @see Node#cloneTree
     * @see DanglingReferenceException
     */
    public void updateNodeReferences(NodeReferenceTable referenceTable) {
        super.updateNodeReferences(referenceTable);

        // check Material
        NodeComponent nc = getTarget();

        if (nc != null) {
            setTarget((Material) referenceTable.getNewObjectReference(nc));
        }
    }
}