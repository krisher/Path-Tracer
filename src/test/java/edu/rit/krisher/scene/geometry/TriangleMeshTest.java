package edu.rit.krisher.scene.geometry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.rit.krisher.scene.geometry.buffer.IndexBuffer;
import edu.rit.krisher.scene.geometry.buffer.Vec3Buffer;
import edu.rit.krisher.scene.geometry.buffer.Vec3fBuffer;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

public class TriangleMeshTest {

   private TriangleMesh xyQuad;

   @Before
   public void initTestPLY() {
      final Vec3Buffer verts = new Vec3fBuffer(4);
      verts.put(new Vec3(1, 1, 0));
      verts.put(new Vec3(-1, 1, 0));
      verts.put(new Vec3(-1, -1, 0));
      verts.put(new Vec3(1, -1, 0));
      verts.flip();

      final IndexBuffer idx = new IndexBuffer(6);
      idx.put(0).put(1).put(2);
      idx.put(0).put(2).put(3);
      idx.flip();

      xyQuad = new TriangleMesh(verts, idx);

   }

   @Test
   public void zParallelRaysShouldIntersect() {
      final Vec3 direction = new Vec3(0, 0, 1);
      for (float delta = -1f; delta <= 1f; delta += 0.05) {
         for (float delta2 = -1f; delta2 <= 1f; delta2 += 0.05) {
            final Ray zRay = new Ray(new Vec3(delta, delta2, -1), direction);
            Assert.assertEquals(1.0f, xyQuad.intersects(zRay), 0.00000001);
         }
      }
   }

   @Test
   public void sphereRaysShouldIntersect() {
      final Vec3 origin = new Vec3(0, 0, -1);
      for (float delta = -0.95f; delta <= 0.95f; delta += 0.05) {
         for (float delta2 = -0.95f; delta2 <= 0.95f; delta2 += 0.05) {
            final Ray ray = new Ray(origin, new Vec3(delta, delta2, 0).subtract(origin).normalize());
            Assert.assertTrue("Ray: " + ray + " should intersect quad.", xyQuad.intersects(ray) >= 1.0f);
         }
      }
   }
}
