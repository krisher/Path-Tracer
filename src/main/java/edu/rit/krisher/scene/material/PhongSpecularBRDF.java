package edu.rit.krisher.scene.material;

import java.util.Random;

import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.MaterialInfo;
import edu.rit.krisher.vecmath.Vec3;

public class PhongSpecularBRDF implements Material {

   private Texture specular = new Color(1, 1, 1);

   /**
    * Controls distribution of specular reflection rays, large values => mirror
    * like, small values => more diffuse.
    */
   private float specExp;

   public PhongSpecularBRDF(final Texture spec, final float specExp) {
      this.specular = spec;
      this.specExp = specExp;
   }

   @Override
   public boolean shouldSampleDirectIllumination() {
      return false;
   }

   @Override
   public void getEmissionColor(final Color emissionOut, final Vec3 sampleDirection, final Vec3 surfaceNormal, final double... materialCoords) {
      emissionOut.set(0, 0, 0);
   }

   @Override
   public void getIrradianceResponse(final Color radiance, final Vec3 sampleDirection, final Random rng,
         final Vec3 incidentLightDirection, final MaterialInfo parameters) {

      /*
       * The specular model is not sampled for direct lighting because the light
       * will almost certainly be sampled for the directions that will produce a
       * non-zero result by the path tracer do to the high coherence of
       * reflection direction. Also, for highly specular surfaces (specExp is
       * large), combined with Monte Carlo sampling of area light sources, this
       * will not produce the desired results.
       */
      // final double cosLightNormalAngle =
      // Math.max(incidentLightDirection.dot(surfaceNormal), 0.0);
      // final Vec3 mirrorReflect = new Vec3(surfaceNormal).multiply(2 *
      // cosLightNormalAngle)
      // .subtract(incidentLightDirection);
      // double spec = -mirrorReflect.dot(sampleDirection);
      // if (spec > 0) {
      // radiance.scaleSet(this.specular.getColor(materialCoords),
      // Math.pow(spec, specExp));
      // } else
      radiance.clear();

      // radiance.set(0, 0, 0);
   }

   @Override
   public void sampleIrradiance(final SampleRay sampleOut, final Random rng, final Vec3 radianceSampleDirection, Vec3 surfaceNormal,
         final double... materialCoords) {
      if (radianceSampleDirection.dot(surfaceNormal) > 0) {
         // TODO: reflection at both interfaces of refractive material?
         // sampleOut.transmissionSpectrum.clear();
         surfaceNormal = surfaceNormal.inverted();
         // return;
      }

      /*
       * Compute the mirror reflection vector...
       */
      final Vec3 directionOut = sampleOut.direction;
      directionOut.set(radianceSampleDirection);
      directionOut.reflect(surfaceNormal);

      if (specExp < 100000) {
         /*
          * Exponential cosine weighted sampling about the mirror reflection
          * direction.
          * 
          * PDF = Modified Phong PDF = ( (n + 1) / 2pi ) * cos(a) ^ n
          * 
          * Where a is the angle between the output direction and the mirror
          * reflection vector.
          */
         final double cosA = Math.pow(rng.nextDouble(), 1 / (specExp + 1));

         /*
          * Generate another random value, uniform between 0 and 2pi, which is
          * the angle around the mirror reflection vector
          */
         final double phi = 2 * Math.PI * rng.nextDouble();
         final double sinTheta = Math.sqrt(1.0 - cosA * cosA);
         final double xb = Math.cos(phi) * sinTheta;
         final double yb = Math.sin(phi) * sinTheta;
         /*
          * Construct an ortho-normal basis using the reflection vector as one
          * axis, and arbitrary (perpendicular) vectors for the other two axes.
          * The orientation of the coordinate system about the reflection vector
          * is irrelevant since xb and yb are generated from a uniform random
          * variable.
          */
         final Vec3 u = new Vec3(0, 1.0, 0);
         final double cosAng = directionOut.dot(u);
         if (cosAng > 0.9 || cosAng < -0.9) {
            // Small angle, pick a better vector...
            u.x = -1.0;
            u.y = 0;
         }
         u.cross(directionOut).normalize();
         final Vec3 v = new Vec3(u).cross(directionOut);

         directionOut.multiply(cosA).scaleAdd(u, xb).scaleAdd(v, yb);
         if (directionOut.dot(surfaceNormal) < 0) {
            directionOut.scaleAdd(u, -2.0 * xb).scaleAdd(v, -2.0 * yb);
         }
      }
      sampleOut.transmissionSpectrum.set(specular.getColor(materialCoords));
      sampleOut.emissiveResponse = true;
   }

   public Texture getSpecular() {
      return specular;
   }

   public void setSpecular(final Texture specular) {
      this.specular = specular;
   }

   public float getSpecExp() {
      return specExp;
   }

   public void setSpecExp(final float specExp) {
      this.specExp = specExp;
   }
}
