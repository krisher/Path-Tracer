/**
 * 
 */
package edu.rit.krisher.scene.geometry.acceleration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

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
      final Timer timer = new Timer("KD Construction Time (Bunny)");
      timer.start();
      final KDTree tree = new KDTree(20, 2, bunnyGeom);
      timer.stop();
      timer.print(System.out);
      final KDTreeMetrics metrics = new KDTreeMetrics(tree);
      System.out.println(metrics);
      // Sanity Check
      assertThat("Primitive Count", metrics.totalPrimitives - metrics.duplicatedPrimitives, equalTo(69451));

      assertThat("Average leaf depth", metrics.avgDepth, greaterThan(18.5f));
      assertThat("Max leaf Primitives", metrics.maxLeafPrimitives, lessThanOrEqualTo(824));
      assertThat("Avg Primitives Per Leaf...", metrics.avgLeafPrimitives, lessThan(4.7f));
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
