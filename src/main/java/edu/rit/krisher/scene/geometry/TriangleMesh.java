package edu.rit.krisher.scene.geometry;

import edu.rit.krisher.raytracer.rays.GeometryRay;
import edu.rit.krisher.raytracer.rays.IntersectionInfo;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.geometry.utils.Vec3fBufferUtils;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.scene.material.DiffuseMaterial;
import edu.rit.krisher.vecmath.AxisAlignedBoundingBox;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Transform;
import edu.rit.krisher.vecmath.Vec3;

public class TriangleMesh implements Geometry {

   private Material material = new DiffuseMaterial(Color.white);
   private final float[] vertices;
   private final float[] normals;
   private final int[] triangleIndices;
   private final AxisAlignedBoundingBox bounds;
   private final int triCount;

   public TriangleMesh(final float[] verts, final int[] triangles) {
      this.vertices = verts;
      this.triangleIndices = triangles;
      this.triCount = triangleIndices.length / 3;
      this.bounds = Vec3fBufferUtils.computeBounds(vertices);
      normals = null;
   }

   public TriangleMesh(final float[] verts, final float[] normals, final int[] triangles) {
      this.vertices = verts;
      this.triangleIndices = triangles;
      this.triCount = triangleIndices.length / 3;
      this.bounds = Vec3fBufferUtils.computeBounds(vertices);
      this.normals = normals;
   }

   public static float[] computeTriangleNormals(final float[] vertices, final int[] triangleIndices) {
      final int triCount = triangleIndices.length / 3;
      final float[] normals = new float[vertices.length];
      final Vec3 v0 = new Vec3();
      final Vec3 e1 = new Vec3();
      final Vec3 e2 = new Vec3();
      for (int i = 0; i < triCount; i++) {
         final int v0Idx = triangleIndices[i * 3];
         final int v1Idx = triangleIndices[i * 3 + 1];
         final int v2Idx = triangleIndices[i * 3 + 2];
         Vec3fBufferUtils.get(v0, vertices, v0Idx);
         Vec3fBufferUtils.get(e1, vertices, v1Idx);
         Vec3fBufferUtils.get(e2, vertices, v2Idx);
         e1.subtract(v0);
         e2.subtract(v0);
         /*
          * Face normal is the cross product of the two edges eminating from v0.
          */
         e1.cross(e2).normalize();
         /*
          * Add normal to the corresponding cumulative sums...
          */
         Vec3fBufferUtils.add(e1, normals, v0Idx);
         Vec3fBufferUtils.add(e1, normals, v1Idx);
         Vec3fBufferUtils.add(e1, normals, v2Idx);
      }
      for (int i = 0; i < normals.length / 3; i++) {
         Vec3fBufferUtils.normalize(normals, i);
      }
      return normals;
   }
   
   public float[] getVertices() {
      return vertices;
   }
   
   public int[] getTriIndices() {
      return triangleIndices;
   }

   public void transform(final Transform transform) {
      final Vec3 vert = new Vec3();
      for (int i = 0; i < vertices.length / 3; i++) {
         Vec3fBufferUtils.get(vert, vertices, i);
         Vec3fBufferUtils.put(transform.transformPoint(vert), vertices, i);
      }
      bounds.set(Vec3fBufferUtils.computeBounds(vertices));
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
   public void getHitData(final GeometryRay ray, final IntersectionInfo data) {
      data.material = material;
      data.materialCoords = null;
      if (normals == null) {
         getTriangleFaceNormal(data.surfaceNormal, data.tangentVector, ray.primitiveID);
         /*
          * Use an arbitrary triangle edge for the tangent vector.
          * TODO: u,v coordinates required to support anisotropic reflection models, for consistent orientation.
          */

      } else {
         // Barycentric normal interpolation if vertex normals present...
         final double[] baryCoords = new double[3];
         intersectsTriangleBarycentric(baryCoords, ray, ray.primitiveID);
         interpolatedNormal(data.surfaceNormal, baryCoords[1], baryCoords[2], ray.primitiveID);
         // TODO: Tangent vector should be based on shading (texture) coords if specified.
         Vec3.computePerpendicularVec(data.tangentVector, data.surfaceNormal);
      }
   }

   @Override
   public final boolean intersects(final GeometryRay ray) {
      for (int idx = 0; idx < triCount; ++idx) {
         final double t = intersectsTriangle(ray, idx);
         if (t > 0 && t < ray.t) {
            ray.t = t;
            ray.primitiveID = idx;
            ray.hitGeometry = this;
         }
      }
      return ray.hitGeometry == this;
   }

   @Override
   public final boolean intersectsP(final Ray ray) {
      for (int idx = 0; idx < triCount; ++idx) {
         final double t = intersectsTriangle(ray, idx);
         if (t > 0 && t < ray.t) {
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean intersectsPrimitive(final Ray ray, final int primitiveID) {
      final double t = intersectsTriangle(ray, primitiveID);
      if (t > 0 && t < ray.t) {
         ray.t = t;
         return true;
      }
      return false;
   }

   @Override
   public AxisAlignedBoundingBox getBounds(final int primID) {
      if (primID < 0) {
         return new AxisAlignedBoundingBox(bounds);
      } else {
         final int triangleIndexOffset = primID * 3;
         return Vec3fBufferUtils.computeBounds(vertices, triangleIndices[triangleIndexOffset], triangleIndices[triangleIndexOffset + 1], triangleIndices[triangleIndexOffset + 2]);
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
   @Override
   public int getPrimitiveCount() {
      return triCount;
   }

   /**
    * @param vecs
    */
   private final void getTriangleVEE(final double[] vecs, final int triangleIndexOffset) {
      final int v0Offs = triangleIndices[triangleIndexOffset] * 3;
      final int v1Offs = triangleIndices[triangleIndexOffset + 1] * 3;
      final int v2Offs = triangleIndices[triangleIndexOffset + 2] * 3;

      vecs[0] = vertices[v0Offs];
      vecs[1] = vertices[v0Offs + 1];
      vecs[2] = vertices[v0Offs + 2];

      vecs[3] = vertices[v1Offs] - vecs[0];
      vecs[4] = vertices[v1Offs + 1] - vecs[1];
      vecs[5] = vertices[v1Offs + 2] - vecs[2];

      vecs[6] = vertices[v2Offs] - vecs[0];
      vecs[7] = vertices[v2Offs + 1] - vecs[1];
      vecs[8] = vertices[v2Offs + 2] - vecs[2];
   }

   private final void getTriangleFaceNormal(final Vec3 outNormal, final Vec3 tangent, final int triangleIndex) {
      final int triangleIndexOffset = triangleIndex * 3;
      final int v0Offs = triangleIndices[triangleIndexOffset] * 3;
      final int v1Offs = triangleIndices[triangleIndexOffset + 1] * 3;
      final int v2Offs = triangleIndices[triangleIndexOffset + 2] * 3;

      final float v0X = vertices[v0Offs];
      final float v0Y = vertices[v0Offs + 1];
      final float v0Z = vertices[v0Offs + 2];
      outNormal.set(vertices[v1Offs] - v0X, vertices[v1Offs + 1] - v0Y, vertices[v1Offs + 2] - v0Z);
      outNormal.cross(vertices[v2Offs] - v0X, vertices[v2Offs + 1] - v0Y, vertices[v2Offs + 2] - v0Z).normalize();
      tangent.set(vertices[v1Offs] - v0X, vertices[v1Offs + 1] - v0Y, vertices[v1Offs + 2] - v0Z).normalize();
   }

   private final double intersectsTriangle(final Ray ray, final int triangleIndex) {
      final int triangleIndexOffset = triangleIndex * 3;
      final int v0Offs = triangleIndices[triangleIndexOffset] * 3;
      final int v1Offs = triangleIndices[triangleIndexOffset + 1] * 3;
      final int v2Offs = triangleIndices[triangleIndexOffset + 2] * 3;

      final float v0X = vertices[v0Offs];
      final float v0Y = vertices[v0Offs + 1];
      final float v0Z = vertices[v0Offs + 2];
      return ray.intersectsTriangle(v0X, v0Y, v0Z, vertices[v1Offs] - v0X, vertices[v1Offs + 1] - v0Y, vertices[v1Offs + 2]
                                                                                                                - v0Z, vertices[v2Offs] - v0X, vertices[v2Offs + 1] - v0Y, vertices[v2Offs + 2] - v0Z);
   }

   private final boolean intersectsTriangleBarycentric(final double[] tuv, final Ray ray, final int triangleIndex) {
      final int triangleIndexOffset = triangleIndex * 3;
      final int v0Offs = triangleIndices[triangleIndexOffset] * 3;
      final int v1Offs = triangleIndices[triangleIndexOffset + 1] * 3;
      final int v2Offs = triangleIndices[triangleIndexOffset + 2] * 3;
      final float v0X = vertices[v0Offs];
      final float v0Y = vertices[v0Offs + 1];
      final float v0Z = vertices[v0Offs + 2];
      return ray.intersectsTriangleBarycentric(tuv, v0X, v0Y, v0Z, vertices[v1Offs] - v0X, vertices[v1Offs + 1] - v0Y, vertices[v1Offs + 2]
                                                                                                                                - v0Z, vertices[v2Offs] - v0X, vertices[v2Offs + 1] - v0Y, vertices[v2Offs + 2] - v0Z);
   }

   private final void interpolatedNormal(final Vec3 normal, final double u, final double v, final int triangleIndex) {
      final int triangleIndexOffset = triangleIndex * 3;
      final int v0Offs = triangleIndices[triangleIndexOffset] * 3;
      final int v1Offs = triangleIndices[triangleIndexOffset + 1] * 3;
      final int v2Offs = triangleIndices[triangleIndexOffset + 2] * 3;

      final double w = 1.0 - u - v;
      normal.x = w * normals[v0Offs] + u * normals[v1Offs] + v * normals[v2Offs];
      normal.y = w * normals[v0Offs + 1] + u * normals[v1Offs + 1] + v * normals[v2Offs + 1];
      normal.z = w * normals[v0Offs + 2] + u * normals[v1Offs + 2] + v * normals[v2Offs + 2];
      normal.normalize();
   }

}
