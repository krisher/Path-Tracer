package edu.rit.krisher.scene.material;

import java.util.Random;

import edu.rit.krisher.raytracer.rays.IntersectionInfo;
import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

public class RefractiveBRDF implements Material {

   private static final double inRef = 1.0; // TODO: the ray should carry the index of refraction with it...
   private final double refractiveIndex;
   private final Color transmissionFilter;
   private final double exp;

   public RefractiveBRDF(final double refractiveIndex, final Color opacity, final double blurExp) {
      this.refractiveIndex = refractiveIndex;
      this.transmissionFilter = opacity;
      /*
       * This handles 0 values OK, the results will be -INFINITY, which when
       * passed into Math.exp() produces 0.
       */
      // transmissionFilter.r = Math.log(transmissionFilter.r);
      // transmissionFilter.g = Math.log(transmissionFilter.g);
      // transmissionFilter.b = Math.log(transmissionFilter.b);
      // this.kt = kt;
      this.exp = blurExp;
   }

   @Override
   public void getEmissionColor(final Color emissionOut, final Ray sampleDirection, final IntersectionInfo parameters) {
      emissionOut.clear();
   }

   @Override
   public void evaluateBRDF(final Color colorOut, final Vec3 wo, final Vec3 wi,
         final IntersectionInfo parameters) {
      /*
       * Like Phong specular, do not compute direct illumination, this will be
       * handled by highly specular bounces from irradiance sampling.
       * 
       * Really only want to sample illumination at exit point if it is a
       * refractive sample... Also, need to compute the percentage of the light
       * that travels in the refraction direction
       */
      // final double sampleDotNormal = sampleDirection.dot(surfaceNormal);
      // if (sampleDirection.dot(surfaceNormal) > 0) {
      // final double rIdxRatio = refractiveIndex / inRef;
      // final double snellRoot = 1.0 - (rIdxRatio * rIdxRatio * (1.0 -
      // sampleDotNormal * sampleDotNormal));
      // if (snellRoot >= 0) {
      // colorOut.set(1,1,1);
      // }
      // }
      colorOut.clear();
   }

   @Override
   public boolean isDiffuse() {
      return false;
   }

   @Override
   public void sampleBRDF(final SampleRay sampleOut, final Vec3 wIncoming, final IntersectionInfo parameters,
         final Random rng) {
      final Vec3 sNormal = new Vec3(parameters.surfaceNormal);
      double cosSampleAndNormal = wIncoming.dot(sNormal);

      final double rIdxRatio;
      final boolean exiting;
      if (cosSampleAndNormal <= 0) {
         /*
          * Sample ray from outside the enclosed volume
          */
         rIdxRatio = inRef / refractiveIndex;
         cosSampleAndNormal = -cosSampleAndNormal;
         exiting = false;
      } else {
         /*
          * Sample ray from inside going out
          */
         rIdxRatio = refractiveIndex / inRef;
         sNormal.multiply(-1);
         exiting = true;
      }
      final double snellRoot = 1.0 - (rIdxRatio * rIdxRatio * (1.0 - cosSampleAndNormal * cosSampleAndNormal));
      if (snellRoot < 0) {
         /*
          * Total internal reflection
          */
         sampleOut.direction.set(wIncoming).reflect(sNormal);
         /*
          * TODO: adjust the transmission spectrum...
          */
         sampleOut.sampleColor.set(1, 1, 1);
      } else {
         /*
          * Refraction
          */
         sampleOut.direction.set(wIncoming).multiply(rIdxRatio);
         sampleOut.direction.scaleAdd(sNormal, (rIdxRatio * cosSampleAndNormal - Math.sqrt(snellRoot)));

         if (exp < 100000) {
            /*
             * Idential to phong, except we substitude the refraction direction
             * for the mirror reflection vector.
             */
            final double cosA = Math.pow(rng.nextDouble(), 1 / (exp + 1));

            /*
             * Generate another random value, uniform between 0 and 2pi, which
             * is the angle around the mirror reflection vector
             */
            final double phi = 2 * Math.PI * rng.nextDouble();
            final double sinTheta = Math.sqrt(1.0 - cosA * cosA);
            final double xb = Math.cos(phi) * sinTheta;
            final double yb = Math.sin(phi) * sinTheta;

            /*
             * Construct an ortho-normal basis using the reflection vector as
             * one axis, and arbitrary (perpendicular) vectors for the other two
             * axes. The orientation of the coordinate system about the
             * reflection vector is irrelevant since xb and yb are generated
             * from a uniform random variable.
             */
            final Vec3 u = new Vec3(0, 1.0, 0);
            final double cosAng = sampleOut.direction.dot(u);
            if (cosAng > 0.9 || cosAng < -0.9) {
               // Small angle, pick a better vector...
               u.x = -1.0;
               u.y = 0;
            }
            u.cross(sampleOut.direction).normalize();
            final Vec3 v = new Vec3(u).cross(sampleOut.direction);

            sampleOut.direction.multiply(cosA).scaleAdd(u, xb).scaleAdd(v, yb);
            if (sampleOut.direction.dot(parameters.surfaceNormal) < 0) {
               sampleOut.direction.scaleAdd(u, -2.0 * xb).scaleAdd(v, -2.0 * yb);
            }
         }

         sampleOut.sampleColor.set(1, 1, 1);
         if (!exiting)
            sampleOut.extinction.set(transmissionFilter);
         else
            sampleOut.extinction.clear();
      }

      sampleOut.emissiveResponse = true;
   }

}
