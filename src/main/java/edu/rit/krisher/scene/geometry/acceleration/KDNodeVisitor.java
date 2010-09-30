/**
 * 
 */
package edu.rit.krisher.scene.geometry.acceleration;

import edu.rit.krisher.scene.AxisAlignedBoundingBox;

/**
 * KDTree node visitor interface. This is useful for analyzing the tree structure for quality and verification.
 */
public interface KDNodeVisitor {
   public void visitNode(final int depth, final AxisAlignedBoundingBox bounds, final boolean leaf, final int childCount, final float splitLocation, final int splitAxis) throws Exception;
}