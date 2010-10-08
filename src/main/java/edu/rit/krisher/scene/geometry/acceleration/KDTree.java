package edu.rit.krisher.scene.geometry.acceleration;

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
   private final int[] primToGeomMap;
   private final AxisAlignedBoundingBox treeBounds;
   private final KDPartitionStrategy partitionStrategy;
   private final Geometry[] content;

   public KDTree(final Geometry... content) {
      this(new SAHPartitionStrategey(), content);
   }

   public KDTree(final KDPartitionStrategy strategy, final Geometry... content) {
      this.partitionStrategy = strategy;
      this.content = content;
      if (content == null || content.length == 0) {
         root = null;
         primToGeomMap = null;
         treeBounds = new AxisAlignedBoundingBox();
         return;
      }

      treeBounds = new AxisAlignedBoundingBox();
      int primCount = 0;
      for (int i = 0; i < content.length; ++i) {
         primCount += content[i].getPrimitiveCount();
      }
      primToGeomMap = new int[primCount * 2];
      final AxisAlignedBoundingBox bounds[] = new AxisAlignedBoundingBox[primCount];
      final int[] members = new int[primCount];

      int globalPrimIdx = 0;

      for (int geomIdx = 0; geomIdx < content.length; ++geomIdx) {
         final Geometry geom = content[geomIdx];
         treeBounds.union(geom.getBounds(-1));
         for (int primIdx = 0; primIdx < geom.getPrimitiveCount(); ++primIdx) {
            bounds[globalPrimIdx] = geom.getBounds(primIdx);
            primToGeomMap[globalPrimIdx * 2] = geomIdx;
            primToGeomMap[globalPrimIdx * 2 + 1] = primIdx;
            members[globalPrimIdx] = globalPrimIdx;
            ++globalPrimIdx;
         }
      }

      root = partition(members, members.length, bounds, 0, treeBounds);
   }

   @Override
   public double getSurfaceArea(final int primIndices) {
      return getBounds(-1).surfaceArea();
   }

   @Override
   public int getPrimitiveCount() {
      return 1;
   }

   public int getTriCount() {
      return primToGeomMap.length / 2;
   }

   /**
    * @return the partitionStrategy
    */
   public KDPartitionStrategy getPartitionStrategy() {
      return partitionStrategy;
   }

   @Override
   public void getHitData(final HitData data, final Ray ray, final double isectDist, final int primIndices) {
      final double[] hitCoord = new double[] { ray.origin.x + ray.direction.x * isectDist,
            ray.origin.y + ray.direction.y * isectDist,

            ray.origin.z + ray.direction.z * isectDist };
      root.getHitData(data, ray, isectDist, hitCoord);
   }

   @Override
   public double intersects(final Ray ray, final int primIndices) {
      if (root != null) {
         final double[] params = new double[2];
         if (treeBounds.rayIntersectsParametric(ray, params))
            return root.intersects(ray, params[0], params[1], ray.origin.get(), ray.direction.get());
      }
      return 0;
   }

   @Override
   public AxisAlignedBoundingBox getBounds(final int primIndices) {
      return new AxisAlignedBoundingBox(treeBounds);
   }

   public void visitTreeNodes(final KDNodeVisitor visitor) throws Exception {
      if (root != null) {
         root.visit(0, treeBounds, visitor);
      }
   }

   private final KDNode partition(final int[] members, final int memberCount, final AxisAlignedBoundingBox[] bounds,
         final int depth, final AxisAlignedBoundingBox nodeBounds) {
      if (memberCount == 0) {
         return new KDLeafNode(new int[0]);
      }

      final PartitionResult partition = partitionStrategy.findSplitLocation(members, memberCount, bounds, nodeBounds, depth);
      if (partition == PartitionResult.LEAF) {
         return new KDLeafNode(Arrays.copyOf(members, memberCount));
      }

      final KDInteriorNode node = new KDInteriorNode((float) partition.splitLocation, partition.splitAxis);
      final int lessCount = partitionPrimitives(members, memberCount, bounds, partition.splitAxis, partition.splitLocation, true);
      if (lessCount > 0) {
         final double maxBound = nodeBounds.maxXYZ[partition.splitAxis];
         nodeBounds.maxXYZ[partition.splitAxis] = partition.splitLocation;
         node.lessChild = partition(members, lessCount, bounds, depth + 1, nodeBounds);
         nodeBounds.maxXYZ[partition.splitAxis] = maxBound;
      }
      final int greaterCount = partitionPrimitives(members, memberCount, bounds, partition.splitAxis, partition.splitLocation, false);
      if (greaterCount > 0) {
         final double minBound = nodeBounds.minXYZ[partition.splitAxis];
         nodeBounds.minXYZ[partition.splitAxis] = partition.splitLocation;
         node.greaterEqChild = partition(members, greaterCount, bounds, depth + 1, nodeBounds);
         nodeBounds.minXYZ[partition.splitAxis] = minBound;
      }
      return node;
   }

   private static final int partitionPrimitives(final int[] members, final int memberCount,
         final AxisAlignedBoundingBox[] bounds, final int splitAxis, final double split, final boolean less) {
      int startIdx = 0;
      int endIdx = memberCount - 1;
      if (less) {
         while (startIdx <= endIdx) {
            if (bounds[members[startIdx]].minXYZ[splitAxis] < split) {
               ++startIdx;
            } else {
               final int tmp = members[endIdx];
               members[endIdx] = members[startIdx];
               members[startIdx] = tmp;
               --endIdx;
            }
         }
      } else {
         while (startIdx <= endIdx) {
            if (bounds[members[startIdx]].maxXYZ[splitAxis] >= split
                  || bounds[members[startIdx]].maxXYZ[splitAxis] == split
                  && bounds[members[startIdx]].minXYZ[splitAxis] == split) {
               ++startIdx;
            } else {
               final int tmp = members[endIdx];
               members[endIdx] = members[startIdx];
               members[startIdx] = tmp;
               --endIdx;
            }
         }
      }
      return startIdx;
   }

   private static interface KDNode {

      public double intersects(final Ray ray, final double tmin, final double tmax, final double[] rayOriginD,
            final double[] rayDirectionD);

      public void getHitData(final HitData data, final Ray ray, final double isectDist, final double[] hitCoord);

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
      public final double intersects(final Ray ray, final double tmin, final double tmax, final double[] rayOriginD,
            final double[] rayDirectionD) {
         final double cEntry = rayOriginD[axis] + tmin * rayDirectionD[axis];
         final double cExit = rayOriginD[axis] + tmax * rayDirectionD[axis];

         if (cEntry < splitLocation) { // entry point on less side of split
            if (cExit < splitLocation) { // exit point on less side of split, only need to check less...
               return (lessChild == null) ? 0 : lessChild.intersects(ray, tmin, tmax, rayOriginD, rayDirectionD);
            } else { // exit point >= split location, need to check both
               final double tsplit = (splitLocation - rayOriginD[axis]) / rayDirectionD[axis];
               // less-child: use tmin, tsplit
               final double hitDist = (lessChild == null) ? 0
                     : lessChild.intersects(ray, tmin, tsplit, rayOriginD, rayDirectionD);
               if ((hitDist > 0 && hitDist < tsplit) || greaterEqChild == null)
                  return hitDist;
               // greater-child: use tsplit, tmax
               return greaterEqChild.intersects(ray, tsplit, tmax, rayOriginD, rayDirectionD);
            }
         } else { // entry >= split coordinate
            if (cExit >= splitLocation) { // exit on greater/eq side of split, only check greater.
               return (greaterEqChild == null) ? 0
                     : greaterEqChild.intersects(ray, tmin, tmax, rayOriginD, rayDirectionD);
            } else { // exit on less side, check both
               final double tsplit = (splitLocation - rayOriginD[axis]) / rayDirectionD[axis];
               // greater-child: use tmin, tsplit
               final double hitDist = (greaterEqChild == null) ? 0
                     : greaterEqChild.intersects(ray, tmin, tsplit, rayOriginD, rayDirectionD);
               if ((hitDist > 0 && hitDist <= tsplit) || lessChild == null)
                  return hitDist;
               // less-child: use tsplit, tmax
               return lessChild.intersects(ray, tsplit, tmax, rayOriginD, rayDirectionD);
            }
         }
      }

      @Override
      public void getHitData(final HitData data, final Ray ray, final double isectDist, final double[] hitCoord) {
         if (hitCoord[axis] < splitLocation && lessChild != null /*
          * Can attempt to traverse the wrong child in the cases
          * where the hit coord is very close to the
          * splitLocation (round-off error...)
          */) {
            lessChild.getHitData(data, ray, isectDist, hitCoord);
         } else
            greaterEqChild.getHitData(data, ray, isectDist, hitCoord);
      }

      @Override
      public void visit(final int depth, final AxisAlignedBoundingBox nodeBounds, final KDNodeVisitor visitor)
      throws Exception {
         if (lessChild != null) {
            final double maxBound = nodeBounds.maxXYZ[axis];
            nodeBounds.maxXYZ[axis] = splitLocation;
            lessChild.visit(depth + 1, nodeBounds, visitor);
            nodeBounds.maxXYZ[axis] = maxBound;
         }

         if (greaterEqChild != null) {
            final double minBound = nodeBounds.minXYZ[axis];
            nodeBounds.minXYZ[axis] = splitLocation;
            greaterEqChild.visit(depth + 1, nodeBounds, visitor);
            nodeBounds.minXYZ[axis] = minBound;
         }
         visitor.visitNode(depth, nodeBounds, false, ((lessChild == null) ? 0 : 1) + ((greaterEqChild == null) ? 0 : 1), splitLocation, axis);
      }
   }

   private class KDLeafNode implements KDNode {
      private final int[] primitives;

      public KDLeafNode(final int[] primitives) {
         this.primitives = primitives;
      }

      @Override
      public double intersects(final Ray ray, final double tmin, final double tmax, final double[] rayOriginD,
            final double[] rayDirectionD) {
         double minDist = 0;
         for (final int prim : primitives) {
            final double dist = KDTree.this.content[primToGeomMap[prim * 2]].intersects(ray, primToGeomMap[prim * 2 + 1]);
            if (dist > 0 && (dist < minDist || minDist <= 0))
               minDist = dist;
         }
         return minDist;
      }

      @Override
      public void getHitData(final HitData data, final Ray ray, final double isectDist, final double[] hitCoord) {
         for (final int prim : primitives) {
            final double dist = KDTree.this.content[primToGeomMap[prim * 2]].intersects(ray, primToGeomMap[prim * 2 + 1]);
            if (dist == isectDist) {
               KDTree.this.content[primToGeomMap[prim * 2]].getHitData(data, ray, isectDist, primToGeomMap[prim * 2 + 1]);
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
