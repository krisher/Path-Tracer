/**
 * 
 */
package edu.rit.krisher.scene.geometry.acceleration;

import java.io.IOException;
import java.util.zip.ZipInputStream;

import org.junit.Assert;
import org.junit.Test;

import edu.rit.krisher.fileparser.ply.PLYParser;
import edu.rit.krisher.scene.AxisAlignedBoundingBox;
import edu.rit.krisher.scene.geometry.TriangleMesh;
import edu.rit.krisher.scene.geometry.buffer.IndexBuffer;
import edu.rit.krisher.scene.geometry.buffer.Vec3Buffer;
import edu.rit.krisher.scene.geometry.buffer.Vec3fBuffer;
import edu.rit.krisher.util.Timer;
import edu.rit.krisher.vecmath.Vec3;

/**
 *
 */
public class KDTreeTest {
   private static final String bunnyResource = "/edu/rit/krisher/fileparser/ply/bun_zipper.ply.zip";

   @Test
   public void splitLocationsShouldBeInNodeRange() throws Exception {
      final Partitionable geometry = createOpenBoxGeometry();
      final KDTree tree = new KDTree(20, 2, new Partitionable[] { geometry });
      final KDNodeVisitor visitor = new KDNodeVisitor() {

         @Override
         public void visitNode(final int depth, final AxisAlignedBoundingBox bounds, final boolean leaf,
               final int childCount, final float splitLocation, final int splitAxis) throws Exception {
            if (!leaf) {
               Assert.assertTrue("Split location " + splitLocation + " out of bounds: " + bounds + " for split axis "
                                 + splitAxis + ".", splitLocation < bounds.maxXYZ[splitAxis]
                                                                                  && splitLocation > bounds.minXYZ[splitAxis]);
            }
         }
      };
      tree.visitTreeNodes(visitor);
   }

   @Test
   public void treeMetricsShouldNotBeWorse() {
      final TriangleMesh bunnyGeom = loadBunny();
      final Timer timer = new Timer("KD Construction Time (Bunny).");
      timer.start();
      final KDTree tree = new KDTree(20, 2, bunnyGeom);
      timer.stop();
      timer.print(System.out);
      final KDTreeMetrics metrics = new KDTreeMetrics(tree);
      System.out.println(metrics);
      // Sanity Check
      Assert.assertEquals("Unexpected primitive count...", 69451, metrics.totalPrimitives
                          - metrics.duplicatedPrimitives);

      Assert.assertTrue("Average Depth...", 18.5 < metrics.avgDepth);
      Assert.assertTrue("Max Primitives Per Leaf...", 824 >= metrics.maxLeafPrimitives);
      Assert.assertTrue("Avg Primitives Per Leaf...", 4.7 > metrics.avgLeafPrimitives);
      Assert.assertTrue("Balance...", 0.96 < metrics.balanceFactor);
   }

   private static TriangleMesh loadBunny() {
      ZipInputStream stream = null;
      try {
         stream = new ZipInputStream(KDTreeTest.class.getResourceAsStream(bunnyResource));
         stream.getNextEntry();
         return PLYParser.parseTriangleMesh(stream);
      } catch (final IOException ioe) {
         ioe.printStackTrace();
      } finally {
         try {
            if (stream != null)
               stream.close();
         } catch (final IOException e) {
            e.printStackTrace();
         }
      }
      return null;
   }

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
