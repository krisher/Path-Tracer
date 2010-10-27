/**
 * 
 */
package edu.rit.krisher.scene.geometry.acceleration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import java.io.IOException;
import java.util.zip.ZipInputStream;

import org.junit.Assert;
import org.junit.Test;

import edu.rit.krisher.fileparser.ply.PLYParser;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.acceleration.KDNodeVisitor;
import edu.rit.krisher.scene.acceleration.KDTree;
import edu.rit.krisher.scene.acceleration.KDTreeMetrics;
import edu.rit.krisher.scene.acceleration.SAHPartitionStrategey;
import edu.rit.krisher.scene.geometry.TriangleMesh;
import edu.rit.krisher.util.Timer;
import edu.rit.krisher.vecmath.AxisAlignedBoundingBox;

/**
 *
 */
public class KDTreeTest {
   private static final String bunnyResource = "/edu/rit/krisher/fileparser/ply/bun_zipper.ply.zip";

   @Test
   public void splitLocationsShouldBeInNodeRange() throws Exception {
      final Geometry geometry = createOpenBoxGeometry();
      final KDTree tree = new KDTree(geometry);
      final KDNodeVisitor visitor = new KDNodeVisitor() {

         @Override
         public void visitNode(final int depth, final AxisAlignedBoundingBox bounds, final boolean leaf,
               final int childCount, final double splitLocation, final int splitAxis) throws Exception {
            if (!leaf) {
               Assert.assertTrue("Split location " + splitLocation + " out of bounds: " + bounds + " for split axis "
                     + splitAxis + ".", splitLocation < bounds.xyzxyz[splitAxis + 3]
                     && splitLocation > bounds.xyzxyz[splitAxis]);
            }
         }
      };
      tree.visitTreeNodes(visitor);
   }

   public void treeMetricsShouldNotChange() {
      final TriangleMesh bunnyGeom = loadBunny();
      final Timer timer = new Timer("KD Construction Time (Bunny)");
      timer.start();
      final KDTree tree = new KDTree(new SAHPartitionStrategey(), bunnyGeom);
      timer.stop();
      timer.print();
      final KDTreeMetrics metrics = new KDTreeMetrics(tree);
      System.out.println(metrics);
      // Sanity Check
      assertThat("Primitive Count", metrics.totalPrimitives - metrics.duplicatedPrimitives, equalTo(69451));

      assertThat("Average leaf depth", metrics.avgDepth, greaterThan(23.4473f));
      assertThat("Max leaf Primitives", metrics.maxLeafPrimitives, equalTo(11));
      assertThat("Duplicated Primitives", metrics.duplicatedPrimitives, equalTo(2421534));
      assertThat("Leaf Volume", metrics.leafVolume / metrics.treeVolume, allOf(greaterThan(0.2141701), lessThan(0.2141702)));
   }

   private static TriangleMesh loadBunny() {
      ZipInputStream stream = null;
      try {
         stream = new ZipInputStream(KDTreeTest.class.getResourceAsStream(bunnyResource));
         stream.getNextEntry();
         return PLYParser.parseTriangleMesh(stream, false);
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

   private static Geometry createOpenBoxGeometry() {
      final float[] vb = new float[] {5,0,-5, -5, 0, -5, -5, 0, 5, 5, 0, 5,
            5, 10, -5, -5, 10, -5, -5, 10, 5, 5, 10, 5};
      final int[] indices = { 0, 1, 2, 0, 2, 3, 4, 5, 1, 4, 1, 0, 5, 6, 2, 5, 2, 1, 6, 7, 3, 6, 3, 2, 7, 4, 0, 7, 0, 3 };
      return new TriangleMesh(vb, indices);
   }
}
