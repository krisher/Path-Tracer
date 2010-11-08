/**
 * 
 */
package edu.rit.krisher.raytracer;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import edu.rit.krisher.raytracer.image.ImageBuffer;
import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.raytracer.sampling.JitteredStratifiedRectangleSampling;
import edu.rit.krisher.raytracer.sampling.UnsafePRNG;
import edu.rit.krisher.scene.Camera;
import edu.rit.krisher.scene.EmissiveGeometry;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.GeometryIntersection;
import edu.rit.krisher.scene.MaterialInfo;
import edu.rit.krisher.scene.Scene;
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

   /**
    * Asynchronously ray traces the specified scene given the camera position and ImageBuffer to store the results in.
    * 
    * @param image
    *           A non-null ImageBuffer. The dimensions of the ray-traced image are determined from the
    *           {@link ImageBuffer#getResolution()} method synchronously with this call.
    * @param scene
    *           The non-null scene to render.
    * @param pixelSampleRate
    *           The linear super-sampling rate. This value squared is the actual number of paths traced for each image
    *           pixel. Must be greater than 0.
    * @param recursionDepth
    *           The maximum length of a ray path. 0 means trace eye rays and direct illumination only.
    */
   @Override
   public void integrate(final ImageBuffer image, final Scene scene, final int pixelSampleRate, final int recursionDepth) {

      /*
       * Imaging parameters
       */
      final Dimension imageSize = image.getResolution();
      /*
       * Estimate for the upper bound of the number of rays that will be generated, including one ray for each ray path
       * component, and shadow rays. The actual number of rays traced will likely be lower than this, some rays will be
       * absorbed before tracing to the maximum path length, some rays might not hit anything (for open scenes), etc.
       */
      final double rayCount = ((double) imageSize.width * imageSize.height * pixelSampleRate * pixelSampleRate
            * recursionDepth * (1.0 + scene.getLightSources().length));
      System.out.println("Max Samples: " + formatter.format(rayCount));
      /*
       * WorkItems may begin processing as soon as they are queued, so notify the ImageBuffer that pixels are on their
       * way...
       */
      timer.reset();
      image.imagingStarted();

      /*
       * Tiled work distribution...
       */
      final Rectangle[] imageChunks = chunkRectangle(imageSize.width, imageSize.height, BLOCK_SIZE);
      /*
       * Thread-safe spin-lock based countdown latch to monitor progress for this image. When this reaches 0, the
       * ImageBuffer is notified that the rendering is complete.
       */
      final AtomicInteger doneSignal = new AtomicInteger(imageChunks.length);
      for (final Rectangle chunk : imageChunks) {
         final ImageBlock block = new ImageBlock(image, scene, chunk.x, chunk.y, chunk.width, chunk.height, pixelSampleRate, recursionDepth, doneSignal);
         threadPool.submit();

      }

   }

   private static final class PathProcessor implements Runnable {
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
      private final JitteredStratifiedRectangleSampling pixelSampler = new JitteredStratifiedRectangleSampling(1, 1);

      /*
       * @see edu.rit.krisher.raytracer.RayIntegrator#integrate(edu.rit.krisher.raytracer.WorkItem)
       */
      @Override
      public void run() {
         try {
            final int pixelCount = workItem.blockWidth * workItem.blockHeight * 3;
            if (pixels == null || pixels.length < pixelCount)
               pixels = new float[pixelCount];
            else
               Arrays.fill(pixels, 0);

            final Dimension imageSize = workItem.image.getResolution();
            final double sampleWeight = 1.0 / (workItem.pixelSampleRate * workItem.pixelSampleRate);

            final SampleRay[] rays = new SampleRay[workItem.pixelSampleRate * workItem.pixelSampleRate
                                                   * workItem.blockWidth * workItem.blockHeight];

            pixelSampler.resize(workItem.pixelSampleRate, workItem.pixelSampleRate);
            final Camera camera = workItem.scene.getCamera();
            /*
             * Imaging ray generation.
             */
            int rayIdx = 0;
            for (int pixelY = 0; pixelY < workItem.blockHeight; pixelY++) {
               for (int pixelX = 0; pixelX < workItem.blockWidth; pixelX++) {
                  pixelSampler.generateSamples(rng);
                  for (int i = 0; i < workItem.pixelSampleRate * workItem.pixelSampleRate; ++i) {
                     final SampleRay ray = new SampleRay(sampleWeight);
                     ray.emissiveResponse = true;
                     ray.extinction.clear();
                     /*
                      * Eye rays transmit the emissive component of intersected objects (i.e. an emissive object is
                      * directly visible)
                      */


                     ray.pixelX = workItem.blockStartX + pixelX + (double) pixelSampler.xSamples[i];
                     ray.pixelY = workItem.blockStartY + pixelY + (double) pixelSampler.ySamples[i];

                     rays[rayIdx] = ray;
                     ++rayIdx;

                     assert ((int) ray.pixelX) == pixelX + workItem.blockStartX : "Ray PixelX (" + ray.pixelX
                     + ") does not match expected (" + (pixelX + workItem.blockStartX) + ") -- J: "
                     + pixelSampler.xSamples[i];
                     assert ((int) ray.pixelY) == (pixelY + workItem.blockStartY) : "Ray PixelY (" + ray.pixelY
                     + ") does not match expected (" + (pixelY + workItem.blockStartY) + ") -- J: "
                     + pixelSampler.ySamples[i];
                  }

               }
            }
            camera.sample(rays, imageSize.width, imageSize.height, rng);

            processRays(workItem, rays);

            /*
             * Out of rays, push pixels back into the image...
             */
            workItem.image.setPixels(workItem.blockStartX, workItem.blockStartY, workItem.blockWidth, workItem.blockHeight, pixels);
         } finally {
            workItem.workDone();
         }
      }

      private final void processRays(final ImageBlock workItem, final SampleRay[] rays) {

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
          * we reach the maximum depth, or all rays have terminated.
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
                  final int dst = 3 * (((int) ray.pixelY - workItem.blockStartY) * workItem.blockWidth
                        + (int) ray.pixelX - workItem.blockStartX);
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
                * respond to light coming from directions that will be sampled via bounce rays.
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
               final int dst = 3 * (((int) ray.pixelY - workItem.blockStartY) * workItem.blockWidth + (int) ray.pixelX - workItem.blockStartX);
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