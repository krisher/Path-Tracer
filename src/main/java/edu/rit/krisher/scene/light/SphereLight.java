package edu.rit.krisher.scene.light;

import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.raytracer.sampling.SamplingUtils;
import edu.rit.krisher.scene.EmissiveGeometry;
import edu.rit.krisher.scene.geometry.Sphere;
import edu.rit.krisher.scene.geometry.utils.ShadingUtils;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.vecmath.Constants;
import edu.rit.krisher.vecmath.Vec3;

public final class SphereLight extends Sphere implements EmissiveGeometry {

   public SphereLight(final Vec3 center, final double radius, final Color material) {
      super(center, radius, material);
   }

   public SphereLight(final Vec3 center, final double radius, final Color emission, final double power) {
      super(center, radius, new Color(emission).multiply(power));
   }

   @Override
   public void sampleIrradiance(final SampleRay wo, final Vec3 point, final float r1, final float r2) {
      final Vec3 pointToCenter = new Vec3(center).subtract(point);
      final double lightDistInv = 1.0 / pointToCenter.length();
      pointToCenter.multiply(lightDistInv);

      /*
       * The maximum angle from originToCenter for a ray eminating from origin that will hit the sphere.
       */
      final double sinMaxAngle = radius * lightDistInv;
      final double cosMaxAngle = Math.sqrt(1.0 - sinMaxAngle * sinMaxAngle);

      /*
       * Uniform sample density over the solid angle subtended by the sphere wrt. the origin point. Taken from Shirley
       * and Morely book.
       * 
       * Basically generates a random polar coordinate in a disc perpendicular to the direction from sphere center to
       * ray origin. The coordinate is projected back onto the sphere surface to determine the ray direction.
       */
      final double cosTheta = 1.0 + r1 * (cosMaxAngle - 1.0);
      final double sinTheta = Math.sqrt(1.0 - cosTheta * cosTheta);
      final double phi = 2.0 * Math.PI * r2;

      /*
       * Construct an orthonormal basis around the direction vector
       */
      final Vec3 tangentXAxis = Vec3.computePerpendicularVec(new Vec3(), pointToCenter);


      wo.direction.x = Math.cos(phi) * sinTheta;
      wo.direction.y = Math.sin(phi) * sinTheta;
      wo.direction.z = cosTheta;
      ShadingUtils.shadingCoordsToWorld(wo.direction, pointToCenter, tangentXAxis);

      // wo.intersection.surfaceNormal.set(wo.getPointOnRay(isectDist).subtract(center).multiply(1.0 / radius));
      wo.origin.set(point);
      wo.t = wo.intersectsSphere(center, radius);
      wo.hitGeometry = this;
      material.getEmissionColor(wo.throughput, wo, null);
      /*
       * Multiply by the solid angle of the light sphere that is visible from the origin (due to self-occlusion).
       */
      wo.throughput.multiply(((2.0 * Math.PI * (1.0 - cosMaxAngle))));

   }

   @Override
   public void sampleEmission(final SampleRay wo, final float r1, final float r2) {
      wo.origin.set(center);
      final double pdf = SamplingUtils.uniformSampleSphere(wo.direction, r1, r2); // Equal probability of sending a ray
      // in any
      // direction.
      wo.origin.scaleAdd(wo.direction, radius + Constants.EPSILON_D); // Move the origin to the surface of the
      // sphere.
      // TODO: this is treated as a point light here, once we have decided a position, must decide direction over the
      // hemisphere.
      // SamplingUtils.uniformSampleHemisphere(wo.direction, rng);
      material.getEmissionColor(wo.throughput, wo, null);
      wo.throughput.multiply(1.0 / pdf);
   }
}
