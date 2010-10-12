/**
 * 
 */
package edu.rit.krisher.scene.geometry.acceleration;

import java.util.Arrays;

import edu.rit.krisher.vecmath.AxisAlignedBoundingBox;

/**
 *
 */
public final class MedianPartitionStrategy implements KDPartitionStrategy {

   private final int maxDepth;
   private final int maxPrimitives;

   /**
    * Creates a new median partitioning strategy.
    * 
    * @param maxDepth
    *           The maximum depth of the tree (hard limit).
    * @param maxPrimitives
    *           The maximum number of primitives in a leaf node (soft limit).
    */
   public MedianPartitionStrategy(final int maxDepth, final int maxPrimitives) {
      this.maxDepth = maxDepth;
      this.maxPrimitives = maxPrimitives;
   }

   /*
    * @see edu.rit.krisher.scene.geometry.acceleration.KDPartitionStrategy#findSplitLocation(int[],
    * edu.rit.krisher.scene.AxisAlignedBoundingBox[], byte, edu.rit.krisher.scene.AxisAlignedBoundingBox, int, int)
    */
   @Override
   public PartitionResult findSplitLocation(final int memberCount, final AxisAlignedBoundingBox[] bounds,
         final AxisAlignedBoundingBox nodeBounds, final int depth) {

      if (depth >= maxDepth || memberCount < maxPrimitives)
         return PartitionResult.LEAF;
      /*
       * From SAH; choose largest dimension as initial split axis.
       */
      int splitAxis = (nodeBounds.xSpan() > nodeBounds.ySpan()) ? (nodeBounds.xSpan() > nodeBounds.zSpan() ? KDTree.X_AXIS
            : KDTree.Z_AXIS)
            : (nodeBounds.ySpan() > nodeBounds.zSpan() ? KDTree.Y_AXIS : KDTree.Z_AXIS);
      for (int i = 0; i < 3; ++i) {
         final float split = findSplitLocation(memberCount, bounds, splitAxis);
         // TODO: need to ensure the split actually occurs within the bounds of the kd-node!,
         // But this generates a really bad tree.
         // Maybe try a different axis?
         if (split >= nodeBounds.xyzxyz[splitAxis + 3] || split <= nodeBounds.xyzxyz[splitAxis]) {
            splitAxis = (splitAxis + 1) % 3;
            continue;
         }
         return new PartitionResult(splitAxis, split);
      }
      return PartitionResult.LEAF;
   }

   /**
    * @param members
    * @param bounds
    * @param splitAxis
    * @return
    */
   private final float findSplitLocation(final int memberCount, final AxisAlignedBoundingBox[] bounds,
         final int splitAxis) {
      final float[] splitCandidates = new float[memberCount];
      int idx = 0;
      for (int i=0; i < memberCount; ++i) {
         splitCandidates[idx++] = (float) bounds[i].centerArray()[splitAxis];
      }
      Arrays.sort(splitCandidates);
      final float split = splitCandidates[splitCandidates.length / 2];
      return split;
   }


}
