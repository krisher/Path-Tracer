package edu.rit.krisher.scene.geometry;

import edu.rit.krisher.raytracer.rays.GeometryRay;
import edu.rit.krisher.raytracer.rays.IntersectionInfo;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.scene.material.DiffuseMaterial;
import edu.rit.krisher.vecmath.AxisAlignedBoundingBox;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

public class Sphere implements Geometry {

   protected final double radius;
   protected final Vec3 center;
   protected Material material = new DiffuseMaterial(Color.white);

   public Sphere() {
      this(Vec3.zero, 1, null);
   }

   public Sphere(final double radius) {
      this(Vec3.zero, radius, null);
   }

   public Sphere(final double radius, final Material material) {
      this(Vec3.zero, radius, material);
   }

   public Sphere(final Vec3 center, final double radius, final Material material) {
      this.radius = radius;
      if (material != null) {
         setMaterial(material);
      }
      this.center = center;
   }

   @Override
   public void getHitData(final GeometryRay ray, final IntersectionInfo data) {
      final Vec3 isectNormal = ray.getPointOnRay(ray.t);
      isectNormal.subtract(center).multiply(1.0 / radius);
      data.material = material;
      data.surfaceNormal.set(isectNormal);
      Vec3.computePerpendicularVec(data.tangentVector, data.surfaceNormal);
      data.materialCoords = null;
   }

   public Material getMaterial() {
      return material;
   }

   @Override
   public boolean intersects(final GeometryRay ray) {
      final double dist = ray.intersectsSphere(center, radius);
      if (dist > 0 && dist < ray.t) {
         ray.hitGeometry = this;
         ray.t = dist;
         return true;
      }
      return false;
   }

   @Override
   public boolean intersectsP(final Ray ray) {
      final double dist = ray.intersectsSphere(center, radius);
      return (dist > 0 && dist < ray.t);
   }

   @Override
   public boolean intersectsPrimitive(final Ray ray, final int primitiveID) {
      final double dist = ray.intersectsSphere(center, radius);
      if (dist > 0 && dist < ray.t) {
         ray.t = dist;
         return true;
      }
      return false;
   }

   public void setMaterial(final Material material) {
      this.material = material;
   }

   @Override
   public double getSurfaceArea(final int primIndices) {
      return 4.0 * Math.PI * radius * radius;
   }

   /*
    * @see edu.rit.krisher.scene.Geometry#getBounds()
    */
   @Override
   public AxisAlignedBoundingBox getBounds(final int primIndices) {
      return new AxisAlignedBoundingBox(center.x - radius, center.y - radius, center.z - radius, center.x + radius, center.y
                                        + radius, center.z + radius);
   }

   @Override
   public int getPrimitiveCount() {
      return 1;
   }

}
