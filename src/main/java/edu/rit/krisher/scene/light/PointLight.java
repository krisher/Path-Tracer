package edu.rit.krisher.scene.light;

import java.util.Random;

import edu.rit.krisher.raytracer.rays.GeometryIntersection;
import edu.rit.krisher.raytracer.rays.IntersectionInfo;
import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.raytracer.sampling.SamplingUtils;
import edu.rit.krisher.scene.EmissiveGeometry;
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
   public int sampleIrradiance(final SampleRay[] samples, final Vec3 point, final Random rng) {
      final SampleRay wo = samples[0];
      wo.origin.set(point);
      wo.direction.set(position).subtract(wo.origin);
      wo.t = wo.direction.length();
      wo.direction.multiply(1.0 / wo.t);
      wo.intersection.surfaceNormal.set(wo.direction).multiply(-1);
      wo.throughput.set(material);
      wo.intersection.hitGeometry = this;
      return 1;
   }

   @Override
   public void getHitData(final Ray ray, final IntersectionInfo data) {
      data.material = material;
      data.surfaceNormal.set(ray.direction).multiply(-1);
   }

   @Override
   public boolean intersects(final Ray ray, final GeometryIntersection intersection) {
      return false;
   }

   @Override
   public boolean intersects(final Ray ray) {
      return false;
   }

   @Override
   public boolean intersectsPrimitive(final Ray ray, final int primitiveID) {
      return false;
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

   @Override
   public int sampleEmission(final SampleRay[] woSamples, final int woOffset, final int woCount,
         final Random rng) {
      for (int i = 0; i < woCount; ++i) {
         final SampleRay wo = woSamples[i + woOffset];
         wo.origin.set(position);
         SamplingUtils.uniformSampleSphere(wo.direction, rng);
         wo.throughput.set(material);
      }
      return woCount;
   }

}
