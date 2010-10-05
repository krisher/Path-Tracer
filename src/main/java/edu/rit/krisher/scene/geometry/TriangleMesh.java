package edu.rit.krisher.scene.geometry;

import edu.rit.krisher.raytracer.rays.HitData;
import edu.rit.krisher.scene.AxisAlignedBoundingBox;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.geometry.buffer.IndexBuffer;
import edu.rit.krisher.scene.geometry.buffer.Vec3Buffer;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.scene.material.LambertBRDF;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

public class TriangleMesh implements Geometry {

   private Material material = new LambertBRDF(Color.white);
   private final Vec3Buffer vertices;
   private final IndexBuffer triangleIndices;
   private final AxisAlignedBoundingBox bounds;
   private final int triCount;

   public TriangleMesh(final Vec3Buffer verts, final IndexBuffer triangles) {
      this.vertices = verts;
      this.triangleIndices = triangles;
      this.triCount = triangleIndices.limit() / 3;
      this.bounds = verts.computeBounds();
   }

   public int getTriangleCount() {
      return triCount;
   }

   @Override
   public double getSurfaceArea() {
      return bounds.surfaceArea();
   }

   @Override
   public void getHitData(final HitData data, final Ray ray, final double isectDist) {
      // TODO: no need to perform the intersection again if we could store the hit data (tri index)
      // in the original intersects(...) call.

      int isectTri = -1;
      final Vec3 v0 = new Vec3();
      final Vec3 e1 = new Vec3();
      final Vec3 e2 = new Vec3();
      for (int idx = 0; idx < triCount; ++idx) {

         getTriangleVEE(idx * 3, v0, e1, e2);

         final double t = ray.intersectsTriangle(v0, e1, e2);
         if (isectDist == t) {
            isectTri = idx;
            break;
         }
      }
      if (isectTri >= 0) {
         getTriangleHitData(isectTri, data, ray);
         return;
      }
   }

   private final void normalFor(final Vec3 result, final int idx) {
      final double[] vecs = new double[9];
      final int idxOffset = idx * 3;
      vertices.get(vecs, triangleIndices.get(idxOffset), triangleIndices.get(idxOffset + 1), triangleIndices.get(idxOffset + 2));
      /*
       * Compute edge1 (indices 3-5), edge2 (indices 6-8) by subtracting the first triangle vertex (indices 0-2) from the second two.
       */
      for (int i=0; i < 3; ++i) {
         vecs[3 + i] -= vecs[i];
         vecs[6 + i] -= vecs[i];
      }
      /*
       * Cross product of edge1, edge2.
       */
      result.set(vecs[4] * vecs[8] - vecs[5] * vecs[7], vecs[5] * vecs[6] - vecs[3] * vecs[8], vecs[3] * vecs[7] - vecs[4] * vecs[6]).normalize();
   }

   private final boolean boundsCheck(final Ray ray) {
      final Vec3 boxSpans = new Vec3();
      final Vec3 boxCenter = new Vec3();
      boxSpans.set(bounds.maxXYZ).subtract(bounds.minXYZ);
      boxCenter.set(bounds.minXYZ).scaleAdd(boxSpans, 0.5);
      boxSpans.multiply(0.5);
      return (ray.intersectsBox(boxCenter, boxSpans.x, boxSpans.y, boxSpans.z) > 0);
   }

   @Override
   public double intersects(final Ray ray) {
      if (triCount > 10 && !boundsCheck(ray))
         return 0;
      double isectDist = Double.POSITIVE_INFINITY;
      int isectTri = -1;
      final Vec3 v0 = new Vec3();
      final Vec3 e1 = new Vec3();
      final Vec3 e2 = new Vec3();
      for (int idx = 0; idx < triCount; ++idx) {
         getTriangleVEE(idx * 3, v0, e1, e2);

         final double t = ray.intersectsTriangle(v0, e1, e2);
         if (t > 0 && t < isectDist) {
            isectDist = t;
            isectTri = idx;
         }
      }
      if (isectTri >= 0) {
         return isectDist;
      }
      return 0;
   }

   private final void getTriangleVEE(final int idxOffset, final Vec3 v0, final Vec3 edge1, final Vec3 edge2) {
      vertices.get(triangleIndices.get(idxOffset), v0);
      vertices.get(triangleIndices.get(idxOffset + 1), edge1);
      edge1.subtract(v0);
      vertices.get(triangleIndices.get(idxOffset + 2), edge2);
      edge2.subtract(v0);
   }

   @Override
   public AxisAlignedBoundingBox getBounds() {
      return new AxisAlignedBoundingBox(bounds);
   }

   public Material getMaterial() {
      return material;
   }

   public void setMaterial(final Material material) {
      this.material = material;
   }

   @Override
   public Geometry[] getPrimitives() {
      final Geometry[] triangles = new Geometry[triCount];
      for (int i = 0; i < triCount; i++) {
         triangles[i] = new MeshTriangle(i);
      }
      return triangles;
   }

   private final void getTriangleHitData(final int triangleIndex, final HitData data, final Ray ray) {
      data.material = material;
      data.materialCoords = null;
      normalFor(data.surfaceNormal, triangleIndex);
   }

   class MeshTriangle implements Geometry {
      private final int triangleIndexOffset;

      public MeshTriangle(final int triangleIndex) {
         this.triangleIndexOffset = triangleIndex * 3;
      }

      @Override
      public void getHitData(final HitData data, final Ray ray, final double isectDist) {
         getTriangleHitData(triangleIndexOffset / 3, data, ray);
      }

      @Override
      public double getSurfaceArea() {
         final Vec3 v0 = new Vec3();
         final Vec3 e1 = new Vec3();
         final Vec3 e2 = new Vec3();
         getTriangleVEE(triangleIndexOffset, v0, e1, e2);
         return 0.5 * e1.cross(e2).length();
      }

      @Override
      public double intersects(final Ray ray) {
         final Vec3 v0 = new Vec3();
         final Vec3 e1 = new Vec3();
         final Vec3 e2 = new Vec3();
         getTriangleVEE(triangleIndexOffset, v0, e1, e2);
         return ray.intersectsTriangle(v0, e1, e2);
      }

      @Override
      public AxisAlignedBoundingBox getBounds() {
         return vertices.computeBounds(triangleIndices.get(triangleIndexOffset), triangleIndices.get(triangleIndexOffset + 1), triangleIndices.get(triangleIndexOffset + 2));
      }

      @Override
      public Geometry[] getPrimitives() {
         return new Geometry[] { this };
      }

   }
}
