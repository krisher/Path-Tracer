package edu.rit.krisher.scene.geometry;

import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.GeometryIntersection;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.MaterialInfo;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.scene.material.LambertBRDF;
import edu.rit.krisher.vecmath.AxisAlignedBoundingBox;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

public class Sphere implements Geometry {

   protected final double radius;
   protected final Vec3 center;
   protected Material material = new LambertBRDF(Color.white);

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
   public void getHitData(final MaterialInfo data, final int primitiveID, final Ray ray, final double distance) {
      final Vec3 isectNormal = ray.getPointOnRay(distance);
      isectNormal.subtract(center).multiply(1.0 / radius);
      data.material = material;
      data.surfaceNormal.set(isectNormal);
      MaterialInfo.computeTangentVector(data.tangentVector, data.surfaceNormal);
      data.materialCoords = null;
   }

   public Material getMaterial() {
      return material;
   }

   @Override
   public double intersects(final GeometryIntersection intersection, final Ray ray, final double maxDistance) {
      return ray.intersectsSphere(center, radius);
   }

   @Override
   public double intersectsPrimitive(final Ray ray, final double maxDistance, final int primitiveID) {
      return ray.intersectsSphere(center, radius);
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
