package edu.rit.krisher.scene.geometry;

import edu.rit.krisher.raytracer.rays.HitData;
import edu.rit.krisher.scene.AxisAlignedBoundingBox;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.scene.material.LambertBRDF;
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
   public void getHitData(final HitData data, final Ray ray, final double isectDist) {
      final Vec3 isectNormal = ray.getPointOnRay(isectDist);
      isectNormal.subtract(center).multiply(1.0 / radius);
      data.material = material;
      data.surfaceNormal = isectNormal;
      data.materialCoords = null;
   }

   public Material getMaterial() {
      return material;
   }

   @Override
   public double intersects(final Ray ray) {
      return ray.intersectsSphere(center, radius);
   }

   public void setMaterial(final Material material) {
      this.material = material;
   }

   /* 
    * @see edu.rit.krisher.scene.Geometry#getBounds()
    */
   @Override
   public AxisAlignedBoundingBox getBounds() {
      final AxisAlignedBoundingBox box = new AxisAlignedBoundingBox();
      box.minXYZ.set(center.x - radius, center.y - radius, center.z - radius);
      box.maxXYZ.set(center.x + radius, center.y + radius, center.z + radius);
      return box;
   }

   
   
}
