package edu.rit.krisher.scene.material;

import java.util.Random;

import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.vecmath.Vec3;

public class LambertBRDF implements Material, Cloneable {

   private Texture diffuse;

   public LambertBRDF(Texture diffuse) {
      this.diffuse = diffuse;
   }

   @Override
   public void getEmissionColor(Color emissionOut, Vec3 sampleDirection, Vec3 surfaceNormal, double... materialCoords) {
      emissionOut.set(0, 0, 0);
   }

   @Override
   public void getDirectIlluminationTransport(Color radiance, Vec3 sampleDirection, Random rng,
         Vec3 incidentLightDirection, Vec3 surfaceNormal, double... materialCoords) {
      /*
       * BRDF = 1/pi * diffuse
       */
      radiance.scaleSet(this.diffuse.getColor(materialCoords), 1.0 / Math.PI);
   }

   @Override
   public void sampleIrradiance(SampleRay sampleOut, Random rng, Vec3 radianceSampleDirection, Vec3 surfaceNormal,
         double... materialCoords) {
      if (radianceSampleDirection.dot(surfaceNormal) > 0) {
         surfaceNormal = surfaceNormal.inverted();
      }

      /*
       * Cosine-weighted sampling about the surface normal:
       * 
       * Probability of direction Ko = 1/pi * cos(theta) where theta is the
       * angle between the surface normal and Ko.
       * 
       * The polar angle about the normal is chosen from a uniform distribution
       * 0..2pi
       */
      final double cosTheta = Math.sqrt(1.0 - rng.nextDouble());
      final double sinTheta = Math.sqrt(1.0 - cosTheta * cosTheta);
      final double phi = 2.0 * Math.PI * rng.nextDouble();
      final double xb = sinTheta * Math.cos(phi);
      final double yb = sinTheta * Math.sin(phi);

      final Vec3 directionOut = sampleOut.direction;
      /*
       * Construct orthonormal basis with vectors:
       * 
       * surfaceNormal, directionOut, nv
       */
      directionOut.set(0, 1, 0);
      if (Math.abs(directionOut.dot(surfaceNormal)) > 0.9) {
         // Small angle, pick a better vector...
         directionOut.x = -1.0;
         directionOut.y = 0;
      }
      directionOut.cross(surfaceNormal).normalize();
      final Vec3 nv = new Vec3(surfaceNormal).cross(directionOut);
      /*
       * Use the x,y,z values calculated above as coordinates in the ONB...
       */
      directionOut.multiply(xb).scaleAdd(surfaceNormal, cosTheta).scaleAdd(nv, yb);
      /*
       * Lo = (brdf(Ki, Ko) * Li * cos(theta)) / pdf
       * 
       * Using PDF above, and BRDF == 1/pi * diffuse (perfect Lambertian
       * diffusion), this reduces to simply
       * 
       * Lo = diffuse * Li.
       * 
       * Here we just return the spectral response (color), Li is handled in the
       * Path-Tracer engine.
       */
      sampleOut.transmissionSpectrum.set(diffuse.getColor(materialCoords));
      sampleOut.emissiveResponse = false;

   }

   public boolean isTranslucent() {
      return false;
   }

   public Texture getDiffuse() {
      return diffuse;
   }

   public void setDiffuse(Texture diffuse) {
      this.diffuse = diffuse;
   }

   public boolean shouldSampleDirectIllumination() {
      return true;
   }

}
