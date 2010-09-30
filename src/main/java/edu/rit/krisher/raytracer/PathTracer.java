/**
 * 
 */
package edu.rit.krisher.raytracer;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.Random;

import edu.rit.krisher.raytracer.rays.HitData;
import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.scene.EmissiveGeometry;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.vecmath.Constants;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;
import edu.rit.krisher.util.Timer;

/**
 * Non thread-safe Path Tracer.
 * 
 * @author krisher
 * 
 */
public final class PathTracer {

   /**
    * Overridden to remove thread safety overhead
    */
   private final Random rng = new Random() {
      private long seed;
      private final static long multiplier = 0x5DEECE66DL;
      private final static long addend = 0xBL;
      private final static long mask = (1L << 48) - 1;

      @Override
      public void setSeed(final long seed) {
         this.seed = (seed ^ multiplier) & mask;
      }

      @Override
      protected int next(final int bits) {
         seed = (seed * multiplier + addend) & mask;
         return (int) (seed >>> (48 - bits));
      }
   };

    private static final Timer timer = new Timer("Path Tracing");

   /*
    * Buffer to collect rgb pixel data
    * 
    * Values will always be >= 0, but are unbounded in magnitude.
    */
   private float[] pixels;
   private final HitData hitData = new HitData();

   /**
    * Creates a new path tracer.
    * 
    */
   public PathTracer() {
      super();
   }

   /**
    * Generates eye rays for each sample of each pixel in the specified
    * WorkItem, and calls processRays to trace them.
    * 
    * {@link WorkItem#workDone()} is invoked before this method completes.
    * 
    * @param workItem
    *           The non-null item to path-trace.
    */
   public void pathTrace(final WorkItem workItem) {
       timer.start();
      try {
         final int pixelCount = workItem.blockWidth * workItem.blockHeight * 3;
         if (pixels == null || pixels.length < pixelCount)
            pixels = new float[pixelCount];
         else
            Arrays.fill(pixels, 0);

         final Dimension imageSize = workItem.image.getResolution();
         final float xCenter = imageSize.width / 2.0f;
         final float yCenter = imageSize.height / 2.0f;
         final double sampleWeight = 1.0 / (workItem.pixelSampleRate * workItem.pixelSampleRate);
         final double sampleDelta = 1.0 / workItem.pixelSampleRate;

         final SampleRay[] rays = new SampleRay[workItem.pixelSampleRate * workItem.pixelSampleRate];
         for (int i = 0; i < rays.length; i++) {
            rays[i] = new SampleRay(new Vec3(), new Vec3(), sampleWeight, 0, 0);
         }
         /*
          * Eye ray generation state
          */
         for (int pixelY = 0; pixelY < workItem.blockHeight; pixelY++) {
            for (int pixelX = 0; pixelX < workItem.blockWidth; pixelX++) {
               // TODO: Adaptive sampling...
               int rayIdx = 0;
               for (int sampleX = 0; sampleX < workItem.pixelSampleRate; sampleX++) {
                  for (int sampleY = 0; sampleY < workItem.pixelSampleRate; sampleY++) {
                     /*
                      * Stratified jittered sampling, an eye ray is generated
                      * that passes through a random location in a small square
                      * region of the pixel area for each sample.
                      * 
                      * This is not quite as good as a true Poisson
                      * distribution, but a reasonable approximation for this
                      * purpose.
                      */
                     final double xSRand = rng.nextDouble();
                     final double ySRand = rng.nextDouble();
                     /*
                      * Compute normalized image coordinates (-1 to 1 for the
                      * full range of the camera's FoV angle).
                      * 
                      * Note that both x and y are normalized based on the
                      * width, so the FoV represents a horizontal range.
                      */
                     final double x = ((pixelX + workItem.blockStartX + (sampleDelta * (sampleX + xSRand))) - xCenter)
                     / xCenter;
                     final double y = ((pixelY + workItem.blockStartY + (sampleDelta * (sampleY + ySRand))) - yCenter)
                     / xCenter;
                     final SampleRay ray = rays[rayIdx++];
                     /*
                      * Reset the ray state since we are probably re-using each
                      * instance many times.
                      */
                     ray.pixelX = pixelX;
                     ray.pixelY = pixelY;
                     /*
                      * The contribution of the path is bounded by 1 / (samples
                      * per pixel)
                      */
                     ray.transmissionSpectrum.set(sampleWeight, sampleWeight, sampleWeight);
                     /*
                      * Eye rays transmit the emissive component of intersected
                      * objects (i.e. an emissive object is directly visible)
                      */
                     ray.emissiveResponse = true;
                     ray.extinction.clear();
                     workItem.camera.initializeRay(ray, x, y, rng);
                  }
               }
               /*
                * Process all of the samples for the current pixel.
                */
               processRays(workItem, rays);
            }
         }

         /*
          * Out of rays, push pixels back into the image...
          */
         workItem.image.setPixels(workItem.blockStartX, workItem.blockStartY, workItem.blockWidth, workItem.blockHeight, pixels);
      } finally {
         workItem.workDone();
	 timer.stop();
	 timer.print(System.out);
      }
   }

   private final void processRays(final WorkItem workItem, final SampleRay[] rays) {

      final Color sampleColor = new Color(0, 0, 0);
      final Color lightResponse = new Color(0, 0, 0);
      final Color lightEnergy = new Color(0, 0, 0);

      final Geometry[] geometry = workItem.scene.getGeometry();
      final EmissiveGeometry[] lights = workItem.scene.getLightSources();
      final Ray shadowRay = new Ray(new Vec3(0, 0, 0), new Vec3(0, 0, 0));
      final Vec3 directLightNormal = new Vec3();

      /*
       * The number of rays that are still active (have not been absorbed or
       * otherwise terminated).
       * 
       * The active rays are always contiguous from the beginning of the rays
       * array.
       */
      int activeRayCount = rays.length;
      /*
       * All active rays are at the same depth into the path (# of bounces from
       * the initial eye ray). Process until we reach the maximum depth, or all
       * rays have terminated.
       */
      for (int rayDepth = 0; rayDepth <= workItem.recursionDepth && activeRayCount > 0; rayDepth++) {
         /*
          * Number of rays that will be processed in the next iteration
          */
         int outRayCount = 0;
         for (int processRayIdx = 0; processRayIdx < activeRayCount; processRayIdx++) {
            final SampleRay ray = rays[processRayIdx];

            /*
             * Process the sample ray for the nearest intersection with scene
             * geometry.
             */
            double intersectDist = 0;
            Geometry hit = null;

            for (final Geometry geom : geometry) {
               final double d = geom.intersects(ray);
               if (d > 0 && (intersectDist <= 0 || d < intersectDist)) {
                  intersectDist = d;
                  hit = geom;
               }
            }
            if (intersectDist <= 0) {
               /*
                * No intersection, process for the scene background color.
                * 
                * Ignore the emissive response flag since the background will
                * not be sampled for direct illumination.
                */
               // if (ray.emissiveResponse) {
               final int dst = 3 * (ray.pixelY * workItem.blockWidth + ray.pixelX);
               final Color bg = workItem.scene.getBackground();
               pixels[dst] += bg.r * ray.transmissionSpectrum.r;
               pixels[dst + 1] += bg.g * ray.transmissionSpectrum.g;
               pixels[dst + 2] += bg.b * ray.transmissionSpectrum.b;
               // }
               /*
                * This path is terminated.
                */
               continue;
            }
            hit.getHitData(hitData, ray, intersectDist);

            /*
             * Diffuse surfaces with a wide distribution of reflectivity are
             * relatively unlikely to bounce to a small emissive object, which
             * introduces significant variance without an extremely large number
             * of samples. Diffuse surfaces will be tested for direct
             * illumination explicitly below, so we ignore the emissive
             * component of an object reflecting off a diffuse surface.
             * 
             * For more information, see:
             * 
             * ﻿Shirley, Peter, Changyaw Wang, and Kurt Zimmerman. 1996. Monte
             * Carlo techniques for direct lighting calculations. ACM
             * Transactions on Graphics 15, no. 1: 1-36.
             * 
             * 
             * or
             * 
             * P. Shirley, R. Morley, Realistic Ray Tracing, 2nd Ed. 2003. AK
             * Peters.
             */
            if (ray.emissiveResponse) {

               hitData.material.getEmissionColor(sampleColor, ray.direction, hitData.surfaceNormal, hitData.materialCoords);
            } else
               sampleColor.set(0, 0, 0);

            /*
             * Save the transmission weights, they may be overwritten if the ray
             * is reused for the next path segment below.
             */
            final double rTransmission = ray.transmissionSpectrum.r
            * (ray.extinction.r == 0.0 ? 1.0 : Math.exp(Math.log(ray.extinction.r) * intersectDist));
            final double gTransmission = ray.transmissionSpectrum.g
            * (ray.extinction.g == 0.0 ? 1.0 : Math.exp(Math.log(ray.extinction.g) * intersectDist));
            final double bTransmission = ray.transmissionSpectrum.b
            * (ray.extinction.b == 0.0 ? 1.0 : Math.exp(Math.log(ray.extinction.b) * intersectDist));

            /*
             * The intersection point. Saved because this data may be
             * overwritten when generating the next path segment.
             */
            final Vec3 hitPoint = ray.getPointOnRay(intersectDist);

            /*
             * Specular and refractive materials do not benefit from direct
             * illuminant sampling, since their relfection/refraction
             * distributions are typically constrained to a small solid angle,
             * they only respond to light coming from directions that will be
             * sampled via bounce rays.
             */
            if (hitData.material.shouldSampleDirectIllumination()) {
               /*
                * Set the origin of the shadow ray to the hit point, but perturb
                * by a small distance along the surface normal vector to avoid
                * self-intersecting the same point due to round-off error.
                */
               shadowRay.origin.set(hitPoint).scaleAdd(hitData.surfaceNormal, Constants.EPSILON_D);

               for (final EmissiveGeometry light : lights) {
                  /*
                   * Generate a random sample direction that hits the light
                   */
                  double lightDist = light.sampleEmissiveRadiance(shadowRay.direction, lightEnergy, directLightNormal, shadowRay.origin, rng);
                  /*
                   * Cosine of the angle between the geometry surface normal and
                   * the shadow ray direction
                   */
                  final double cosWi = shadowRay.direction.dot(hitData.surfaceNormal);
                  if (cosWi > 0) {
                     /*
                      * Determine whether the light source is visible from the
                      * irradiated point
                      */
                     for (final Geometry geom : geometry) {
                        if (geom != light) {
                           final double t = geom.intersects(shadowRay);
                           if (t > 0 && t < lightDist) {
                              lightDist = 0;
                              break;
                           }
                        }
                     }
                     if (lightDist > 0) {
                        /*
                         * Cosine of the angle between the light sample point's
                         * normal and the shadow ray.
                         */
                        final double cosWo = -directLightNormal.dot(shadowRay.direction);

                        /*
                         * Compute the reflected spectrum/power by modulating
                         * the energy transmitted along the shadow ray with the
                         * response of the material...
                         */
                        hitData.material.getDirectIlluminationTransport(lightResponse, ray.direction, rng, shadowRay.direction, hitData.surfaceNormal, hitData.materialCoords);

                        final double diffAngle = (cosWi * cosWo) / (lightDist * lightDist);
                        sampleColor.scaleAdd(lightEnergy.r * lightResponse.r, lightEnergy.g * lightResponse.g, lightEnergy.b
                                             * lightResponse.b, diffAngle);
                     }
                  }

               }
            }

            /*
             * If we have not reached the maximum recursion depth, generate a
             * new ray for the next path segment.
             */
            if (rayDepth < workItem.recursionDepth
                  /*
                   * Russion roulette for variance
                   * reduction.
                   */
                  && rng.nextFloat() >= 1 / 6.0) {
               final SampleRay outRay = rays[outRayCount];
               /*
                * Preserve the current extinction, this is only modified when
                * the ray passes through a refractive interface, at which point
                * the extinction is changed in the Material model.
                */
               outRay.extinction.set(ray.extinction);
               outRay.origin.set(hitPoint);
               outRay.reset();
               hitData.material.sampleIrradiance(outRay, rng, new Vec3(ray.direction), hitData.surfaceNormal, hitData.materialCoords);
               if (!outRay.transmissionSpectrum.isZero()) {
                  outRay.transmissionSpectrum.multiply(rTransmission, gTransmission, bTransmission);

                  outRay.pixelX = ray.pixelX;
                  outRay.pixelY = ray.pixelY;
                  /*
                   * Avoid precision issues when processing the ray for the next
                   * intersection.
                   */
                  outRay.origin.scaleAdd(outRay.direction, Constants.EPSILON_D);
                  outRayCount++;

               }
            }
            /*
             * Add the contribution to the pixel, modulated by the transmission
             * across all previous bounces in this path.
             */
            final int dst = 3 * (ray.pixelY * workItem.blockWidth + ray.pixelX);
            pixels[dst] += (sampleColor.r) * rTransmission;
            pixels[dst + 1] += (sampleColor.g) * gTransmission;
            pixels[dst + 2] += (sampleColor.b) * bTransmission;
         }
         activeRayCount = outRayCount;
      }
   }

}