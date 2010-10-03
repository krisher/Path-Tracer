/**
 * 
 */
package edu.rit.krisher.scene.geometry.acceleration;

import java.util.Arrays;

import edu.rit.krisher.scene.AxisAlignedBoundingBox;

/**
 *
 */
public final class MedianPartitionStrategy implements KDPartitionStrategy {

   /*
    * @see edu.rit.krisher.scene.geometry.acceleration.KDPartitionStrategy#findSplitLocation(int[],
    * edu.rit.krisher.scene.AxisAlignedBoundingBox[], byte, edu.rit.krisher.scene.AxisAlignedBoundingBox, int, int)
    */
   @Override
   public PartitionResult findSplitLocation(final int[] members, final AxisAlignedBoundingBox[] bounds,
         final int previousSplitAxis, final AxisAlignedBoundingBox nodeBounds, final int depthRemaining,
         final int minPrimitives) {
      int splitAxis = (previousSplitAxis + 1) % 3;
      for (int i = 0; i < 3; ++i) {
         final float split = findSplitLocation(members, bounds, splitAxis);
         // TODO: need to ensure the split actually occurs within the bounds of the kd-node!,
         // But this generates a really bad tree.
         // Maybe try a different axis?
         if (split >= nodeBounds.maxXYZ[splitAxis] || split <= nodeBounds.minXYZ[splitAxis]) {
            splitAxis = (splitAxis + 1) % 3;
            continue;
         }
         return new PartitionResult(splitAxis, split);
      }
      return new PartitionResult();
   }

   /**
    * @param members
    * @param bounds
    * @param splitAxis
    * @return
    */
   private final float findSplitLocation(final int[] members, final AxisAlignedBoundingBox[] bounds, final int splitAxis) {
      final float[] splitCandidates = new float[members.length];
      int idx = 0;
      for (final int prim : members) {
         splitCandidates[idx++] = (float) bounds[prim].centerArray()[splitAxis];
      }
      Arrays.sort(splitCandidates);
      final float split = splitCandidates[splitCandidates.length / 2];
      return split;
   }

}
