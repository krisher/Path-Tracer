package edu.rit.krisher.scene.geometry.acceleration;

import java.util.ArrayList;
import java.util.Arrays;

import edu.rit.krisher.raytracer.rays.HitData;
import edu.rit.krisher.scene.AxisAlignedBoundingBox;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.vecmath.Ray;

/**
 * KD-Tree spatial partitioning structure for storing {@link Partitionable} geometry.
 * 
 * @author krisher
 * 
 */
public class KDTree implements Geometry {

   public static final byte X_AXIS = 0;
   public static final byte Y_AXIS = 1;
   public static final byte Z_AXIS = 2;

   private final KDNode root;
   private final Geometry[] primitives;
   private final AxisAlignedBoundingBox treeBounds;
   private final KDPartitionStrategy partitionStrategy;

   public KDTree(final Geometry... content) {
      this(new SAHPartitionStrategey(), content);
   }

   public KDTree(final KDPartitionStrategy strategy, final Geometry... content) {
      this.partitionStrategy = strategy;
      if (content == null || content.length == 0) {
         root = null;
         primitives = null;
         treeBounds = new AxisAlignedBoundingBox();
         return;
      }

      ArrayList<Geometry> primitivesList = new ArrayList<Geometry>();
      treeBounds = new AxisAlignedBoundingBox(content[0].getBounds());
      for (int i = 0; i < content.length; ++i) {
         final Geometry[] prims = content[i].getPrimitives();
         primitivesList.ensureCapacity(primitivesList.size() + prims.length);
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

      root = partition(members, bounds, 0, treeBounds);
   }

   @Override
   public double getSurfaceArea() {
      return getBounds().surfaceArea();
   }

   @Override
   public Geometry[] getPrimitives() {
      return primitives;
   }

   /**
    * @return the partitionStrategy
    */
   public KDPartitionStrategy getPartitionStrategy() {
      return partitionStrategy;
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

   private final KDNode partition(final int[] members, final AxisAlignedBoundingBox[] bounds, final int depth,
         final AxisAlignedBoundingBox nodeBounds) {
      if (members.length == 0) {
         return null;
      }

      final PartitionResult partition = partitionStrategy.findSplitLocation(members, bounds, nodeBounds, depth);
      if (partition == PartitionResult.LEAF) {
         return new KDLeafNode(members);
      }

      final int[][] lgPrims;
      lgPrims = partitionPrimitives(members, bounds, partition.splitAxis, partition.splitLocation);

      final KDInteriorNode node = new KDInteriorNode((float) partition.splitLocation, partition.splitAxis);
      if (lgPrims[0].length > 0)
         node.lessChild = partition(lgPrims[0], bounds, depth + 1, boundsForChild(nodeBounds, partition.splitLocation, partition.splitAxis, true));
      if (lgPrims[1].length > 0)
         node.greaterEqChild = partition(lgPrims[1], bounds, depth + 1, boundsForChild(nodeBounds, partition.splitLocation, partition.splitAxis, false));
      return node;
   }

   private int[][] partitionPrimitives(final int[] members, final AxisAlignedBoundingBox[] bounds, final int splitAxis,
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
         this.axis = (byte) splitAxis;
      }

      @Override
      public double intersects(final Ray ray, final double tmin, final double tmax) {
         final double cEntry, cExit;
         if (axis == X_AXIS) {
            cEntry = ray.origin.x + tmin * ray.direction.x;
            cExit = ray.origin.x + tmax * ray.direction.x;
         } else if (axis == Y_AXIS) {
            cEntry = ray.origin.y + tmin * ray.direction.y;
            cExit = ray.origin.y + tmax * ray.direction.y;
         } else {
            cEntry = ray.origin.z + tmin * ray.direction.z;
            cExit = ray.origin.z + tmax * ray.direction.z;
         }

         if (cEntry < splitLocation) { // entry point on less side of split, or on split
            if (cExit < splitLocation) { // exit point on less side of split, only need to check less...
               return (lessChild == null) ? 0 : lessChild.intersects(ray, tmin, tmax);
            } else { // exit point >= split location, need to check both
               final double tsplit;
               if (axis == X_AXIS)
                  tsplit = (splitLocation - ray.origin.x) / ray.direction.x;
               else if (axis == Y_AXIS)
                  tsplit = (splitLocation - ray.origin.y) / ray.direction.y;
               else
                  tsplit = (splitLocation - ray.origin.z) / ray.direction.z;
               // less-child: use tmin, tsplit
               final double hitDist = (lessChild == null) ? 0 : lessChild.intersects(ray, tmin, tsplit);
               if ((hitDist > 0 && hitDist < tsplit) || greaterEqChild == null)
                  return hitDist;
               // greater-child: use tsplit, tmax
               return greaterEqChild.intersects(ray, tsplit, tmax);
            }
         } else { // entry >= split coordinate
            if (cExit >= splitLocation) { // exit on greater/eq side of split, only check greater.
               return (greaterEqChild == null) ? 0 : greaterEqChild.intersects(ray, tmin, tmax);
            } else { // exit on less side, check both
               final double tsplit;
               if (axis == X_AXIS)
                  tsplit = (splitLocation - ray.origin.x) / ray.direction.x;
               else if (axis == Y_AXIS)
                  tsplit = (splitLocation - ray.origin.y) / ray.direction.y;
               else
                  tsplit = (splitLocation - ray.origin.z) / ray.direction.z;
               // greater-child: use tmin, tsplit
               final double hitDist = (greaterEqChild == null) ? 0 : greaterEqChild.intersects(ray, tmin, tsplit);
               if ((hitDist > 0 && hitDist <= tsplit) || lessChild == null)
                  return hitDist;
               // less-child: use tsplit, tmax
               return lessChild.intersects(ray, tsplit, tmax);
            }
         }
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
