/**
 * 
 */
package edu.rit.krisher.raytracer;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.Random;

import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.raytracer.sampling.JitteredStratifiedRectangleSampling;
import edu.rit.krisher.raytracer.sampling.UnsafePRNG;
import edu.rit.krisher.scene.EmissiveGeometry;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.GeometryIntersection;
import edu.rit.krisher.scene.MaterialInfo;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.vecmath.Constants;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

/**
 * Non thread-safe Path Tracer.
 * 
 * @author krisher
 * 
 */
public final class PathTracer extends ThreadedIntegrator {

   /**
    * Creates a new path tracer.
    * 
    */
   public PathTracer() {
      super();
   }
   
   

   /* 
    * @see edu.rit.krisher.raytracer.ThreadedIntegrator#createProcessors(int)
    */
   @Override
   protected WorkItemProcessor[] createProcessors(final int count) {
      final WorkItemProcessor[] processors = new WorkItemProcessor[count];
      for (int i=0; i < count; ++i) {
         processors[i] = new PathProcessor();
      }
      return processors;
   }



   private static final class PathProcessor implements WorkItemProcessor {
      /**
       * Overridden to remove thread safety overhead
       */
      private final Random rng = new UnsafePRNG();

      /*
       * Buffer to collect rgb pixel data
       * 
       * Values will always be >= 0, but are unbounded in magnitude.
       */
      private float[] pixels;
      private final MaterialInfo shadingInfo = new MaterialInfo();
      private final JitteredStratifiedRectangleSampling pixelSampler = new JitteredStratifiedRectangleSampling(1);

      /*
       * @see edu.rit.krisher.raytracer.RayIntegrator#integrate(edu.rit.krisher.raytracer.WorkItem)
       */
      @Override
      public void integrate(final WorkItem workItem) {
         try {
            final int pixelCount = workItem.blockWidth * workItem.blockHeight * 3;
            if (pixels == null || pixels.length < pixelCount)
               pixels = new float[pixelCount];
            else
               Arrays.fill(pixels, 0);

            final Dimension imageSize = workItem.image.getResolution();
            final double sampleWeight = 1.0 / (workItem.pixelSampleRate * workItem.pixelSampleRate);

            final SampleRay[] rays = new SampleRay[workItem.pixelSampleRate * workItem.pixelSampleRate];
            int rayIdx = 0;
            for (int sampleX = 0; sampleX < workItem.pixelSampleRate; sampleX++) {
               for (int sampleY = 0; sampleY < workItem.pixelSampleRate; sampleY++) {
                  /*
                   * Use of 1/<samples-per-pixel> as the sample weight effectively applies a single-pixel wide box
                   * filter
                   * to average the samples. It may be benificial to try other filters...
                   */
                  rays[rayIdx++] = new SampleRay(sampleWeight);
               }
            }
            pixelSampler.resize(workItem.pixelSampleRate);
            /*
             * Eye ray generation state
             */
            for (int pixelY = 0; pixelY < workItem.blockHeight; pixelY++) {
               for (int pixelX = 0; pixelX < workItem.blockWidth; pixelX++) {
                  rayIdx = 0;
                  pixelSampler.generateSamples(rng);
                  for (int sampleX = 0; sampleX < workItem.pixelSampleRate; sampleX++) {
                     for (int sampleY = 0; sampleY < workItem.pixelSampleRate; sampleY++) {
                        final SampleRay ray = rays[rayIdx];
                        /*
                         * Reset the ray state since we are probably re-using each instance many times.
                         */
                        ray.pixelX = pixelX;
                        ray.pixelY = pixelY;
                        /*
                         * The contribution of the path is bounded by 1 / (samples per pixel)
                         */
                        ray.sampleColor.set(sampleWeight, sampleWeight, sampleWeight);
                        /*
                         * Eye rays transmit the emissive component of intersected objects (i.e. an emissive object is
                         * directly visible)
                         */
                        ray.emissiveResponse = true;
                        ray.extinction.clear();
                        final double x = 2.0 * (workItem.blockStartX + pixelX + pixelSampler.xSamples[rayIdx])
                              / imageSize.width - 1.0;
                        final double y = 2.0 * (workItem.blockStartY + pixelY + pixelSampler.ySamples[rayIdx])
                              / imageSize.height - 1.0;
                        workItem.camera.sample(ray, x, y, rng);
                        ++rayIdx;
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
         }
      }

      private final void processRays(final WorkItem workItem, final SampleRay[] rays) {

         final Color sampleColor = new Color(0, 0, 0);

         final Geometry[] geometry = workItem.scene.getGeometry();
         final EmissiveGeometry[] lights = workItem.scene.getLightSources();

         final Color bg = workItem.scene.getBackground();
         /*
          * The number of rays that are still active (have not been absorbed or otherwise terminated).
          * 
          * The active rays are always contiguous from the beginning of the rays array.
          */
         int activeRayCount = rays.length;
         /*
          * All active rays are at the same depth into the path (# of bounces from the initial eye ray). Process until
          * we
          * reach the maximum depth, or all rays have terminated.
          */
         for (int rayDepth = 0; rayDepth <= workItem.recursionDepth && activeRayCount > 0; rayDepth++) {
            /*
             * Number of rays that will be processed in the next iteration
             */
            int outRayCount = 0;
            for (int processRayIdx = 0; processRayIdx < activeRayCount; processRayIdx++) {
               final SampleRay ray = rays[processRayIdx];

               /*
                * Process the sample ray for the nearest intersection with scene geometry.
                */
               double intersectDist = Double.POSITIVE_INFINITY;
               Geometry hit = null;
               int hitPrimitive = -1;

               for (final Geometry geom : geometry) {
                  shadingInfo.primitiveID = -1;
                  final double d = geom.intersects(shadingInfo, ray, intersectDist);
                  if (d > 0 && d < intersectDist) {
                     intersectDist = d;
                     hit = geom;
                     hitPrimitive = shadingInfo.primitiveID;
                  }
               }
               if (intersectDist == Double.POSITIVE_INFINITY) {
                  /*
                   * No intersection, process for the scene background color.
                   * 
                   * Ignore the emissive response flag since the background will not be sampled for direct illumination.
                   */
                  // if (ray.emissiveResponse) {
                  final int dst = 3 * (ray.pixelY * workItem.blockWidth + ray.pixelX);
                  pixels[dst] += bg.r * ray.sampleColor.r;
                  pixels[dst + 1] += bg.g * ray.sampleColor.g;
                  pixels[dst + 2] += bg.b * ray.sampleColor.b;
                  // }
                  /*
                   * This path is terminated.
                   */
                  continue;
               }

               /*
                * The intersection point. Saved because this data may be overwritten when generating the next path
                * segment.
                */
               ray.getPointOnRay(shadingInfo.hitLocation, intersectDist);
               hit.getHitData(shadingInfo, hitPrimitive, ray, intersectDist);

               /*
                * Diffuse surfaces with a wide distribution of reflectivity are relatively unlikely to bounce to a small
                * emissive object, which introduces significant variance without an extremely large number of samples.
                * Diffuse surfaces will be tested for direct illumination explicitly below, so we ignore the emissive
                * component of an object reflecting off a diffuse surface.
                * 
                * For more information, see:
                * 
                * ﻿Shirley, Peter, Changyaw Wang, and Kurt Zimmerman. 1996. Monte Carlo techniques for direct lighting
                * calculations. ACM Transactions on Graphics 15, no. 1: 1-36.
                * 
                * 
                * or
                * 
                * P. Shirley, R. Morley, Realistic Ray Tracing, 2nd Ed. 2003. AK Peters.
                */
               if (ray.emissiveResponse) {
                  shadingInfo.material.getEmissionColor(sampleColor, ray.direction, shadingInfo);
               } else
                  sampleColor.set(0, 0, 0);

               /*
                * Save the transmission weights, they may be overwritten if the ray is reused for the next path segment
                * below.
                */
               final double rTransmission = ray.sampleColor.r
                     * (ray.extinction.r == 0.0 ? 1.0 : Math.exp(Math.log(ray.extinction.r) * intersectDist));
               final double gTransmission = ray.sampleColor.g
                     * (ray.extinction.g == 0.0 ? 1.0 : Math.exp(Math.log(ray.extinction.g) * intersectDist));
               final double bTransmission = ray.sampleColor.b
                     * (ray.extinction.b == 0.0 ? 1.0 : Math.exp(Math.log(ray.extinction.b) * intersectDist));

               /*
                * Specular and refractive materials do not benefit from direct illuminant sampling, since their
                * relfection/refraction distributions are typically constrained to a small solid angle, they only
                * respond
                * to light coming from directions that will be sampled via bounce rays.
                */
               if (shadingInfo.material.isDiffuse()) {
                  integrateDirectIllumination(sampleColor, geometry, lights, ray, shadingInfo, rng);
               }

               /*
                * If we have not reached the maximum recursion depth, generate a new ray for the next path segment.
                */
               if (rayDepth < workItem.recursionDepth
               /*
                * Russian roulette for variance reduction.
                */
               && (rayDepth < 3 || rng.nextFloat() >= 1 / 6.0)) {
                  final SampleRay irradianceRay = rays[outRayCount];
                  /*
                   * Preserve the current extinction, this is only modified when the ray passes through a refractive
                   * interface, at which point the extinction is changed in the Material model.
                   */
                  irradianceRay.extinction.set(ray.extinction);
                  irradianceRay.origin.set(shadingInfo.hitLocation);
                  irradianceRay.reset();
                  shadingInfo.material.sampleBRDF(irradianceRay, rng, ray.direction, shadingInfo);
                  if (!irradianceRay.sampleColor.isZero()) {
                     // TODO: scale transmission by probability of reaching this depth due to RR.
                     irradianceRay.sampleColor.multiply(rTransmission, gTransmission, bTransmission);

                     irradianceRay.pixelX = ray.pixelX;
                     irradianceRay.pixelY = ray.pixelY;
                     /*
                      * Avoid precision issues when processing the ray for the next intersection.
                      */
                     irradianceRay.origin.scaleAdd(irradianceRay.direction, Constants.EPSILON_D);
                     ++outRayCount;

                  }
               }
               /*
                * Add the contribution to the pixel, modulated by the transmission across all previous bounces in this
                * path.
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

   private static final void integrateDirectIllumination(final Color irradianceOut, final Geometry[] geometry,
         final EmissiveGeometry[] lights, final SampleRay woRay, final MaterialInfo shadingInfo, final Random rng) {
      final Color lightEnergy = new Color(0, 0, 0);
      final GeometryIntersection isect = new GeometryIntersection();
      /*
       * Set the origin of the shadow ray to the hit point, but perturb by a small distance along the surface normal
       * vector to avoid self-intersecting the same point due to round-off error.
       */
      final Ray lightSourceExitantRadianceRay = new Ray(new Vec3(shadingInfo.hitLocation).scaleAdd(shadingInfo.surfaceNormal, Constants.EPSILON_D), new Vec3());

      for (final EmissiveGeometry light : lights) {
         /*
          * Generate a random sample direction that hits the light
          */
         double lightDist = light.sampleEmissiveRadiance(lightSourceExitantRadianceRay.direction, lightEnergy, lightSourceExitantRadianceRay.origin, rng);
         /*
          * Cosine of the angle between the geometry surface normal and the shadow ray direction
          */
         final double cosWi = lightSourceExitantRadianceRay.direction.dot(shadingInfo.surfaceNormal);
         if (cosWi > 0) {
            /*
             * Determine whether the light source is visible from the irradiated point
             */
            for (final Geometry geom : geometry) {
               if (geom != light) {
                  final double isectDist = geom.intersects(isect, lightSourceExitantRadianceRay, lightDist);
                  if (isectDist > 0 && isectDist < lightDist) {
                     lightDist = 0;
                     break;
                  }
               }
            }
            if (lightDist > 0) {
               /*
                * Compute the reflected spectrum/power by modulating the energy transmitted along the shadow ray with
                * the response of the material...
                */
               shadingInfo.material.evaluateBRDF(lightEnergy, woRay.direction, lightSourceExitantRadianceRay.direction, shadingInfo);

               final double diffAngle = (cosWi) / (lightDist * lightDist);
               irradianceOut.scaleAdd(lightEnergy.r, lightEnergy.g, lightEnergy.b, diffAngle);
            }
         }

      }

   }

}