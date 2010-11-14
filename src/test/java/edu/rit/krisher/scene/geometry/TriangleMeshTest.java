package edu.rit.krisher.scene.geometry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.rit.krisher.raytracer.rays.GeometryRay;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.vecmath.Vec3;

public class TriangleMeshTest {

   private TriangleMesh xyQuad;

   @Before
   public void initTestPLY() {
      final float[] verts = new float[] {
            1,1,0, -1, 1, 0, -1, -1, 0, 1, -1, 0
      };
      xyQuad = new TriangleMesh(verts, new int[] {0,1,2,0,2,3});

   }

   @Test
   public void zParallelRaysShouldIntersect() {
      final Vec3 direction = new Vec3(0, 0, 1);
      for (float delta = -1f; delta <= 1f; delta += 0.05) {
         for (float delta2 = -1f; delta2 <= 1f; delta2 += 0.05) {
            final GeometryRay zRay = new GeometryRay(new Vec3(delta, delta2, -1), direction);
            zRay.primitiveID = Geometry.ALL_PRIMITIVES;
            zRay.hitGeometry = null;
            zRay.t = Double.POSITIVE_INFINITY;
            xyQuad.intersects(zRay);
            Assert.assertEquals(1.0f, zRay.t, 0.00000001);
         }
      }
   }

   @Test
   public void sphereRaysShouldIntersect() {
      final Vec3 origin = new Vec3(0, 0, -1);
      for (float delta = -0.95f; delta <= 0.95f; delta += 0.05) {
         for (float delta2 = -0.95f; delta2 <= 0.95f; delta2 += 0.05) {
            final GeometryRay ray = new GeometryRay(origin, new Vec3(delta, delta2, 0).subtract(origin).normalize());
            ray.primitiveID = Geometry.ALL_PRIMITIVES;
            ray.hitGeometry = null;
            ray.t = Double.POSITIVE_INFINITY;
            xyQuad.intersects(ray);
            Assert.assertTrue("Ray: " + ray + " should intersect quad.", ray.t >= 1.0f);
         }
      }
   }
}
