package edu.rit.krisher.scene.light;

import java.util.Random;

import edu.rit.krisher.scene.EmissiveGeometry;
import edu.rit.krisher.scene.GeometryIntersection;
import edu.rit.krisher.scene.MaterialInfo;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.vecmath.AxisAlignedBoundingBox;
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
   public void getHitData(final MaterialInfo data, final int primitiveID, final Ray ray, final double distance) {
      data.material = material;
      data.surfaceNormal.set(ray.direction).multiply(-1);
   }

   @Override
   public double intersects(final GeometryIntersection intersection, final Ray ray, final double maxDistance) {
      return -1;
   }

   @Override
   public double intersectsPrimitive(final Ray ray, final double maxDistance,
         final int primitiveID) {
      return -1;
   }

   public double getIntensity() {
      return intensity;
   }

   public void setIntensity(final double intensity) {
      this.intensity = intensity;
   }

   @Override
   public double getSurfaceArea(final int primIndices) {
      return 0;
   }

   @Override
   public int getPrimitiveCount() {
      return 1;
   }

   /*
    * @see edu.rit.krisher.scene.Geometry#getBounds()
    */
   @Override
   public AxisAlignedBoundingBox getBounds(final int primIndices) {
      return new AxisAlignedBoundingBox(position, position);
   }

}
