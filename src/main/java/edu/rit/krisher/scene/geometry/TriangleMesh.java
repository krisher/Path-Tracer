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

   @Override
   public double getSurfaceArea(final int primIndex) {
      if (primIndex < 0)
         return bounds.surfaceArea();
      final double[] vecs = new double[9];
      getTriangleVEE(vecs, primIndex * 3);
      /*
       * Cross product of edge1, edge2.
       */
      final double cX = vecs[4] * vecs[8] - vecs[5] * vecs[7];
      final double cY = vecs[5] * vecs[6] - vecs[3] * vecs[8];
      final double cZ = vecs[3] * vecs[7] - vecs[4] * vecs[6];
      return 0.5 * Vec3.length(cX, cY, cZ);
   }

   @Override
   public void getHitData(final HitData data, final Ray ray, final double isectDist, final int primIndex) {
      final double[] triVerts = new double[9];
      if (primIndex < 0) {
         int isectTri = -1;
         for (int idx = 0; idx < triCount; ++idx) {
            getTriangleVEE(triVerts, idx * 3);
            final double t = ray.intersectsTriangle(triVerts);
            if (isectDist == t) {
               isectTri = idx;
               break;
            }
         }
         if (isectTri >= 0) {
            getTriangleHitData(isectTri, data, ray);
            return;
         }
      } else {
         getTriangleHitData(primIndex, data, ray);
      }
   }

   private final void normalFor(final Vec3 result, final int idx) {
      final double[] vecs = new double[9];
      getTriangleVEE(vecs, idx * 3);
      /*
       * Cross product of edge1, edge2.
       */
      result.set(vecs[4] * vecs[8] - vecs[5] * vecs[7], vecs[5] * vecs[6] - vecs[3] * vecs[8], vecs[3] * vecs[7]
            - vecs[4] * vecs[6]).normalize();
   }

   @Override
   public double intersects(final Ray ray, final int primIndex) {
      double isectDist = Double.POSITIVE_INFINITY;
      final double[] triVerts = new double[9];
      if (primIndex < 0) {
         int isectTri = -1;
         for (int idx = 0; idx < triCount; ++idx) {
            getTriangleVEE(triVerts, idx * 3);
            final double t = ray.intersectsTriangle(triVerts);
            if (t > 0 && t < isectDist) {
               isectDist = t;
               isectTri = idx;
            }
         }
         if (isectTri >= 0) {
            return isectDist;
         }
      } else {
         getTriangleVEE(triVerts, primIndex * 3);
         return ray.intersectsTriangle(triVerts);
      }
      return 0;
   }

   @Override
   public AxisAlignedBoundingBox getBounds(final int primIndex) {
      if (primIndex < 0) {
         return new AxisAlignedBoundingBox(bounds);
      } else {
         final int triangleIndexOffset = primIndex * 3;
         return vertices.computeBounds(triangleIndices.get(triangleIndexOffset), triangleIndices.get(triangleIndexOffset + 1), triangleIndices.get(triangleIndexOffset + 2));
      }
   }

   public Material getMaterial() {
      return material;
   }

   public void setMaterial(final Material material) {
      this.material = material;
   }

   /*
    * @see edu.rit.krisher.scene.Geometry#getPrimitiveCount()
    */
   public int getPrimitiveCount() {
      return triCount;
   }

   private final void getTriangleHitData(final int triangleIndex, final HitData data, final Ray ray) {
      data.material = material;
      data.materialCoords = null;
      normalFor(data.surfaceNormal, triangleIndex);
   }

   /**
    * @param vecs
    */
   private final void getTriangleVEE(final double[] vecs, final int triangleIndexOffset) {
      vertices.get(vecs, triangleIndices.get(triangleIndexOffset), triangleIndices.get(triangleIndexOffset + 1), triangleIndices.get(triangleIndexOffset + 2));
      /*
       * Compute edge1 (indices 3-5), edge2 (indices 6-8) by subtracting the first triangle vertex (indices 0-2) from
       * the second two.
       */
      for (int i = 0; i < 3; ++i) {
         vecs[3 + i] -= vecs[i];
         vecs[6 + i] -= vecs[i];
      }
   }

}
