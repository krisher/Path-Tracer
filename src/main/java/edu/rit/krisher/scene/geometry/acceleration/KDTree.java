package edu.rit.krisher.scene.geometry.acceleration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.rit.krisher.raytracer.rays.HitData;
import edu.rit.krisher.scene.AxisAlignedBoundingBox;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

public class KDTree implements Geometry {

   public static final byte X_AXIS = 0;
   public static final byte Y_AXIS = 1;
   public static final byte Z_AXIS = 2;

   private final KDNode root;
   private final Geometry[] primitives;
   private final int minPrims;
   private final AxisAlignedBoundingBox treeBounds;

   public KDTree(final Partitionable[] content, final int maxDepth, final int minPrims) {
      this.minPrims = minPrims;
      if (content == null || content.length == 0) {
         root = null;
         primitives = null;
         treeBounds = new AxisAlignedBoundingBox();
         return;
      }

      List<Geometry> primitivesList = new ArrayList<Geometry>();
      treeBounds = content[0].getBounds().clone();
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

   @Override
   public void getHitData(final HitData data, final Ray ray, final double isectDist) {
      root.getHitData(data, ray, isectDist);
   }

   @Override
   public double intersects(final Ray ray) {
      if (root != null) {
         final double[] params = new double[2];
         if (ray.intersectsBoxParametric(params, treeBounds.minXYZ, treeBounds.maxXYZ))
            return root.intersects(ray, params[0], params[1]);
      }
      return 0;
   }

   @Override
   public AxisAlignedBoundingBox getBounds() {
      return treeBounds.clone();
   }

   public void visitTreeNodes(final KDNodeVisitor visitor) throws Exception {
      if (root != null) {
         root.visit(0, treeBounds, visitor);
      }
   }

   private KDNode partition(final int[] members, final AxisAlignedBoundingBox[] bounds, final int depthRemaining,
         byte splitAxis, final AxisAlignedBoundingBox nodeBounds) {

      if (members.length < minPrims || depthRemaining == 0) {
         return new KDLeafNodeImpl(members);
      }

      final float split = findSplitLocationMedianMin(members, bounds, splitAxis, nodeBounds);
      if (Float.isInfinite(split)) {
         return new KDLeafNodeImpl(members);
      }

      final int[][] lgPrims;
      lgPrims = partition(members, bounds, splitAxis, split);

      final KDInteriorNodeImpl node = new KDInteriorNodeImpl(split, splitAxis);
      splitAxis = (byte) ((splitAxis + 1) % 3);
      if (lgPrims[0].length > 0)
         node.lessChild = partition(lgPrims[0], bounds, depthRemaining - 1, splitAxis, boundsForChild(nodeBounds, split, splitAxis, true));
      if (lgPrims[1].length > 0)
         node.greaterEqChild = partition(lgPrims[1], bounds, depthRemaining - 1, splitAxis, boundsForChild(nodeBounds, split, splitAxis, false));
      return node;
   }

   private int[][] partition(final int[] members, final AxisAlignedBoundingBox[] bounds, final byte splitAxis,
         final float split) {
      final int[][] lgPrims;
      final int[] lessPrims = new int[members.length];
      final int[] greaterPrims = new int[members.length];
      int lidx = 0, gidx = 0;
      if (splitAxis == 0) {
         for (final int prim : members) {
            if (bounds[prim].minXYZ.x < split)
               lessPrims[lidx++] = prim;
            if (bounds[prim].maxXYZ.x >= split)
               greaterPrims[gidx++] = prim;
         }
      } else if (splitAxis == 1) {
         for (final int prim : members) {
            if (bounds[prim].minXYZ.y < split)
               lessPrims[lidx++] = prim;
            if (bounds[prim].maxXYZ.y >= split)
               greaterPrims[gidx++] = prim;
         }
      } else if (splitAxis == 2) {
         for (final int prim : members) {
            if (bounds[prim].minXYZ.z < split)
               lessPrims[lidx++] = prim;
            if (bounds[prim].maxXYZ.z >= split)
               greaterPrims[gidx++] = prim;
         }
      }
      lgPrims = new int[][] { Arrays.copyOf(lessPrims, lidx), Arrays.copyOf(greaterPrims, gidx) };
      return lgPrims;
   }

   private float findSplitLocationMedianMin(final int[] members, final AxisAlignedBoundingBox[] bounds,
         final byte splitAxis, final AxisAlignedBoundingBox nodeBounds) {
      // TODO: need to ensure the split actually occurs within the bounds of the kd-node!
      final float[] splitCandidates = new float[members.length];
      int idx = 0;
      if (splitAxis == 0) {
         for (final int prim : members) {
            splitCandidates[idx++] = (float) bounds[prim].minXYZ.x;
         }
      } else if (splitAxis == 1) {
         for (final int prim : members) {
            splitCandidates[idx++] = (float) bounds[prim].minXYZ.y;
         }
      } else if (splitAxis == 2) {
         for (final int prim : members) {
            splitCandidates[idx++] = (float) bounds[prim].minXYZ.z;
         }
      }
      Arrays.sort(splitCandidates);
      final float split = splitCandidates[splitCandidates.length / 2];
      if (splitAxis == 0) {
         if (split >= nodeBounds.maxXYZ.x || split <= nodeBounds.minXYZ.x)
            return Float.POSITIVE_INFINITY;
      } else if (splitAxis == 1) {
         if (split >= nodeBounds.maxXYZ.y || split <= nodeBounds.minXYZ.y)
            return Float.POSITIVE_INFINITY;
      } else {
         if (split >= nodeBounds.maxXYZ.z || split <= nodeBounds.minXYZ.z)
            return Float.POSITIVE_INFINITY;
      }
      return split;
   }

   private static AxisAlignedBoundingBox boundsForChild(final AxisAlignedBoundingBox nodeBounds,
         final double splitLocation, final byte axis, final boolean lessChild) {
      final AxisAlignedBoundingBox childBounds = new AxisAlignedBoundingBox();
      childBounds.minXYZ.set(nodeBounds.minXYZ);
      childBounds.maxXYZ.set(nodeBounds.maxXYZ);
      final Vec3 minMaxLimit = (lessChild) ? childBounds.maxXYZ : childBounds.minXYZ;
      if (axis == X_AXIS) {
         minMaxLimit.x = splitLocation;
      } else if (axis == Y_AXIS) {
         minMaxLimit.y = splitLocation;
      } else {
         minMaxLimit.z = splitLocation;
      }
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

   protected static interface KDNode {

      public double intersects(final Ray ray, final double tmin, final double tmax);

      public void getHitData(final HitData data, final Ray ray, final double isectDist);

      public void visit(int depth, AxisAlignedBoundingBox nodeBounds, KDNodeVisitor vistor) throws Exception;
   }

   protected class KDInteriorNodeImpl implements KDNode {
      KDNode lessChild;
      KDNode greaterEqChild;
      final float splitLocation;
      final byte axis;

      KDInteriorNodeImpl(final float splitLocation, final byte splitAxis) {
         this.splitLocation = splitLocation;
         this.axis = splitAxis;
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

   protected class KDLeafNodeImpl implements KDNode {
      final int[] primitives;

      public KDLeafNodeImpl(final int[] primitives) {
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
