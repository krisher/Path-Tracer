package edu.rit.krisher.scene.geometry.acceleration;

import java.util.Arrays;

import edu.rit.krisher.raytracer.rays.HitData;
import edu.rit.krisher.scene.AxisAlignedBoundingBox;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.GeometryIntersection;
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
   /*
    * Array indexed by KDTree primitive index (x2) for pair containing Geometry index (into content array), and
    * primitive index for the corresponding geometry.
    */
   private final int[] primToGeomMap;
   private final AxisAlignedBoundingBox treeBounds;
   private final KDPartitionStrategy partitionStrategy;
   private final Geometry[] content;

   /**
    * Creates a KDTree with the specified geometry content. This uses a default partitioning strategy.
    * 
    * @param content
    *           The geometry to store in the KD-Tree. At least one must be provided.
    */
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
   public void getHitData(final HitData data, final Ray ray, final double isectDist, final int prim) {
      KDTree.this.content[primToGeomMap[prim * 2]].getHitData(data, ray, isectDist, primToGeomMap[prim * 2 + 1]);
   }

   @Override
   public double intersects(final GeometryIntersection intersection, final Ray ray, final int primIndices) {
      if (root != null) {
         final double[] params = new double[2];
         if (treeBounds.rayIntersectsParametric(ray, params))
            return root.intersects(intersection, ray, params[0], params[1], ray.origin.get(), ray.direction.get());
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

      final KDInteriorNode node = new KDInteriorNode(partition.splitLocation, partition.splitAxis);
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
         node.greaterChild = partition(members, greaterCount, bounds, depth + 1, nodeBounds);
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
            if (bounds[members[startIdx]].minXYZ[splitAxis] < split
                  || bounds[members[startIdx]].maxXYZ[splitAxis] <= split) {
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
            if (bounds[members[startIdx]].maxXYZ[splitAxis] > split) {
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

      public double intersects(final GeometryIntersection intersection, final Ray ray, final double tmin,
            final double tmax, final double[] rayOriginD, final double[] rayDirectionD);

      public void visit(int depth, AxisAlignedBoundingBox nodeBounds, KDNodeVisitor vistor) throws Exception;
   }

   private static class KDInteriorNode implements KDNode {
      private KDNode lessChild;
      private KDNode greaterChild;
      private final double splitLocation;
      private final byte axis;

      KDInteriorNode(final double splitLocation, final int splitAxis) {
         this.splitLocation = splitLocation;
         this.axis = (byte) splitAxis;
      }

      @Override
      public final double intersects(final GeometryIntersection intersection, final Ray ray, final double tmin,
            final double tmax, final double[] rayOriginD, final double[] rayDirectionD) {
         assert tmin <= tmax : "Bad intersection parameterization.";

         final double cEntry = rayOriginD[axis] + tmin * rayDirectionD[axis];
         final double cExit = rayOriginD[axis] + tmax * rayDirectionD[axis];

         if (cEntry <= splitLocation) { /* node entry point on less side of split */
            if (cExit < splitLocation) { /* exit point on less side of split, only need to check lessChild */
               return (lessChild == null) ? 0
                     : lessChild.intersects(intersection, ray, tmin, tmax, rayOriginD, rayDirectionD);
            } else { /* Traverses from less child to greater child */
               final double tsplit = (splitLocation - rayOriginD[axis]) / rayDirectionD[axis];
               /* first hit child; use tmin, tsplit */
               final double hitDist = (lessChild == null) ? 0
                     : lessChild.intersects(intersection, ray, tmin, tsplit, rayOriginD, rayDirectionD);
               if (greaterChild == null)
                  return hitDist;
               if (hitDist <= 0) { /* No hit from lessChild */
                  return greaterChild.intersects(intersection, ray, tsplit, tmax, rayOriginD, rayDirectionD);
               }
               assert hitDist >= tmin - Ray.SMALL_D : "An earlier node should have found this intersection";
               if (hitDist <= tsplit) /* Won't get a closer intersection in the other child node. */
                  return hitDist;
               /* Hit occurred after tSplit. Compare to result from greater child. */
               /* The hit primitive from the lessChild node will be overwritten */
               final int primIdx = intersection.primitiveIndex;
               final double newHit = greaterChild.intersects(intersection, ray, tsplit, tmax, rayOriginD, rayDirectionD);
               if (newHit > 0 && newHit < hitDist) {
                  assert newHit >= tsplit - Ray.SMALL_D;
                  return newHit; /* Greater child had closer hit. */
               }
               /* Restore greaterChild's primitive index as the hit result. */
               intersection.primitiveIndex = primIdx;
               return hitDist;
            }
         } else { /* Entry on greater side. */
            if (cExit > splitLocation) { // exit on greater/eq side of split, only check greater.
               return (greaterChild == null) ? 0
                     : greaterChild.intersects(intersection, ray, tmin, tmax, rayOriginD, rayDirectionD);
            } else { // exit on less side, check both
               final double tsplit = (splitLocation - rayOriginD[axis]) / rayDirectionD[axis];
               // greater-child: use tmin, tsplit
               final double hitDist = (greaterChild == null) ? 0
                     : greaterChild.intersects(intersection, ray, tmin, tsplit, rayOriginD, rayDirectionD);
               if (lessChild == null)
                  return hitDist;
               if (hitDist <= 0) { /* No hit from lessChild */
                  return lessChild.intersects(intersection, ray, tsplit, tmax, rayOriginD, rayDirectionD);
               }
               assert hitDist >= tmin - Ray.SMALL_D : "An earlier node should have found this intersection";
               if (hitDist <= tsplit) /* Won't get a closer intersection in the other child node. */
                  return hitDist;
               final int primIdx = intersection.primitiveIndex;
               final double newHit = lessChild.intersects(intersection, ray, tsplit, tmax, rayOriginD, rayDirectionD);
               if (newHit > 0 && newHit < hitDist)
                  return newHit; /* Greater child had closer hit. */
               /* Restore greaterChild's primitive index as the hit result. */
               intersection.primitiveIndex = primIdx;
               return hitDist;
            }
         }
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

         if (greaterChild != null) {
            final double minBound = nodeBounds.minXYZ[axis];
            nodeBounds.minXYZ[axis] = splitLocation;
            greaterChild.visit(depth + 1, nodeBounds, visitor);
            nodeBounds.minXYZ[axis] = minBound;
         }
         visitor.visitNode(depth, nodeBounds, false, ((lessChild == null) ? 0 : 1) + ((greaterChild == null) ? 0 : 1), splitLocation, axis);
      }
   }

   private class KDLeafNode implements KDNode {
      private final int[] primitives;

      public KDLeafNode(final int[] primitives) {
         this.primitives = primitives;
      }

      @Override
      public double intersects(final GeometryIntersection intersection, final Ray ray, final double tmin,
            final double tmax, final double[] rayOriginD, final double[] rayDirectionD) {
         double minDist = 0;
         int primIdx = -1;
         for (final int prim : primitives) {
            final double dist = KDTree.this.content[primToGeomMap[prim * 2]].intersects(intersection, ray, primToGeomMap[prim * 2 + 1]);
            if (dist > 0 && (dist < minDist || minDist <= 0)) {
               primIdx = prim;
               minDist = dist;
            }
         }
         intersection.primitiveIndex = primIdx;
         return minDist;
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
