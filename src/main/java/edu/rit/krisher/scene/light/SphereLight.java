package edu.rit.krisher.scene.light;

import java.util.Random;

import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.raytracer.sampling.SamplingUtils;
import edu.rit.krisher.scene.EmissiveGeometry;
import edu.rit.krisher.scene.geometry.Sphere;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.vecmath.Vec3;

public final class SphereLight extends Sphere implements EmissiveGeometry {

   public SphereLight(final Vec3 center, final double radius, final Color material) {
      super(center, radius, material);
   }

   public SphereLight(final Vec3 center, final double radius, final Color emission, final double power) {
      super(center, radius, new Color(emission).multiply(power));
   }

   @Override
   public void sampleEmissiveRadiance(final SampleRay wo, final Random rng) {

      wo.direction.set(center).subtract(wo.origin);
      final double lightDist = wo.direction.length();
      /*
       * The maximum angle from originToCenter for a ray eminating from origin that will hit the sphere.
       */
      final double sinMaxAngle = radius / lightDist;
      final double cosMaxAngle = Math.sqrt(1 - sinMaxAngle * sinMaxAngle);

      /*
       * Uniform sample density over the solid angle subtended by the sphere wrt. the origin point. Taken from Shirley
       * and Morely book.
       * 
       * Basically generates a random polar coordinate in a disc perpendicular to the direction from sphere center to
       * ray origin. The coordinate is projected back onto the sphere surface to determine the ray direction.
       */
      final double cosRandomAzimuth = 1.0 + rng.nextDouble() * (cosMaxAngle - 1.0);
      final double sinRandomAzimuth = Math.sqrt(1.0 - cosRandomAzimuth * cosRandomAzimuth);

      final double randomPolar = 2.0 * Math.PI * rng.nextDouble();

      /*
       * Construct an orthonormal basis around the direction vector
       */
      wo.direction.multiply(1.0 / lightDist);
      final Vec3 nu = new Vec3();
      Vec3.computeTangentVector(nu, wo.direction);
      final Vec3 nv = new Vec3(wo.direction).cross(nu);

      /*
       * Make wo.direction point to a location on the surface of the sphere.
       */
      wo.direction.multiply(cosRandomAzimuth).scaleAdd(nu, Math.cos(randomPolar) * sinRandomAzimuth).scaleAdd(nv, Math.sin(randomPolar)
                                                                                                              * sinRandomAzimuth);

      final double isectDist = wo.intersectsSphere(center, radius);
      wo.intersection.surfaceNormal.set(wo.getPointOnRay(isectDist).subtract(center).multiply(1.0 / radius));
      wo.intersection.t = isectDist;
      wo.intersection.hitGeometry = this;

      material.getEmissionColor(wo.sampleColor, wo, null);
      /*
       * Multiply by the solid angle of the light sphere that is visible from the origin (due to self-occlusion).
       */
      wo.sampleColor.multiply(((2.0 * Math.PI * (1.0 - cosMaxAngle))));
   }

   @Override
   public int multisampleEmissiveRadiance(final SampleRay[] woSamples, final int woOffset, final int woCount,
         final Random rng) {
      for (int i = 0; i < woCount; ++i) {
         final SampleRay wo = woSamples[i + woOffset];
         wo.origin.set(center);
         SamplingUtils.uniformSampleSphere(wo.direction, rng); // Equal probability of sending a ray in any
         // direction.
         wo.origin.scaleAdd(wo.direction, radius); // Move the origin to the surface of the sphere.
         // TODO: this is treated as a point light here, once we have decided a position, must decide direction over the
         // hemisphere.
         material.getEmissionColor(wo.sampleColor, wo, null);
      }
      return woCount;
   }
}
