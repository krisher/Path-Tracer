package edu.rit.krisher.scene.geometry.acceleration;

import java.util.Arrays;

import edu.rit.krisher.scene.AxisAlignedBoundingBox;

/**
 * Surface-Area-Heuristic based partitioning strategy for KDTree.
 * 
 * @author krisher
 * 
 */
public class SAHPartitionStrategey implements KDPartitionStrategy {

   private final double kdNodeTraversalCost;
   private final double geometryIntersectionCost;
   private final double emptyBias;// .25;
   private final int maxDepth;

   public SAHPartitionStrategey() {
      this(25);
   }

   public SAHPartitionStrategey(final int maxDepth) {
      this(maxDepth, 1.0, 100.0, 0.75);
   }

   public SAHPartitionStrategey(final int maxDepth, final double nodeTraversalCost,
         final double geometryIntersectionCost, final double emptyBias) {
      this.maxDepth = maxDepth;
      this.kdNodeTraversalCost = nodeTraversalCost;
      this.geometryIntersectionCost = geometryIntersectionCost;
      this.emptyBias = Math.max(0.0, Math.min(1.0, emptyBias));
   }

   @Override
   public PartitionResult findSplitLocation(final int[] members, final int memberCount,
         final AxisAlignedBoundingBox[] bounds, final AxisAlignedBoundingBox nodeBounds, final int depth) {
      if (depth >= maxDepth) {
         return PartitionResult.LEAF;
      }

      double bestSplit = 0;
      int bestSplitAxis = -1;
      double bestSACost = geometryIntersectionCost * memberCount; // Initialize to the cost of creating a leaf.
      /*
       * The surface area of the node being split...
       */
      final double nodeSurfaceArea = nodeBounds.surfaceArea();

      /*
       * Bounding box to use for calculating the surface area at a candidate split location.
       */
      final AxisAlignedBoundingBox saTemp = new AxisAlignedBoundingBox();
      /*
       * Try to split the longest dimension of the node bounds first.
       */
      int splitAxis = (nodeBounds.xSpan() > nodeBounds.ySpan()) ? (nodeBounds.xSpan() > nodeBounds.zSpan() ? KDTree.X_AXIS
            : KDTree.Z_AXIS)
            : (nodeBounds.ySpan() > nodeBounds.zSpan() ? KDTree.Y_AXIS : KDTree.Z_AXIS);
      final SplitCandidate[] splitCandidates = new SplitCandidate[memberCount * 2];
      for (int i = 0; i < splitCandidates.length; ++i)
         splitCandidates[i] = new SplitCandidate();
      for (int axisAttempt = 0; axisAttempt < 3; axisAttempt++) {
         /*
          * Use the bounding box edges along the split axis as candidate split locations.
          */
         for (int bbIdx = 0; bbIdx < memberCount; ++bbIdx) {
            final int boundsIdx = members[bbIdx];
            splitCandidates[bbIdx * 2].splitLocation = bounds[boundsIdx].minXYZ[splitAxis];
            splitCandidates[bbIdx * 2].isMax = false;
            splitCandidates[bbIdx * 2 + 1].splitLocation = bounds[boundsIdx].maxXYZ[splitAxis];
            splitCandidates[bbIdx * 2 + 1].isMax = true;
         }
         /*
          * Sort the split candidates by split location so we can easily count how many members fall on each side of the
          * split plane.
          */
         Arrays.sort(splitCandidates);

         /*
          * Compute the SA-based cost of spliting at each candidate, recording the best location.
          */
         int lessPrims = 0;
         int greaterPrims = memberCount;

         saTemp.set(nodeBounds); // Initialize bounding box to node bounds, this is used to calculate surface area.
         for (final SplitCandidate candidate : splitCandidates) {
            /*
             * If we have entered a new bounding box, increment the number that fall on the less side of the split
             * (since the current candidate is at the edge the node falls to the greater side of the split until after
             * we move to the next greater candidate).
             */

            if (candidate.isMax)
               --greaterPrims;
            /*
             * Ensure that the split candidate falls inside the node's bounds.
             */
            if (candidate.splitLocation > nodeBounds.minXYZ[splitAxis]
                                                            && candidate.splitLocation < nodeBounds.maxXYZ[splitAxis]) {

               /*
                * Compute the expected cost of traversing the children if we split at this candidate.
                */
               saTemp.minXYZ[splitAxis] = nodeBounds.minXYZ[splitAxis];
               saTemp.maxXYZ[splitAxis] = candidate.splitLocation;
               final double lessNodeSurfaceAreaRatio = saTemp.surfaceArea() / nodeSurfaceArea;
               saTemp.minXYZ[splitAxis] = candidate.splitLocation;
               saTemp.maxXYZ[splitAxis] = nodeBounds.maxXYZ[splitAxis];
               final double greaterNodeSurfaceAreaRatio = saTemp.surfaceArea() / nodeSurfaceArea;

               /*
                * The cost of splitting at this candidate is: node traversal cost + the probability of traversing to the
                * less node * the cost of traversing it + the probability of traversing the right node * the cost of
                * traversing it. The empty bias factor decreases the computed cost by a factor if the partition results
                * in an empty child node to encourage culling of empty space.
                */
               final double splitCost = kdNodeTraversalCost + geometryIntersectionCost
               * (lessNodeSurfaceAreaRatio * lessPrims + greaterNodeSurfaceAreaRatio * greaterPrims)
               * ((lessPrims == 0 || greaterPrims == 0) ? (emptyBias) : 1.0);

               if (splitCost < bestSACost) {
                  bestSACost = splitCost;
                  bestSplit = candidate.splitLocation;
                  bestSplitAxis = splitAxis;
               }
            }
            if (!candidate.isMax)
               ++lessPrims;


         }
         if (bestSplitAxis >= 0) {
            return new PartitionResult(bestSplitAxis, bestSplit);
         }
         /*
          * It was cheaper to create a leaf than to split along the current axis; retry the next axis...
          */
         splitAxis = (splitAxis + 1) % 3;
      }
      /*
       * No split attempt was cheaper than creating a leaf, so that's what we do.
       */
      return PartitionResult.LEAF;
   }

   private static final class SplitCandidate implements Comparable<SplitCandidate> {
      public double splitLocation;
      public boolean isMax;

      @Override
      public int compareTo(final SplitCandidate o) {
         if (splitLocation > o.splitLocation)
            return 1;
         if (splitLocation < o.splitLocation)
            return -1;
         if (isMax == o.isMax)
            return 0;
         if (isMax)
            return 1;
         return -1;
      }

      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         long temp;
         temp = Double.doubleToLongBits(splitLocation);
         result = prime * result + (int) (temp ^ (temp >>> 32));
         return result;
      }

      @Override
      public boolean equals(final Object obj) {
         return splitLocation == ((SplitCandidate) obj).splitLocation && isMax == ((SplitCandidate) obj).isMax;
      }

   }
}
