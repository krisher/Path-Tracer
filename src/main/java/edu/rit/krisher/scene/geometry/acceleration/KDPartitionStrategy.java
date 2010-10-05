/**
 * 
 */
package edu.rit.krisher.scene.geometry.acceleration;

import edu.rit.krisher.scene.AxisAlignedBoundingBox;

/**
 * Determines how the contents of a KD Tree node should be split into two children...
 */
public interface KDPartitionStrategy {

   public PartitionResult findSplitLocation(final int[] members, int memberCount, final AxisAlignedBoundingBox[] bounds,
         final AxisAlignedBoundingBox nodeBounds, final int depthRemaining);
}
