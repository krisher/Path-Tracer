package edu.rit.krisher.scene.material;

import java.util.Random;

import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.MaterialInfo;
import edu.rit.krisher.vecmath.Vec3;

public class LambertBRDF implements Material, Cloneable {

   private Texture diffuse;

   public LambertBRDF(final Texture diffuse) {
      this.diffuse = diffuse;
   }

   @Override
   public void getEmissionColor(final Color emissionOut, final Vec3 sampleDirection, final MaterialInfo parameters) {
      emissionOut.set(0, 0, 0);
   }

   @Override
   public void evaluateBRDF(final Color radiance, final Vec3 sampleDirection, final Vec3 incidentLightDirection,
         final MaterialInfo parameters) {
      /*
       * BRDF = 1/pi * diffuse
       */
      final Color diffColor = this.diffuse.getColor(parameters.materialCoords);
      radiance.multiply(diffColor.r / Math.PI, diffColor.g / Math.PI, diffColor.b / Math.PI);
   }

   @Override
   public void sampleBRDF(final SampleRay sampleOut, final Random rng, final Vec3 wIncoming,
         final MaterialInfo parameters) {
      Vec3 surfaceNormal = parameters.surfaceNormal;
      if (wIncoming.dot(surfaceNormal) > 0) {
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
      directionOut.set(parameters.tangentVector);
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
       * i.e. we take fewer samples in directions that are more perpendicular to the surface normal.
       * 
       * Here we just return the spectral response (color), Li is handled in the
       * Path-Tracer engine.
       */
      sampleOut.sampleColor.set(diffuse.getColor(parameters.materialCoords));
      sampleOut.emissiveResponse = false;

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
