package edu.rit.krisher.scene.light;

import java.util.Random;

import edu.rit.krisher.raytracer.rays.HitData;
import edu.rit.krisher.scene.AxisAlignedBoundingBox;
import edu.rit.krisher.scene.EmissiveGeometry;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

public class PointLight implements EmissiveGeometry {

   private final Vec3 position;
   private final Color material;
   private double intensity = 100.0;

   public PointLight(Vec3 position, float r, float g, float b) {
      this.position = position;
      this.material = new Color(r, g, b);
   }

   public PointLight(Vec3 position, float r, float g, float b, double intensity) {
      this.position = position;
      this.material = new Color(r, g, b);
      this.intensity = intensity;
   }

   @Override
   public double sampleEmissiveRadiance(Vec3 directionOut, Color radianceOut, Vec3 normalOut, Vec3 origin, Random rng) {
      directionOut.set(position).subtract(origin).normalize();
      radianceOut.set(material);
      normalOut.set(origin).subtract(position);
      final double dist = normalOut.length();
      normalOut.multiply(1.0 / dist);
      return dist;
   }

   @Override
   public void getHitData(HitData data, Ray ray, double isectDist) {
      data.material = material;
      data.surfaceNormal = ray.direction.inverted();
   }

   @Override
   public double intersects(Ray ray) {
      return -1;
   }

   public double getIntensity() {
      return intensity;
   }

   public void setIntensity(double intensity) {
      this.intensity = intensity;
   }
   
   public double getSurfaceArea() {
      return 0;
   }

   /* 
    * @see edu.rit.krisher.scene.Geometry#getBounds()
    */
   @Override
   public AxisAlignedBoundingBox getBounds() {
      final AxisAlignedBoundingBox bounds = new AxisAlignedBoundingBox();
      bounds.minXYZ.set(position);
      bounds.maxXYZ.set(position);
      return bounds;
   }
   
   
}
