package edu.rit.krisher.scene.acceleration;

import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.GeometryIntersection;
import edu.rit.krisher.scene.IntersectionInfo;
import edu.rit.krisher.vecmath.AxisAlignedBoundingBox;
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

   private final AxisAlignedBoundingBox treeBounds;
   private final KDPartitionStrategy partitionStrategy;
   private final Geometry[] content;

   private final int geomBits;
   private final int geomMask;

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
         throw new IllegalArgumentException("Must specify content for a KD Tree.");
      }

      treeBounds = new AxisAlignedBoundingBox();
      int primCount = 0;
      for (int i = 0; i < content.length; ++i) {
         primCount += content[i].getPrimitiveCount();
      }
      final PrimitiveAABB[] bounds = new PrimitiveAABB[primCount];

      geomBits = (32 - Integer.numberOfLeadingZeros(content.length));
      int gMask = 0;
      for (int i = 0; i < geomBits; i++)
         gMask |= 1 << i;
      geomMask = gMask;


      int globalPrimIdx = 0;
      for (int geomIdx = 0; geomIdx < content.length; ++geomIdx) {
         final Geometry geom = content[geomIdx];
         treeBounds.union(geom.getBounds(-1));
         for (int primIdx = geom.getPrimitiveCount() - 1; primIdx >= 0; --primIdx) {
            bounds[globalPrimIdx] = new PrimitiveAABB(geomIdx | (primIdx << geomBits), geom.getBounds(primIdx));
            ++globalPrimIdx;
         }
      }

      root = partition(bounds.length, bounds, 0, treeBounds);
   }

   @Override
   public double getSurfaceArea(final int primIndices) {
      return getBounds(-1).surfaceArea();
   }

   @Override
   public int getPrimitiveCount() {
      int count = 0;
      for (final Geometry geom : content) {
         count += geom.getPrimitiveCount();
      }
      return count;
   }


   /**
    * Accessor for the KDTree partition strategy used to subdivide the tree nodes.
    * 
    * @return the partitionStrategy (as specified via a constructor).
    */
   public KDPartitionStrategy getPartitionStrategy() {
      return partitionStrategy;
   }

   @Override
   public void getHitData(final IntersectionInfo data, final int primitiveID, final Ray ray, final double distance) {
      KDTree.this.content[primitiveID & geomMask].getHitData(data, primitiveID >> geomBits, ray, distance);
   }

   @Override
   public final boolean intersects(final Ray ray, final GeometryIntersection intersection) {
      if (root != null) {
         final double[] params = new double[2];
         if (treeBounds.rayIntersectsParametric(ray, params)) {
            root.intersects(intersection, ray, params[0], params[1], ray.origin.get(), ray.direction.get());
            if (intersection.hitGeometry == this) return true;
         }
      }
      return false;
   }


   @Override
   public final double intersectsPrimitive(final Ray ray,
         final double maxDistance, final int primitiveID) {
      // As long is getPrimitiveCount() returns 1, the primitiveID is meaningless...
      final GeometryIntersection isect = new GeometryIntersection();
      isect.t = maxDistance;
      if (intersects(ray, isect)) {
         return isect.t;
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

   private final KDNode partition(final int memberCount, final PrimitiveAABB[] bounds,
         final int depth, final AxisAlignedBoundingBox nodeBounds) {
      if (memberCount == 0) {
         return new KDLeafNode(new int[0]);
      }

      final PartitionResult partition = partitionStrategy.findSplitLocation(memberCount, bounds, nodeBounds, depth);
      if (partition == PartitionResult.LEAF) {
         return new KDLeafNode(createPrimIdxArray(bounds, memberCount));
      }

      final KDInteriorNode node = new KDInteriorNode(partition.splitLocation, partition.splitAxis);
      final int lessCount = partitionPrimitives(memberCount, bounds, partition.splitAxis, partition.splitLocation, true);
      if (lessCount > 0) {
         final double maxBound = nodeBounds.xyzxyz[partition.splitAxis + 3];
         nodeBounds.xyzxyz[partition.splitAxis + 3] = partition.splitLocation;
         node.lessChild = partition(lessCount, bounds, depth + 1, nodeBounds);
         nodeBounds.xyzxyz[partition.splitAxis + 3] = maxBound;
      }
      final int greaterCount = partitionPrimitives(memberCount, bounds, partition.splitAxis, partition.splitLocation, false);
      if (greaterCount > 0) {
         final double minBound = nodeBounds.xyzxyz[partition.splitAxis];
         nodeBounds.xyzxyz[partition.splitAxis] = partition.splitLocation;
         node.greaterChild = partition(greaterCount, bounds, depth + 1, nodeBounds);
         nodeBounds.xyzxyz[partition.splitAxis] = minBound;
      }
      return node;
   }

   private static final int[] createPrimIdxArray(final PrimitiveAABB[] bounds, final int count) {
      final int[] primIdx = new int[count];
      for (int i = 0; i < count; ++i) {
         primIdx[i] = bounds[i].primID;
      }
      return primIdx;
   }

   private static final int partitionPrimitives(final int memberCount,
         final AxisAlignedBoundingBox[] bounds, final int splitAxis, final double split, final boolean less) {
      int startIdx = 0;
      int endIdx = memberCount - 1;
      if (less) {
         while (startIdx <= endIdx) {
            if (bounds[startIdx].xyzxyz[splitAxis] < split || bounds[startIdx].xyzxyz[splitAxis + 3] <= split) {
               ++startIdx;
            } else {
               final AxisAlignedBoundingBox tmp = bounds[endIdx];
               bounds[endIdx] = bounds[startIdx];
               bounds[startIdx] = tmp;
               --endIdx;
            }
         }
      } else {
         while (startIdx <= endIdx) {
            if (bounds[startIdx].xyzxyz[splitAxis + 3] > split) {
               ++startIdx;
            } else {
               final AxisAlignedBoundingBox tmp = bounds[endIdx];
               bounds[endIdx] = bounds[startIdx];
               bounds[startIdx] = tmp;
               --endIdx;
            }
         }
      }
      return startIdx;
   }

   private static interface KDNode {

      public void intersects(final GeometryIntersection intersection, final Ray ray, final double tmin,
            final double tmax, final double[] rayOriginD, final double[] rayDirectionD);

      public void visit(int depth, AxisAlignedBoundingBox nodeBounds, KDNodeVisitor vistor) throws Exception;
   }

   private static final class KDInteriorNode implements KDNode {
      private KDNode lessChild;
      private KDNode greaterChild;
      private final double splitLocation;
      private final int axis;

      KDInteriorNode(final double splitLocation, final int splitAxis) {
         this.splitLocation = splitLocation;
         this.axis = splitAxis;
      }

      @Override
      public final void intersects(final GeometryIntersection intersection, final Ray ray, final double tmin,
            final double tmax, final double[] rayOriginD, final double[] rayDirectionD) {
         assert tmin <= tmax : "Bad intersection parameterization.";
         if (tmin > intersection.t)
            return;
         final double cEntry = rayOriginD[axis] + tmin * rayDirectionD[axis];
         final double cExit = rayOriginD[axis] + tmax * rayDirectionD[axis];

         if (cEntry <= splitLocation) { /* node entry point on less side of split */
            if (cExit < splitLocation) { /* exit point on less side of split, only need to check lessChild */
               if (lessChild != null) lessChild.intersects(intersection, ray, tmin, tmax, rayOriginD, rayDirectionD);
            } else { /* Traverses from less child to greater child */
               final double tsplit = (splitLocation - rayOriginD[axis]) / rayDirectionD[axis];
               /* first hit child; use tmin, tsplit */
               if (lessChild != null) lessChild.intersects(intersection, ray, tmin, tsplit, rayOriginD, rayDirectionD);
               if (greaterChild != null && intersection.t >= tsplit)
                  greaterChild.intersects(intersection, ray, tsplit, tmax, rayOriginD, rayDirectionD);               
            }
         } else { /* Entry on greater side. */
            if (cExit > splitLocation) { // exit on greater/eq side of split, only check greater.
               if (greaterChild != null) greaterChild.intersects(intersection, ray, tmin, tmax, rayOriginD, rayDirectionD);
            } else { // exit on less side, check both
               final double tsplit = (splitLocation - rayOriginD[axis]) / rayDirectionD[axis];
               // greater-child: use tmin, tsplit
               if (greaterChild != null) greaterChild.intersects(intersection, ray, tmin, tsplit, rayOriginD, rayDirectionD);
               if (lessChild != null && intersection.t >= tsplit)
                  lessChild.intersects(intersection, ray, tsplit, tmax, rayOriginD, rayDirectionD);
            }
         }
      }

      @Override
      public void visit(final int depth, final AxisAlignedBoundingBox nodeBounds, final KDNodeVisitor visitor)
      throws Exception {
         if (lessChild != null) {
            final double maxBound = nodeBounds.xyzxyz[axis + 3];
            nodeBounds.xyzxyz[axis + 3] = splitLocation;
            lessChild.visit(depth + 1, nodeBounds, visitor);
            nodeBounds.xyzxyz[axis + 3] = maxBound;
         }

         if (greaterChild != null) {
            final double minBound = nodeBounds.xyzxyz[axis];
            nodeBounds.xyzxyz[axis] = splitLocation;
            greaterChild.visit(depth + 1, nodeBounds, visitor);
            nodeBounds.xyzxyz[axis] = minBound;
         }
         visitor.visitNode(depth, nodeBounds, false, ((lessChild == null) ? 0 : 1) + ((greaterChild == null) ? 0 : 1), splitLocation, axis);
      }
   }

   private final class KDLeafNode implements KDNode {
      private final int[] primitives;

      public KDLeafNode(final int[] primitives) {
         this.primitives = primitives;
      }

      @Override
      public void intersects(final GeometryIntersection intersection, final Ray ray, final double tmin,
            final double tmax, final double[] rayOriginD, final double[] rayDirectionD) {
         final int primIdx = -1;
         for (final int prim : primitives) {
            final double dist = KDTree.this.content[prim & geomMask].intersectsPrimitive(ray, intersection.t, prim >> geomBits);
            if (dist > 0 && (dist < intersection.t)) {
               intersection.t = dist;
               intersection.hitGeometry = KDTree.this;
               intersection.primitiveID = prim;
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

   private static final class PrimitiveAABB extends AxisAlignedBoundingBox {
      final int primID;

      public PrimitiveAABB(final int primID, final AxisAlignedBoundingBox aabb) {
         super(aabb);
         this.primID = primID;
      }
   }
}
