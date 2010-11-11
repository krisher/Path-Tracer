package edu.rit.krisher.scene.material;

import java.util.Random;

import edu.rit.krisher.raytracer.rays.IntersectionInfo;
import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.raytracer.sampling.SamplingUtils;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.geometry.utils.ShadingUtils;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

public class LambertBRDF implements Material, Cloneable {

   private Texture diffuse;

   public LambertBRDF(final Texture diffuse) {
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
      final double factor = parameters.surfaceNormal.dot(wi) / Math.PI;
      radiance.multiply(diffColor.r * factor, diffColor.g * factor, diffColor.b * factor);
   }

   @Override
   public void sampleBRDF(final SampleRay wo, final Vec3 wi, final IntersectionInfo parameters, final Random rng) {
      Vec3 surfaceNormal = parameters.surfaceNormal;
      if (wi.dot(surfaceNormal) > 0) {
         surfaceNormal = surfaceNormal.inverted();
      }
      SamplingUtils.cosSampleHemisphere(wo.direction, rng);
      ShadingUtils.shadingCoordsToWorld(wo.direction, surfaceNormal, parameters.tangentVector);
      /*
       * Lo = (brdf(Ki, Ko) * Li * cos(theta)) / pdf
       * 
       * Using PDF (cosTheta / (Math.PI)), and BRDF == 1/pi * diffuse (perfect Lambertian diffusion), this reduces to
       * simply
       * 
       * Lo = diffuse * Li.
       * 
       * Li is handled in the Path-Tracer engine.
       */
      wo.sampleColor.set(diffuse.getColor(parameters.materialCoords));
      wo.emissiveResponse = false;
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
