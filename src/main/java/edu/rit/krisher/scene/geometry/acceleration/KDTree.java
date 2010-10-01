package edu.rit.krisher.scene.geometry.acceleration;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.rit.krisher.raytracer.rays.HitData;
import edu.rit.krisher.scene.AxisAlignedBoundingBox;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.vecmath.Ray;

public class KDTree implements Geometry {

   public static final byte X_AXIS = 0;
   public static final byte Y_AXIS = 1;
   public static final byte Z_AXIS = 2;

   private final KDNode root;
   private final Geometry[] primitives;
   private final int minPrims;
   private final AxisAlignedBoundingBox treeBounds;
   private KDPartitionStrategy partitionStrategy = new MedianPartitionStrategy();

   public KDTree(final Partitionable[] content, final int maxDepth, final int minPrims) {
      this(maxDepth, minPrims, content);
   }

   public KDTree(final int maxDepth, final int minPrims, final Partitionable... content) {
      this.minPrims = minPrims;
      if (content == null || content.length == 0) {
         root = null;
         primitives = null;
         treeBounds = new AxisAlignedBoundingBox();
         return;
      }

      List<Geometry> primitivesList = new ArrayList<Geometry>();
      treeBounds = new AxisAlignedBoundingBox(content[0].getBounds());
      for (int i = 0; i < content.length; ++i) {
         final Geometry[] prims = content[i].getPrimitives();
         for (final Geometry prim : prims) {
            primitivesList.add(prim);
         }
         treeBounds.union(content[i].getBounds());
      }

      primitives = primitivesList.toArray(new Geometry[primitivesList.size()]);
      primitivesList = null;
      final AxisAlignedBoundingBox bounds[] = new AxisAlignedBoundingBox[primitives.length];
      final int[] members = new int[primitives.length];
      for (int i = 0; i < primitives.length; ++i) {
         bounds[i] = primitives[i].getBounds();
         members[i] = i;
      }

      root = partition(members, bounds, maxDepth, (byte) 0, treeBounds);
   }

   /**
    * @return the partitionStrategy
    */
   public KDPartitionStrategy getPartitionStrategy() {
      return partitionStrategy;
   }

   /**
    * @param partitionStrategy
    *           the partitionStrategy to set
    */
   public void setPartitionStrategy(final KDPartitionStrategy partitionStrategy) {
      if (partitionStrategy == null)
         this.partitionStrategy = new MedianPartitionStrategy();
      else
         this.partitionStrategy = partitionStrategy;
   }

   @Override
   public void getHitData(final HitData data, final Ray ray, final double isectDist) {
      root.getHitData(data, ray, isectDist);
   }

   @Override
   public double intersects(final Ray ray) {
      if (root != null) {
         final double[] params = new double[2];
         if (treeBounds.rayIntersectsParametric(ray, params))
            return root.intersects(ray, params[0], params[1]);
      }
      return 0;
   }

   @Override
   public AxisAlignedBoundingBox getBounds() {
      return new AxisAlignedBoundingBox(treeBounds);
   }

   public void visitTreeNodes(final KDNodeVisitor visitor) throws Exception {
      if (root != null) {
         root.visit(0, treeBounds, visitor);
      }
   }

   private KDNode partition(final int[] members, final AxisAlignedBoundingBox[] bounds, final int depthRemaining,
         final int splitAxis, final AxisAlignedBoundingBox nodeBounds) {

      if (members.length < minPrims || depthRemaining == 0) {
         return new KDLeafNode(members);
      }

      final PartitionResult partition = partitionStrategy.findSplitLocation(members, bounds, splitAxis, nodeBounds, depthRemaining, minPrims);
      if (partition.splitAxis == PartitionResult.NO_SPLIT) {
         return new KDLeafNode(members);
      }

      final int[][] lgPrims;
      lgPrims = partition(members, bounds, partition.splitAxis, partition.splitLocation);

      final KDInteriorNode node = new KDInteriorNode((float)partition.splitLocation, partition.splitAxis);
      if (lgPrims[0].length > 0)
         node.lessChild = partition(lgPrims[0], bounds, depthRemaining - 1, splitAxis, boundsForChild(nodeBounds, partition.splitLocation, partition.splitAxis, true));
      if (lgPrims[1].length > 0)
         node.greaterEqChild = partition(lgPrims[1], bounds, depthRemaining - 1, splitAxis, boundsForChild(nodeBounds, partition.splitLocation, partition.splitAxis, false));
      return node;
   }

   private int[][] partition(final int[] members, final AxisAlignedBoundingBox[] bounds, final int splitAxis,
         final double split) {
      final int[][] lgPrims;
      final int[] lessPrims = new int[members.length];
      final int[] greaterPrims = new int[members.length];
      int lidx = 0, gidx = 0;
      for (final int prim : members) {
         if (bounds[prim].minXYZ[splitAxis] < split)
            lessPrims[lidx++] = prim;
         if (bounds[prim].maxXYZ[splitAxis] >= split)
            greaterPrims[gidx++] = prim;
      }
      lgPrims = new int[][] { Arrays.copyOf(lessPrims, lidx), Arrays.copyOf(greaterPrims, gidx) };
      return lgPrims;
   }

   private static AxisAlignedBoundingBox boundsForChild(final AxisAlignedBoundingBox nodeBounds,
         final double splitLocation, final int axis, final boolean lessChild) {
      final AxisAlignedBoundingBox childBounds = new AxisAlignedBoundingBox(nodeBounds);
      final double[] minMaxLimit = (lessChild) ? childBounds.maxXYZ : childBounds.minXYZ;
      minMaxLimit[axis] = splitLocation;
      return childBounds;
   }

   public int getMaxDepth() {
      final int[] maxDepth = new int[1];
      try {
         visitTreeNodes(new KDNodeVisitor() {
            @Override
            public void visitNode(final int depth, final AxisAlignedBoundingBox bounds, final boolean leaf,
                  final int childCount, final float splitLocation, final int splitAxis) throws Exception {
               if (leaf && depth > maxDepth[0])
                  maxDepth[0] = depth;
            }
         });
      } catch (final Exception e) {
         // Unreachable...
      }
      return maxDepth[0];
   }

   public float getAvgDepth() {
      final int[] avgDepth = new int[2];
      try {
         visitTreeNodes(new KDNodeVisitor() {
            @Override
            public void visitNode(final int depth, final AxisAlignedBoundingBox bounds, final boolean leaf,
                  final int childCount, final float splitLocation, final int splitAxis) throws Exception {
               if (leaf) {
                  avgDepth[0] += depth;
                  ++avgDepth[1];
               }
            }
         });
      } catch (final Exception e) {
         // Unreachable...
      }
      return avgDepth[0] / (float) avgDepth[1];
   }

   public double getMinDepth() {
      final int[] minDepth = new int[] { Integer.MAX_VALUE };
      try {
         visitTreeNodes(new KDNodeVisitor() {
            @Override
            public void visitNode(final int depth, final AxisAlignedBoundingBox bounds, final boolean leaf,
                  final int childCount, final float splitLocation, final int splitAxis) throws Exception {
               if (leaf && depth < minDepth[0])
                  minDepth[0] = depth;
            }
         });
      } catch (final Exception e) {
         // Unreachable...
      }
      return minDepth[0];
   }

   public float getAvgPrimitiveCount() {
      final int[] avgCount = new int[2];
      try {
         visitTreeNodes(new KDNodeVisitor() {
            @Override
            public void visitNode(final int depth, final AxisAlignedBoundingBox bounds, final boolean leaf,
                  final int childCount, final float splitLocation, final int splitAxis) throws Exception {
               if (leaf) {
                  avgCount[0] += childCount;
                  ++avgCount[1];
               }
            }
         });
      } catch (final Exception e) {
         // Unreachable...
      }
      return avgCount[0] / (float) avgCount[1];
   }
   
   public void printMetrics(final PrintStream out) {
      out.println("Min Leaf Depth: " + getMinDepth());
      out.println("Max Leaf Depth: " + getMaxDepth());
      out.println("Avg Leaf Depth: " + getAvgDepth());
      out.println("Avg Primitives/Leaf: " + getAvgPrimitiveCount());
   }

   private static interface KDNode {

      public double intersects(final Ray ray, final double tmin, final double tmax);

      public void getHitData(final HitData data, final Ray ray, final double isectDist);

      public void visit(int depth, AxisAlignedBoundingBox nodeBounds, KDNodeVisitor vistor) throws Exception;
   }

   private static class KDInteriorNode implements KDNode {
      private KDNode lessChild;
      private KDNode greaterEqChild;
      private final float splitLocation;
      private final byte axis;

      KDInteriorNode(final float splitLocation, final int splitAxis) {
         this.splitLocation = splitLocation;
         this.axis = (byte)splitAxis;
      }

      @Override
      public double intersects(final Ray ray, final double tmin, final double tmax) {
         final double tCMin, tCMax;
         if (axis == X_AXIS) {
            tCMin = ray.origin.x + tmin * ray.direction.x;
            tCMax = ray.origin.x + tmax * ray.direction.x;
         } else if (axis == Y_AXIS) {
            tCMin = ray.origin.y + tmin * ray.direction.y;
            tCMax = ray.origin.y + tmax * ray.direction.y;
         } else {
            tCMin = ray.origin.z + tmin * ray.direction.z;
            tCMax = ray.origin.z + tmax * ray.direction.z;
         }

         if (tCMin <= splitLocation) {
            if (tCMax < splitLocation) {
               final double hitDist = (lessChild == null) ? 0 : lessChild.intersects(ray, tmin, tmax);
               if (hitDist > 0)
                  return hitDist;
            } else if (tCMax == splitLocation) {
               final double hitDist = (greaterEqChild == null) ? 0 : greaterEqChild.intersects(ray, tmin, tmax);
               if (hitDist > 0)
                  return hitDist;
            } else {
               final double tsplit;
               if (axis == X_AXIS)
                  tsplit = (splitLocation - ray.origin.x) / ray.direction.x;
               else if (axis == Y_AXIS)
                  tsplit = (splitLocation - ray.origin.y) / ray.direction.y;
               else
                  tsplit = (splitLocation - ray.origin.z) / ray.direction.z;
               // less-child: use tmin, tsplit
               double hitDist = (lessChild == null) ? 0 : lessChild.intersects(ray, tmin, tsplit);
               if (hitDist > 0)
                  return hitDist;
               // greater-child: use tsplit, tmax
               hitDist = (greaterEqChild == null) ? 0 : greaterEqChild.intersects(ray, tsplit, tmax);
               if (hitDist > 0)
                  return hitDist;
            }
         } else {
            if (tCMax > splitLocation) {
               final double hitDist = (greaterEqChild == null) ? 0 : greaterEqChild.intersects(ray, tmin, tmax);
               if (hitDist > 0)
                  return hitDist;
            } else {
               final double tsplit;
               if (axis == X_AXIS)
                  tsplit = (splitLocation - ray.origin.x) / ray.direction.x;
               else if (axis == Y_AXIS)
                  tsplit = (splitLocation - ray.origin.y) / ray.direction.y;
               else
                  tsplit = (splitLocation - ray.origin.z) / ray.direction.z;
               // greater-child: use tmin, tsplit
               double hitDist = (greaterEqChild == null) ? 0 : greaterEqChild.intersects(ray, tmin, tsplit);
               if (hitDist > 0)
                  return hitDist;
               // less-child: use tsplit, tmax
               hitDist = (lessChild == null) ? 0 : lessChild.intersects(ray, tsplit, tmax);
               if (hitDist > 0)
                  return hitDist;
            }
         }
         return 0;
      }

      @Override
      public void getHitData(final HitData data, final Ray ray, final double isectDist) {
         final double isectCoord;
         if (axis == X_AXIS) {
            isectCoord = ray.origin.x + ray.direction.x * isectDist;
         } else if (axis == Y_AXIS) {
            isectCoord = ray.origin.y + ray.direction.y * isectDist;
         } else {
            isectCoord = ray.origin.z + ray.direction.z * isectDist;
         }
         if (isectCoord < splitLocation)
            lessChild.getHitData(data, ray, isectDist);
         else
            greaterEqChild.getHitData(data, ray, isectDist);
      }

      @Override
      public void visit(final int depth, final AxisAlignedBoundingBox nodeBounds, final KDNodeVisitor visitor)
            throws Exception {
         if (lessChild != null)
            lessChild.visit(depth + 1, boundsForChild(nodeBounds, splitLocation, axis, true), visitor);

         if (greaterEqChild != null)
            greaterEqChild.visit(depth + 1, boundsForChild(nodeBounds, splitLocation, axis, false), visitor);

         visitor.visitNode(depth, nodeBounds, false, ((lessChild == null) ? 0 : 1) + ((greaterEqChild == null) ? 0 : 1), splitLocation, axis);
      }

   }

   private class KDLeafNode implements KDNode {
      private final int[] primitives;

      public KDLeafNode(final int[] primitives) {
         this.primitives = primitives;
      }

      @Override
      public double intersects(final Ray ray, final double tmin, final double tmax) {
         double minDist = 0;
         for (final int prim : primitives) {
            final double dist = KDTree.this.primitives[prim].intersects(ray);
            if (dist > 0 && (dist < minDist || minDist <= 0))
               minDist = dist;
         }
         return minDist;
      }

      @Override
      public void getHitData(final HitData data, final Ray ray, final double isectDist) {
         for (final int prim : primitives) {
            final double dist = KDTree.this.primitives[prim].intersects(ray);
            if (dist == isectDist) {
               KDTree.this.primitives[prim].getHitData(data, ray, isectDist);
               return;
            }
         }
      }

      /*
       * @see edu.rit.krisher.scene.geometry.acceleration.KDTree.KDNode#visit(int,
       * edu.rit.krisher.scene.AxisAlignedBoundingBox, edu.rit.krisher.scene.geometry.acceleration.KDNodeVisitor)
       */
      @Override
      public void visit(final int depth, final AxisAlignedBoundingBox nodeBounds, final KDNodeVisitor visitor)
            throws Exception {
         visitor.visitNode(depth, nodeBounds, true, primitives.length, 0, -1);
      }

   }
}
