/*
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
 */

package javax.media.j3d;

import javax.vecmath.Matrix4d;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3f;


/**
 * RotPosScalePathInterpolation behavior.  This class defines a behavior
 * that varies the rotational, translational, and scale components of its
 * target TransformGroup by linearly interpolating among a series of
 * predefined knot/position, knot/orientation, and knot/scale pairs
 * (using the value generated by the specified Alpha object).  The
 * interpolated position, orientation, and scale are used to generate
 * a transform in the local coordinate system of this interpolator.  The
 * first knot must have a value of 0.0.  The last knot must have a value
 * of 1.0.  An intermediate knot with index k must have a value strictly
 * greater than any knot with index less than k.
 */

public class RotPosScalePathInterpolator extends PathInterpolator {
    private Transform3D rotation = new Transform3D();

    private Vector3f pos = new Vector3f();
    private Quat4f tQuat = new Quat4f();
    private Matrix4d tMat = new Matrix4d();
    private Matrix4d sMat = new Matrix4d();

    // Arrays of quaternions, positions, and scales at each knot
    private Quat4f quats[];
    private Point3f positions[];
    private float scales[];

    private float prevInterpolationValue = Float.NaN;

    // We can't use a boolean flag since it is possible
    // that after alpha change, this procedure only run
    // once at alpha.finish(). So the best way is to
    // detect alpha value change.
    private float prevAlphaValue = Float.NaN;
    private WakeupCriterion passiveWakeupCriterion =
    (WakeupCriterion) new WakeupOnElapsedFrames(0, true);

    // non-public, default constructor used by cloneNode
    RotPosScalePathInterpolator() {
    }


    /**
     * Constructs a new RotPosScalePathInterpolator object that varies the
     * rotation, translation, and scale of the target TransformGroup's
     * transform.
     * @param alpha the alpha object for this interpolator.
     * @param target the TransformGroup node affected by this interpolator.
     * @param axisOfTransform the transform that specifies the local
     * coordinate system in which this interpolator operates.
     * @param knots an array of knot values that specify interpolation points.
     * @param quats an array of quaternion values at the knots.
     * @param positions an array of position values at the knots.
     * @param scales an array of scale component values at the knots.
     * @exception IllegalArgumentException if the lengths of the
     * knots, quats, positions, and scales arrays are not all the same.
     */
    public RotPosScalePathInterpolator(Alpha alpha,
				       TransformGroup target,
				       Transform3D axisOfTransform,
				       float[] knots,
				       Quat4f[] quats,
				       Point3f[] positions,
				       float[] scales) {
	super(alpha, target, axisOfTransform, knots);

	if (knots.length != quats.length)
	    throw new IllegalArgumentException(J3dI18N.getString("RotPosScalePathInterpolator1"));

	if (knots.length != positions.length)
	    throw new IllegalArgumentException(J3dI18N.getString("RotPosScalePathInterpolator0"));

	if (knots.length != scales.length)
	    throw new IllegalArgumentException(J3dI18N.getString("RotPosScalePathInterpolator2"));

	setPathArrays(quats, positions, scales);
    }


    /**
     * Sets the quat value at the specified index for this
     * interpolator.
     * @param index the index to be changed
     * @param quat the new quat value at index
     */
    public void setQuat(int index, Quat4f quat) {
	this.quats[index].set(quat);
    }


    /**
     * Retrieves the quat value at the specified index.
     * @param index the index of the value requested
     * @param quat returns the interpolator's quat value at the index
     */
    public void getQuat(int index, Quat4f quat) {
	quat.set(this.quats[index]);
    }


    /**
     * Sets the position value at the specified index for
     * this interpolator.
     * @param index the index to be changed
     * @param position the new position value at index
     */
    public void setPosition(int index, Point3f position) {
	this.positions[index].set(position);
    }


    /**
     * Retrieves the position value at the specified index.
     * @param index the index of the value requested
     * @param position returns the interpolator's position value at the index
     */
    public void getPosition(int index, Point3f position) {
	position.set(this.positions[index]);
    }


    /**
     * Sets the scale at the specified index for this
     * interpolator.
     * @param index the index to be changed
     * @param scale the new scale at index
     */
    public void setScale(int index, float scale) {
	this.scales[index] = scale;
    }


    /**
     * Retrieves the scale at the specified index.
     * @param index the index of the value requested
     * @return the interpolator's scale value at index
     */
    public float getScale(int index) {
	return this.scales[index];
    }


    /**
     * Replaces the existing arrays of knot values, quaternion
     * values, position values, and scale values with the specified arrays.
     * The arrays of knots, quats, positions, and scales are copied
     * into this interpolator object.
     * @param knots a new array of knot values that specify
     * interpolation points.
     * @param quats a new array of quaternion values at the knots.
     * @param positions a new array of position values at the knots.
     * @param scales a new array of scale component values at the knots.
     * @exception IllegalArgumentException if the lengths of the
     * knots, quats, positions, and scales arrays are not all the same.
     *
     * @since Java 3D 1.2
     */
    public void setPathArrays(float[] knots,
			      Quat4f[] quats,
			      Point3f[] positions,
			      float[] scales) {
	if (knots.length != quats.length)
	    throw new IllegalArgumentException(J3dI18N.getString("RotPosScalePathInterpolator1"));

	if (knots.length != positions.length)
	    throw new IllegalArgumentException(J3dI18N.getString("RotPosScalePathInterpolator0"));

	if (knots.length != scales.length)
	    throw new IllegalArgumentException(J3dI18N.getString("RotPosScalePathInterpolator2"));

	setKnots(knots);
	setPathArrays(quats, positions, scales);
    }


    // Set the specific arrays for this path interpolator
    private void setPathArrays(Quat4f[] quats,
			       Point3f[] positions,
			       float[] scales) {

	this.quats = new Quat4f[quats.length];
	for(int i = 0; i < quats.length; i++) {
	    this.quats[i] = new Quat4f();
	    this.quats[i].set(quats[i]);
	}

	this.positions = new Point3f[positions.length];
	for(int i = 0; i < positions.length; i++) {
	    this.positions[i] = new Point3f();
	    this.positions[i].set(positions[i]);
	}

	this.scales = new float[scales.length];
	for(int i = 0; i < scales.length; i++) {
	    this.scales[i] = scales[i];
	}
    }


    /**
     * Copies the array of quaternion values from this interpolator
     * into the specified array.
     * The array must be large enough to hold all of the quats.
     * The individual array elements must be allocated by the caller.
     * @param quats array that will receive the quats.
     *
     * @since Java 3D 1.2
     */
    public void getQuats(Quat4f[] quats) {
	for (int i = 0; i < this.quats.length; i++)  {
	    quats[i].set(this.quats[i]);
	}
    }


    /**
     * Copies the array of position values from this interpolator
     * into the specified array.
     * The array must be large enough to hold all of the positions.
     * The individual array elements must be allocated by the caller.
     * @param positions array that will receive the positions.
     *
     * @since Java 3D 1.2
     */
    public void getPositions(Point3f[] positions) {
	for (int i = 0; i < this.positions.length; i++)  {
	    positions[i].set(this.positions[i]);
	}
    }


    /**
     * Copies the array of scale values from this interpolator
     * into the specified array.
     * The array must be large enough to hold all of the scales.
     * @param scales array that will receive the scales.
     *
     * @since Java 3D 1.2
     */
    public void getScales(float[] scales) {
	for (int i = 0; i < this.scales.length; i++)  {
	    scales[i] = this.scales[i];
	}
    }

    /**
     * @deprecated As of Java 3D version 1.3, replaced by
     * <code>TransformInterpolator.setTransformAxis(Transform3D)</code>
     */

    public void setAxisOfRotPosScale(Transform3D axisOfRotPosScale) {
        setTransformAxis(axisOfRotPosScale);
    }

    /**
     * @deprecated As of Java 3D version 1.3, replaced by
     * <code>TransformInterpolator.geTransformAxis()</code>
     */
    public Transform3D getAxisOfRotPosScale() {
        return getTransformAxis();
    }



    /**
     * Computes the new transform for this interpolator for a given
     * alpha value.
     *
     * @param alphaValue alpha value between 0.0 and 1.0
     * @param transform object that receives the computed transform for
     * the specified alpha value
     *
     * @since Java 3D 1.3
     */
    public void computeTransform(float alphaValue, Transform3D transform) {

	float scale;
        double quatDot;

	computePathInterpolation(alphaValue);

	if (currentKnotIndex == 0 &&
	    currentInterpolationValue == 0f) {
	    tQuat.x = quats[0].x;
	    tQuat.y = quats[0].y;
	    tQuat.z = quats[0].z;
	    tQuat.w = quats[0].w;
	    pos.x = positions[0].x;
	    pos.y = positions[0].y;
	    pos.z = positions[0].z;
	    scale = scales[0];
	} else {
	    quatDot = quats[currentKnotIndex].x *
		quats[currentKnotIndex+1].x +
		quats[currentKnotIndex].y *
		quats[currentKnotIndex+1].y +
		quats[currentKnotIndex].z *
		quats[currentKnotIndex+1].z +
		quats[currentKnotIndex].w *
		quats[currentKnotIndex+1].w;
	    if (quatDot < 0) {
		tQuat.x = quats[currentKnotIndex].x +
		    (-quats[currentKnotIndex+1].x -
		     quats[currentKnotIndex].x)*currentInterpolationValue;
		tQuat.y = quats[currentKnotIndex].y +
		    (-quats[currentKnotIndex+1].y -
		     quats[currentKnotIndex].y)*currentInterpolationValue;
		tQuat.z = quats[currentKnotIndex].z +
		    (-quats[currentKnotIndex+1].z -
		     quats[currentKnotIndex].z)*currentInterpolationValue;
		tQuat.w = quats[currentKnotIndex].w +
		    (-quats[currentKnotIndex+1].w -
		     quats[currentKnotIndex].w)*currentInterpolationValue;
	    } else {
		tQuat.x = quats[currentKnotIndex].x +
		    (quats[currentKnotIndex+1].x -
		     quats[currentKnotIndex].x)*currentInterpolationValue;
		tQuat.y = quats[currentKnotIndex].y +
		    (quats[currentKnotIndex+1].y -
		     quats[currentKnotIndex].y)*currentInterpolationValue;
		tQuat.z = quats[currentKnotIndex].z +
		    (quats[currentKnotIndex+1].z -
		     quats[currentKnotIndex].z)*currentInterpolationValue;
		tQuat.w = quats[currentKnotIndex].w +
		    (quats[currentKnotIndex+1].w -
		     quats[currentKnotIndex].w)*currentInterpolationValue;
	    }
	    pos.x = positions[currentKnotIndex].x +
		(positions[currentKnotIndex+1].x -
		 positions[currentKnotIndex].x) * currentInterpolationValue;
	    pos.y = positions[currentKnotIndex].y +
		(positions[currentKnotIndex+1].y -
		 positions[currentKnotIndex].y) * currentInterpolationValue;
	    pos.z = positions[currentKnotIndex].z +
		(positions[currentKnotIndex+1].z -
		 positions[currentKnotIndex].z) * currentInterpolationValue;
	    scale = scales[currentKnotIndex] +
		(scales[currentKnotIndex+1] -
		 scales[currentKnotIndex]) * currentInterpolationValue;
	}
	tQuat.normalize();

	sMat.set(scale);
	tMat.set(tQuat);
	tMat.mul(sMat);
	// Set the translation components.

	tMat.m03 = pos.x;
	tMat.m13 = pos.y;
	tMat.m23 = pos.z;
	rotation.set(tMat);

	// construct a Transform3D from:  axis * rotation * axisInverse
	transform.mul(axis, rotation);
	transform.mul(transform, axisInverse);
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
        RotPosScalePathInterpolator ri = new RotPosScalePathInterpolator();
        ri.duplicateNode(this, forceDuplicate);
        return ri;
    }


   /**
     * Copies all RotPosScalePathInterpolator information from
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

	RotPosScalePathInterpolator ri =
	    (RotPosScalePathInterpolator) originalNode;

        int len = ri.getArrayLengths();

	// No API available to change size of array, so set here explicitly
        positions = new Point3f[len];
        quats = new Quat4f[len];
        scales = new float[len];

        Point3f point = new Point3f();
        Quat4f quat = new Quat4f();

        for (int i = 0; i < len; i++) {
            positions[i] = new Point3f();
            ri.getPosition(i, point);
            setPosition(i, point);

            quats[i] = new Quat4f();
            ri.getQuat(i, quat);
            setQuat(i, quat);

            setScale(i, ri.getScale(i));
        }
    }


}
