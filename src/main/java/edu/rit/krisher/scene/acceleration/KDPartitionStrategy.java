/**
 * 
 */
package edu.rit.krisher.scene.acceleration;

import edu.rit.krisher.vecmath.AxisAlignedBoundingBox;

/**
 * Determines how the contents of a KD Tree node should be split into two children...
 */
public interface KDPartitionStrategy {

   /**
    * Given the parameters of geometric primitives intersecting with a volume, determines whether a KD Tree node should
    * be split, and if so, where and along which axis.
    * 
    * @param memberCount
    *           Any values in members or bounds beyond the count specified by memberCount are not under consideration
    *           and should not be modified in any way.
    * @param bounds
    *           An array of bounding boxes for each primitive. This array is indexed by the first memberCount values in
    *           members. The contents should not be modified in any way.
    * @param nodeBounds
    * @param depthRemaining
    * @return
    */
   public PartitionResult findSplitLocation(int memberCount,
         final AxisAlignedBoundingBox[] bounds, final AxisAlignedBoundingBox nodeBounds, final int depthRemaining);
}
