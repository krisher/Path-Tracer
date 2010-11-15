package edu.rit.krisher.scene.light;

import edu.rit.krisher.raytracer.rays.GeometryRay;
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
   public void sampleIrradiance(final SampleRay wo, final Vec3 point, final float r1, final float r2) {
      wo.origin.set(point);
      wo.direction.set(position).subtract(wo.origin);
      wo.t = wo.direction.length();
      wo.direction.multiply(1.0 / wo.t);
      wo.intersection.surfaceNormal.set(wo.direction).multiply(-1);
      wo.throughput.set(material);
      wo.hitGeometry = this;
   }

   @Override
   public void getHitData(final GeometryRay ray, final IntersectionInfo data) {
      data.material = material;
      data.surfaceNormal.set(ray.direction).multiply(-1);
   }

   @Override
   public boolean intersects(final GeometryRay ray) {
      return false;
   }

   @Override
   public boolean intersectsP(final Ray ray) {
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
   public void sampleEmission(final SampleRay wo, final float r1, final float r2) {
      wo.origin.set(position);
      SamplingUtils.uniformSampleSphere(wo.direction, r1, r2);
      wo.throughput.set(material);
   }

}
