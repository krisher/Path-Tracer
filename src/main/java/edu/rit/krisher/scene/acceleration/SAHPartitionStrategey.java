package edu.rit.krisher.scene.acceleration;

import java.util.Arrays;
import java.util.Comparator;

import edu.rit.krisher.vecmath.AxisAlignedBoundingBox;

/**
 * Surface-Area-Heuristic based partitioning strategy for KDTree.
 * 
 * @author krisher
 * 
 */
public class SAHPartitionStrategey implements KDPartitionStrategy {

   private final double geometryIntersectionCost;
   private final double emptyBias;// .25;
   private final int maxDepth;

   public SAHPartitionStrategey() {
      this(25);
   }

   public SAHPartitionStrategey(final int maxDepth) {
      this(maxDepth, 1, 0.8);
   }

   /**
    * 
    * @param maxDepth
    *           The absolute maximum depth of the tree.
    * @param geometryIntersectionCost
    *           The cost of performing a single ray intersection test against a geometric primitive, as a factor of the
    *           cost of traversing a level in the KD Tree.
    * @param emptyBias
    */
   public SAHPartitionStrategey(final int maxDepth, final double geometryIntersectionCost, final double emptyBias) {
      this.maxDepth = maxDepth;
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
      double bestSACost = memberCount * geometryIntersectionCost; // Initialize to the cost of creating a leaf.
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
      int splitAxis = (nodeBounds.xSpan() > nodeBounds.ySpan()) ? (nodeBounds.xSpan() > nodeBounds.zSpan() ? 0
            : 2)
            : (nodeBounds.ySpan() > nodeBounds.zSpan() ? 1 : 2);
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
               splitLocation = maxEdges[maxIdx].xyzxyz[splitAxis + 3];
               --greaterPrims;
               ++maxIdx;
            } else if (maxIdx >= memberCount
                  || bounds[minIdx].xyzxyz[splitAxis] < maxEdges[maxIdx].xyzxyz[splitAxis + 3]) {
               /*
                * A minimum
                */
               splitLocation = bounds[minIdx].xyzxyz[splitAxis];
               ++minIdx;
               newPrim = true;
            } else {
               /*
                * We encountered a maximum
                */
               splitLocation = maxEdges[maxIdx].xyzxyz[splitAxis + 3];
               --greaterPrims;
               ++maxIdx;
            }
            /*
             * Ensure that the split candidate falls inside the node's bounds.
             */
            if (splitLocation > nodeBounds.xyzxyz[splitAxis] && splitLocation < nodeBounds.xyzxyz[splitAxis + 3]) {

               /*
                * Compute the expected cost of traversing the children if we split at this candidate.
                */
               saTemp.xyzxyz[splitAxis] = nodeBounds.xyzxyz[splitAxis];
               saTemp.xyzxyz[splitAxis + 3] = splitLocation;
               final double lessNodeSurfaceAreaRatio = saTemp.surfaceArea() / nodeSurfaceArea;
               saTemp.xyzxyz[splitAxis] = splitLocation;
               saTemp.xyzxyz[splitAxis + 3] = nodeBounds.xyzxyz[splitAxis + 3];
               final double greaterNodeSurfaceAreaRatio = saTemp.surfaceArea() / nodeSurfaceArea;

               /*
                * The cost of splitting at this candidate is: node traversal cost + the probability of traversing to the
                * less node * the cost of traversing it + the probability of traversing the right node * the cost of
                * traversing it. The empty bias factor decreases the computed cost by a factor if the partition results
                * in an empty child node to encourage culling of empty space.
                */
               final double splitCost = 1 + geometryIntersectionCost
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

   private static final class AABBMinComparator implements Comparator<AxisAlignedBoundingBox> {
      private final int axis;

      public AABBMinComparator(final int axis) {
         this.axis = axis;
      }

      @Override
      public int compare(final AxisAlignedBoundingBox o1, final AxisAlignedBoundingBox o2) {
         return o1.xyzxyz[axis] < o2.xyzxyz[axis] ? -1 : (o1.xyzxyz[axis] > o2.xyzxyz[axis] ? 1 : 0);
      }

   };

   private static final class AABBMaxComparator implements Comparator<AxisAlignedBoundingBox> {
      private final int axis;

      public AABBMaxComparator(final int axis) {
         this.axis = axis + 3;
      }

      @Override
      public int compare(final AxisAlignedBoundingBox o1, final AxisAlignedBoundingBox o2) {
         return o1.xyzxyz[axis] < o2.xyzxyz[axis] ? -1 : (o1.xyzxyz[axis] > o2.xyzxyz[axis] ? 1 : 0);
      }

   };

}
