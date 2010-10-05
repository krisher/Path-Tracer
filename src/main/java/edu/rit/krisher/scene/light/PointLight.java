package edu.rit.krisher.scene.light;

import java.util.Random;

import edu.rit.krisher.raytracer.rays.HitData;
import edu.rit.krisher.scene.AxisAlignedBoundingBox;
import edu.rit.krisher.scene.EmissiveGeometry;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

public class PointLight implements EmissiveGeometry {

   private final Vec3 position;
   private final Color material;
   private double intensity = 100.0;

   public PointLight(final Vec3 position, final float r, final float g, final float b) {
      this.position = position;
      this.material = new Color(r, g, b);
   }

   public PointLight(final Vec3 position, final float r, final float g, final float b, final double intensity) {
      this.position = position;
      this.material = new Color(r, g, b);
      this.intensity = intensity;
   }

   @Override
   public double sampleEmissiveRadiance(final Vec3 directionOut, final Color radianceOut, final Vec3 normalOut,
         final Vec3 origin, final Random rng) {
      directionOut.set(position).subtract(origin).normalize();
      radianceOut.set(material);
      normalOut.set(origin).subtract(position);
      final double dist = normalOut.length();
      normalOut.multiply(1.0 / dist);
      return dist;
   }

   @Override
   public void getHitData(final HitData data, final Ray ray, final double isectDist) {
      data.material = material;
      data.surfaceNormal.set(ray.direction).multiply(-1);
   }

   @Override
   public double intersects(final Ray ray) {
      return -1;
   }

   public double getIntensity() {
      return intensity;
   }

   public void setIntensity(final double intensity) {
      this.intensity = intensity;
   }

   @Override
   public double getSurfaceArea() {
      return 0;
   }

   @Override
   public Geometry[] getPrimitives() {
      return new Geometry[] { this };
   }

   /*
    * @see edu.rit.krisher.scene.Geometry#getBounds()
    */
   @Override
   public AxisAlignedBoundingBox getBounds() {
      return new AxisAlignedBoundingBox(position, position);
   }

}
