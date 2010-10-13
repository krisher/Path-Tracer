package edu.rit.krisher.scene.geometry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.GeometryIntersection;
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

      xyQuad = new TriangleMesh(verts, new int[] {0,1,2,0,2,3});

   }

   @Test
   public void zParallelRaysShouldIntersect() {
      final Vec3 direction = new Vec3(0, 0, 1);
      final GeometryIntersection intersectionInfo = new GeometryIntersection();
      for (float delta = -1f; delta <= 1f; delta += 0.05) {
         for (float delta2 = -1f; delta2 <= 1f; delta2 += 0.05) {
            final Ray zRay = new Ray(new Vec3(delta, delta2, -1), direction);
            intersectionInfo.primitiveID = Geometry.ALL_PRIMITIVES;
            Assert.assertEquals(1.0f, xyQuad.intersects(intersectionInfo, zRay, Double.POSITIVE_INFINITY), 0.00000001);
         }
      }
   }

   @Test
   public void sphereRaysShouldIntersect() {
      final Vec3 origin = new Vec3(0, 0, -1);
      final GeometryIntersection intersectionInfo = new GeometryIntersection();
      for (float delta = -0.95f; delta <= 0.95f; delta += 0.05) {
         for (float delta2 = -0.95f; delta2 <= 0.95f; delta2 += 0.05) {
            final Ray ray = new Ray(origin, new Vec3(delta, delta2, 0).subtract(origin).normalize());
            intersectionInfo.primitiveID = Geometry.ALL_PRIMITIVES;
            Assert.assertTrue("Ray: " + ray + " should intersect quad.", xyQuad.intersects(intersectionInfo, ray, Double.POSITIVE_INFINITY) >= 1.0f);
         }
      }
   }
}
