package edu.rit.krisher.scene.material;

import java.util.Random;

import edu.rit.krisher.raytracer.rays.IntersectionInfo;
import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.raytracer.sampling.SamplingUtils;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.geometry.utils.ShadingUtils;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

public class DiffuseMaterial implements Material, Cloneable {

   private Texture diffuse;

   public DiffuseMaterial(final Texture diffuse) {
      this.diffuse = diffuse;
   }

   @Override
   public void getEmissionColor(final Color emissionOut, final Ray sampleDirection, final IntersectionInfo parameters) {
      emissionOut.set(0, 0, 0);
   }

   @Override
   public void evaluateBRDF(final Color radiance, final Vec3 wo, final Vec3 wi, final IntersectionInfo parameters) {
      /*
       * BRDF = 1/pi * diffuse
       */
      final Color diffColor = this.diffuse.getColor(parameters.materialCoords);
      final double factor = 1.0 / Math.PI;
      radiance.multiply(diffColor.r * factor, diffColor.g * factor, diffColor.b * factor);
   }

   @Override
   public double sampleBRDF(final SampleRay wi, final Vec3 wo, final IntersectionInfo parameters, final Random rng) {
      final double pdf = SamplingUtils.cosSampleHemisphere(wi.direction, rng); //Sample hemisphere surrounding z axis according to cosine dist.
      ShadingUtils.shadingCoordsToWorld(wi.direction, parameters.surfaceNormal, parameters.tangentVector); //Transform sample direction to world coordinates.
      /*
       * Evaluate: brdf(Ki, Ko) / pdf
       * 
       * Where pdf = (cos(theta_i) / Pi)
       * and BRDF = color / Pi 
       */
      wi.throughput.set(diffuse.getColor(parameters.materialCoords)).multiply(1.0 / Math.PI );
      wi.specularBounce = false;
      return pdf;
   }

   public boolean isTranslucent() {
      return false;
   }

   public Texture getDiffuse() {
      return diffuse;
   }

   public void setDiffuse(final Texture diffuse) {
      this.diffuse = diffuse;
   }

   @Override
   public boolean isDiffuse() {
      return true;
   }

}
