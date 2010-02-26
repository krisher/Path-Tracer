package edu.rit.krisher.scene.geometry;

import edu.rit.krisher.raytracer.rays.HitData;
import edu.rit.krisher.scene.AxisAlignedBoundingBox;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.scene.material.LambertBRDF;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

public class Triangles implements Geometry {

   private final Vec3[] vertexBuffer;
   private final int[] indexBuffer;
   private Material material = new LambertBRDF(Color.white);

   public Triangles(Vec3[] verts, int... indices) {
      this.vertexBuffer = verts;
      this.indexBuffer = indices;
      if (indices.length % 3 != 0)
         throw new IllegalArgumentException("Indices must contain three values per triangle");
   }

   @Override
   public void getHitData(HitData hitData, Ray ray, double isectDist) {
      for (int idx = 0; idx < indexBuffer.length; idx += 3) {
         Vec3 v0 = vertexBuffer[indexBuffer[idx]];
         Vec3 e1 = new Vec3(vertexBuffer[indexBuffer[idx + 1]]).subtract(v0);
         Vec3 e2 = new Vec3(vertexBuffer[indexBuffer[idx + 2]]).subtract(v0);
         double t = ray.intersectsTriangle(v0, e1, e2);
         if (t == isectDist) {
            hitData.material = material;
            hitData.materialCoords = null;
            hitData.surfaceNormal = normalFor(idx, ray.direction);
            return;
         }
      }
   }

   @Override
   public double intersects(Ray ray) {
      double isectDist = Double.POSITIVE_INFINITY;
      int isectTri = -1;
      for (int idx = 0; idx < indexBuffer.length; idx += 3) {
         Vec3 v0 = vertexBuffer[indexBuffer[idx]];
         Vec3 e1 = new Vec3(vertexBuffer[indexBuffer[idx + 1]]).subtract(v0);
         Vec3 e2 = new Vec3(vertexBuffer[indexBuffer[idx + 2]]).subtract(v0);
         double t = ray.intersectsTriangle(v0, e1, e2);
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

   private Vec3 normalFor(int idx, Vec3 rayDirection) {
      Vec3 v0 = vertexBuffer[indexBuffer[idx]];
      Vec3 e1 = new Vec3(vertexBuffer[indexBuffer[idx + 1]]).subtract(v0);
      Vec3 e2 = new Vec3(vertexBuffer[indexBuffer[idx + 2]]).subtract(v0);
      Vec3 normal = e1.cross(e2).normalize();
      if (rayDirection.dot(normal) > 0)
         return normal.inverted();
      return normal;
   }

   public Material getMaterial() {
      return material;
   }

   public void setMaterial(Material material) {
      this.material = material;
   }

   @Override
   public AxisAlignedBoundingBox getBounds() {
      double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY, minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

      for (Vec3 vec : vertexBuffer) {
         if (vec.x < minX)
            minX = vec.x;
         if (vec.x > maxX)
            maxX = vec.x;

         if (vec.y < minY)
            minY = vec.y;
         if (vec.y > maxY)
            maxY = vec.y;

         if (vec.z < minZ)
            minZ = vec.z;
         if (vec.z > maxZ)
            maxZ = vec.z;
      }

      final AxisAlignedBoundingBox box = new AxisAlignedBoundingBox();
      box.minXYZ = new Vec3(minX, minY, minZ);
      box.maxXYZ = new Vec3(maxX, maxY, maxZ);
      return box;
   }

}
