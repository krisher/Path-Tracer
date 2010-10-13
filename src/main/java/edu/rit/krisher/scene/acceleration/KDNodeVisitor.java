/**
 * 
 */
package edu.rit.krisher.scene.acceleration;

import edu.rit.krisher.vecmath.AxisAlignedBoundingBox;

/**
 * KDTree node visitor interface. This is useful for analyzing the tree structure for quality and verification.
 */
public interface KDNodeVisitor {
   public void visitNode(final int depth, final AxisAlignedBoundingBox bounds, final boolean leaf, final int childCount, final double splitLocation, final int splitAxis) throws Exception;
}