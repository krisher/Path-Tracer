package edu.rit.krisher.scene.geometry.acceleration;

import edu.rit.krisher.scene.AxisAlignedBoundingBox;
import edu.rit.krisher.scene.geometry.TriangleMesh;
import edu.rit.krisher.scene.geometry.buffer.IndexBuffer;
import edu.rit.krisher.scene.geometry.buffer.Vec3Buffer;
import edu.rit.krisher.scene.geometry.buffer.Vec3fBuffer;

public class KDSplitMeshGenerator {

   public static TriangleMesh generateLeafNodeMesh(final KDTree tree) {
      final KDTreeMetrics metrics = new KDTreeMetrics(tree);
      final Vec3Buffer vertices = new Vec3fBuffer(metrics.leafNodes * 8);
      final IndexBuffer ib = new IndexBuffer(metrics.leafNodes * 36);

      try {
         tree.visitTreeNodes(new KDNodeVisitor() {
            int vIdx = 0;

            @Override
            public void visitNode(final int depth, final AxisAlignedBoundingBox aabb, final boolean leaf,
                  final int childCount, final double splitLocation, final int splitAxis) throws Exception {
               if (leaf) {
                  vertices.put(aabb.maxXYZ[0], aabb.minXYZ[1], aabb.minXYZ[2]);
                  vertices.put(aabb.minXYZ[0], aabb.minXYZ[1], aabb.minXYZ[2]);
                  vertices.put(aabb.minXYZ[0], aabb.minXYZ[1], aabb.maxXYZ[2]);
                  vertices.put(aabb.maxXYZ[0], aabb.minXYZ[1], aabb.maxXYZ[2]);

                  vertices.put(aabb.maxXYZ[0], aabb.maxXYZ[1], aabb.minXYZ[2]);
                  vertices.put(aabb.minXYZ[0], aabb.maxXYZ[1], aabb.minXYZ[2]);
                  vertices.put(aabb.minXYZ[0], aabb.maxXYZ[1], aabb.maxXYZ[2]);
                  vertices.put(aabb.maxXYZ[0], aabb.maxXYZ[1], aabb.maxXYZ[2]);

                  ib.put(vIdx + 0).put(vIdx + 1).put(vIdx + 2);
                  ib.put(vIdx + 0).put(vIdx + 2).put(vIdx + 3);

                  ib.put(vIdx + 5).put(vIdx + 4).put(vIdx + 1);
                  ib.put(vIdx + 4).put(vIdx + 0).put(vIdx + 1);

                  ib.put(vIdx + 5).put(vIdx + 2).put(vIdx + 6);
                  ib.put(vIdx + 5).put(vIdx + 1).put(vIdx + 2);

                  ib.put(vIdx + 3).put(vIdx + 7).put(vIdx + 6);
                  ib.put(vIdx + 3).put(vIdx + 6).put(vIdx + 2);

                  ib.put(vIdx + 0).put(vIdx + 4).put(vIdx + 7);
                  ib.put(vIdx + 0).put(vIdx + 7).put(vIdx + 3);

                  ib.put(vIdx + 4).put(vIdx + 5).put(vIdx + 6);
                  ib.put(vIdx + 4).put(vIdx + 6).put(vIdx + 7);

                  vIdx += 8;
               }
            }
         });
      } catch (final Exception e) {
         e.printStackTrace();
      }
      return new TriangleMesh(vertices, ib.getIndices());
   }

}