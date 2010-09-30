/**
 * 
 */
package edu.rit.krisher.scene.geometry.acceleration;

import org.junit.Assert;
import org.junit.Test;

import edu.rit.krisher.scene.AxisAlignedBoundingBox;
import edu.rit.krisher.scene.geometry.TriangleMesh;
import edu.rit.krisher.scene.geometry.buffer.IndexBuffer;
import edu.rit.krisher.scene.geometry.buffer.Vec3Buffer;
import edu.rit.krisher.scene.geometry.buffer.Vec3fBuffer;
import edu.rit.krisher.vecmath.Vec3;

/**
 *
 */
public class KDTreeTest {

   @Test
   public void splitLocationsShouldBeInNodeRange() throws Exception {
      final Partitionable geometry = createOpenBoxGeometry();
      final KDTree tree = new KDTree(new Partitionable[] { geometry }, 20, 2);
      final KDNodeVisitor visitor = new KDNodeVisitor() {

         @Override
         public void visitNode(final int depth, final AxisAlignedBoundingBox bounds, final boolean leaf,
               final int childCount, final float splitLocation, final int splitAxis) throws Exception {
            if (!leaf) {
               if (splitAxis == KDTree.X_AXIS) {
                  Assert.assertTrue("Split location " + splitLocation + " out of bounds: " + bounds
                        + " for split axis X.", splitLocation < bounds.maxXYZ[0] && splitLocation > bounds.minXYZ[0]);
               } else if (splitAxis == KDTree.Y_AXIS) {
                  Assert.assertTrue("Split location " + splitLocation + " out of bounds: " + bounds
                        + " for split axis Y.", splitLocation < bounds.maxXYZ[1] && splitLocation > bounds.minXYZ[1]);
               } else {
                  Assert.assertTrue("Split location " + splitLocation + " out of bounds: " + bounds
                        + " for split axis Z.", splitLocation < bounds.maxXYZ[2] && splitLocation > bounds.minXYZ[2]);
               }
            }
         }
      };
      tree.visitTreeNodes(visitor);
   }

   // @Test
   // public void memberCountOrNodeDepthCountShouldBeReached() throws Exception {
   // /*
   // * This could be violated if there are overlapping primitives in a node...
   // */
   // final Partitionable geometry = createOpenBoxGeometry();
   // final KDTree tree = new KDTree(new Partitionable[] { geometry }, 20, 2);
   // final KDNodeVisitor visitor = new KDNodeVisitor() {
   //
   // @Override
   // public void visitLeafNode(final int depth, final AxisAlignedBoundingBox bounds, final int memberCount) {
   // Assert.assertTrue(depth == 20 || memberCount <= 2);
   // }
   //
   // @Override
   // public void visitInteriorNode(final int depth, final AxisAlignedBoundingBox bounds, final float splitLocation,
   // final byte splitAxis, final boolean lessChildren, final boolean greaterChildren) throws Exception {
   // }
   // };
   // tree.visitTreeNodes(visitor);
   // }

   private static Partitionable createOpenBoxGeometry() {
      final Vec3Buffer vb = new Vec3fBuffer(8);
      final IndexBuffer ib = new IndexBuffer(30);
      vb.put(new Vec3(5, 0, -5));
      vb.put(new Vec3(-5, 0, -5));
      vb.put(new Vec3(-5, 0, 5));
      vb.put(new Vec3(5, 0, 5));
      ib.put(0).put(1).put(2);
      ib.put(0).put(2).put(3);

      vb.put(new Vec3(5, 10, -5));
      vb.put(new Vec3(-5, 10, -5));
      vb.put(new Vec3(-5, 10, 5));
      vb.put(new Vec3(5, 10, 5));

      ib.put(4).put(5).put(1);
      ib.put(4).put(1).put(0);

      ib.put(5).put(6).put(2);
      ib.put(5).put(2).put(1);

      ib.put(6).put(7).put(3);
      ib.put(6).put(3).put(2);

      ib.put(7).put(4).put(0);
      ib.put(7).put(0).put(3);
      return new TriangleMesh(vb, ib);
   }
}
