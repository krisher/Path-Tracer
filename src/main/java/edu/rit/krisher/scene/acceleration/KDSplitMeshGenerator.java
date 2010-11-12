package edu.rit.krisher.scene.acceleration;

import java.nio.FloatBuffer;

import edu.rit.krisher.scene.geometry.TriangleMesh;
import edu.rit.krisher.vecmath.AxisAlignedBoundingBox;

public class KDSplitMeshGenerator {

   private static final int[] boxVertIndices = { 0, 1, 2, 0, 2, 3, 5, 4, 1, 4, 0, 1, 5, 2, 6, 5, 1, 2, 3, 7, 6, 3, 6, 2, 0, 4,
      7, 0, 7, 3, 4, 5, 6, 4, 6, 7 };

   public static TriangleMesh generateLeafNodeMesh(final KDGeometryContainer tree) {
      final KDTreeMetrics metrics = new KDTreeMetrics(tree);
      final FloatBuffer vertices = FloatBuffer.wrap(new float[metrics.leafNodes * 8 * 3]);
      final int[] ib = new int[metrics.leafNodes * 36];

      try {
         tree.visitTreeNodes(new KDNodeVisitor() {
            int vIdx = 0;
            int iIdx = 0;

            @Override
            public void visitNode(final int depth, final AxisAlignedBoundingBox aabb, final boolean leaf,
                  final int childCount, final double splitLocation, final int splitAxis) throws Exception {
               if (leaf) {
                  vertices.put((float)aabb.xyzxyz[3]).put( (float)aabb.xyzxyz[1]).put( (float)aabb.xyzxyz[2]);
                  vertices.put((float)aabb.xyzxyz[0]).put((float) aabb.xyzxyz[1]).put( (float)aabb.xyzxyz[2]);
                  vertices.put((float)aabb.xyzxyz[0]).put((float) aabb.xyzxyz[1]).put( (float)aabb.xyzxyz[5]);
                  vertices.put((float)aabb.xyzxyz[3]).put( (float)aabb.xyzxyz[1]).put((float) aabb.xyzxyz[5]);

                  vertices.put((float)aabb.xyzxyz[3]).put((float) aabb.xyzxyz[4]).put((float) aabb.xyzxyz[2]);
                  vertices.put((float)aabb.xyzxyz[0]).put((float) aabb.xyzxyz[4]).put((float) aabb.xyzxyz[2]);
                  vertices.put((float)aabb.xyzxyz[0]).put( (float)aabb.xyzxyz[4]).put( (float)aabb.xyzxyz[5]);
                  vertices.put((float)aabb.xyzxyz[3]).put( (float)aabb.xyzxyz[4]).put((float) aabb.xyzxyz[5]);

                  for (int i = 0; i < 36; ++i) {
                     ib[iIdx + i] = vIdx + boxVertIndices[i];
                  }

                  vIdx += 8;
                  iIdx += 36;
               }
            }
         });
      } catch (final Exception e) {
         e.printStackTrace();
      }
      return new TriangleMesh(vertices.array(), ib);
   }

}
