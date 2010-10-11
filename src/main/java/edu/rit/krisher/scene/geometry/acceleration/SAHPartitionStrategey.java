package edu.rit.krisher.scene.geometry.acceleration;

import java.util.Arrays;
import java.util.Comparator;

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
      this(maxDepth, 1.0, 100.0, 0.80);
   }

   public SAHPartitionStrategey(final int maxDepth, final double nodeTraversalCost,
         final double geometryIntersectionCost, final double emptyBias) {
      this.maxDepth = maxDepth;
      this.kdNodeTraversalCost = nodeTraversalCost;
      this.geometryIntersectionCost = geometryIntersectionCost;
      this.emptyBias = Math.max(0.0, Math.min(1.0, emptyBias));
   }

   @Override
   public PartitionResult findSplitLocation(final int memberCount, final AxisAlignedBoundingBox[] bounds,
         final AxisAlignedBoundingBox nodeBounds, final int depth) {
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
      final AxisAlignedBoundingBox[] maxEdges = Arrays.copyOf(bounds, memberCount);
      for (int axisAttempt = 0; axisAttempt < 3; axisAttempt++) {
         /*
          * Sort the split candidates by split location so we can easily count how many members fall on each side of the
          * split plane.
          */
         Arrays.sort(bounds, 0, memberCount, new AABBMinComparator(splitAxis));
         Arrays.sort(maxEdges, 0, memberCount, new AABBMaxComparator(splitAxis));

         /*
          * Compute the SA-based cost of spliting at each candidate, recording the best location.
          */
         int lessPrims = 0;
         int greaterPrims = memberCount;

         saTemp.set(nodeBounds); // Initialize bounding box to node bounds, this is used to calculate surface area.

         for (int minIdx = 0, maxIdx = 0; minIdx < memberCount || maxIdx < memberCount;) {
            double splitLocation;
            boolean newPrim = false;

            if (minIdx >= memberCount) {
               splitLocation = maxEdges[maxIdx].maxXYZ[splitAxis];
               --greaterPrims;
               ++maxIdx;
            } else if (maxIdx >= memberCount || bounds[minIdx].minXYZ[splitAxis] < maxEdges[maxIdx].maxXYZ[splitAxis]) {
               /*
                * A minimum
                */
               splitLocation = bounds[minIdx].minXYZ[splitAxis];
               ++minIdx;
               newPrim = true;
            } else {
               /*
                * We encountered a maximum
                */
               splitLocation = maxEdges[maxIdx].maxXYZ[splitAxis];
               --greaterPrims;
               ++maxIdx;
            }
            /*
             * Ensure that the split candidate falls inside the node's bounds.
             */
            if (splitLocation > nodeBounds.minXYZ[splitAxis] && splitLocation < nodeBounds.maxXYZ[splitAxis]) {

               /*
                * Compute the expected cost of traversing the children if we split at this candidate.
                */
               saTemp.minXYZ[splitAxis] = nodeBounds.minXYZ[splitAxis];
               saTemp.maxXYZ[splitAxis] = splitLocation;
               final double lessNodeSurfaceAreaRatio = saTemp.surfaceArea() / nodeSurfaceArea;
               saTemp.minXYZ[splitAxis] = splitLocation;
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
                  bestSplit = splitLocation;
                  bestSplitAxis = splitAxis;
               }
            }
            if (newPrim)
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

   private static final class AABBMinComparator implements Comparator<AxisAlignedBoundingBox> {
      private final int axis;

      public AABBMinComparator(final int axis) {
         this.axis = axis;
      }

      @Override
      public int compare(final AxisAlignedBoundingBox o1, final AxisAlignedBoundingBox o2) {
         return o1.minXYZ[axis] < o2.minXYZ[axis] ? -1 : (o1.minXYZ[axis] > o2.minXYZ[axis] ? 1 : 0);
      }

   };

   private static final class AABBMaxComparator implements Comparator<AxisAlignedBoundingBox> {
      private final int axis;

      public AABBMaxComparator(final int axis) {
         this.axis = axis;
      }

      @Override
      public int compare(final AxisAlignedBoundingBox o1, final AxisAlignedBoundingBox o2) {
         return o1.maxXYZ[axis] < o2.maxXYZ[axis] ? -1 : (o1.maxXYZ[axis] > o2.maxXYZ[axis] ? 1 : 0);
      }

   };

}
