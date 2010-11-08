/**
 * 
 */
package edu.rit.krisher.raytracer;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import edu.rit.krisher.raytracer.image.ImageBuffer;
import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.raytracer.sampling.UnsafePRNG;
import edu.rit.krisher.scene.Camera;
import edu.rit.krisher.scene.EmissiveGeometry;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.GeometryIntersection;
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
public final class PathTracer extends IntegratorUtils {

   private static final Map<ImageBuffer, AtomicInteger> active = new ConcurrentHashMap<ImageBuffer, AtomicInteger>();

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
      final ConcurrentLinkedQueue<Rectangle> blocks = new ConcurrentLinkedQueue<Rectangle>();
      for (final Rectangle chunk : imageChunks) {
         blocks.add(chunk);
      }
      active.put(image, doneSignal);
      for (int i = 0; i < threads; i++)
         threadPool.submit(new PathProcessor(scene, image, blocks, pixelSampleRate, recursionDepth, doneSignal));
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
      private final int pixelSampleRate;
      private final int recursionDepth;
      private final ImageBuffer imageBuffer;
      private final Scene scene;
      private final Queue<Rectangle> workQueue;
      private final AtomicInteger doneSignal;

      public PathProcessor(final Scene scene, final ImageBuffer image, final Queue<Rectangle> workQueue,
            final int pixelSampleRate, final int recursionDepth, final AtomicInteger doneSignal) {
         this.pixelSampleRate = pixelSampleRate;
         this.recursionDepth = recursionDepth;
         this.imageBuffer = image;
         this.scene = scene;
         this.doneSignal = doneSignal;
         this.workQueue = workQueue;
      }

      /*
       * @see edu.rit.krisher.raytracer.RayIntegrator#integrate(edu.rit.krisher.raytracer.WorkItem)
       */
      @Override
      public void run() {
         final Dimension imageSize = imageBuffer.getResolution();
         final double sampleWeight = 1.0 / (pixelSampleRate * pixelSampleRate);

         Rectangle rect;
         while ((rect = workQueue.poll()) != null) {
            try {
               final int pixelCount = rect.width * rect.height * 3;
               if (pixels == null || pixels.length < pixelCount)
                  pixels = new float[pixelCount];
               else
                  Arrays.fill(pixels, 0);

               final SampleRay[] rays = new SampleRay[pixelSampleRate * pixelSampleRate * rect.width * rect.height];
               for (int rayIdx = 0; rayIdx < rays.length; ++rayIdx) {
                  rays[rayIdx] = new SampleRay(sampleWeight);
               }

               /* Generate Eye Rays */
               IntegratorUtils.generatePixelSamples(rays, rect, pixelSampleRate, rng);
               scene.getCamera().sample(rays, imageSize.width, imageSize.height, rng);

               /* Trace Rays */
               processRays(rect, rays);

               /* Put results back into image buffer */
               imageBuffer.setPixels(rect.x, rect.y, rect.width, rect.height, pixels);
            } catch (final Exception ex) {
               ex.printStackTrace();
            } finally {
               final int remaining = doneSignal.decrementAndGet();
               if (remaining == 0) {
                  imageBuffer.imagingDone();
                  active.remove(imageBuffer);
                  return;
               } else if (remaining < 0) {
                  return; // Process was cancelled.
               }
            }
         }
      }

      private final void processRays(final Rectangle rect, final SampleRay[] rays) {

         final Color sampleColor = new Color(0, 0, 0);

         final Geometry[] geometry = scene.getGeometry();
         final EmissiveGeometry[] lights = scene.getLightSources();

         final Color bg = scene.getBackground();
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
         for (int rayDepth = 0; rayDepth <= recursionDepth && activeRayCount > 0; rayDepth++) {
            /* Process all active rays for intersection with scene geometry */
            processIntersections(rays, activeRayCount, geometry);
            /*
             * Number of rays that will be processed in the next iteration
             */
            int outRayCount = 0;
            for (int processRayIdx = 0; processRayIdx < activeRayCount; processRayIdx++) {
               final SampleRay ray = rays[processRayIdx];
               if (ray.intersection.hitGeometry == null) {
                  /*
                   * No intersection, process for the scene background color.
                   * 
                   * Ignore the emissive response flag since the background will not be sampled for direct illumination.
                   */
                  // if (ray.emissiveResponse) {
                  final int dst = 3 * (((int) ray.pixelY - rect.y) * rect.width + (int) ray.pixelX - rect.x);
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
                * Populate the intersection data...
                */
               ray.intersection.hitGeometry.getHitData(ray.intersection, ray.intersection.primitiveID, ray, ray.intersection.t);

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
                  ray.intersection.material.getEmissionColor(sampleColor, ray.direction, ray.intersection);
               } else
                  sampleColor.set(0, 0, 0);

               /*
                * Save the transmission weights, they may be overwritten if the ray is reused for the next path segment
                * below.
                */
               final double rTransmission = ray.sampleColor.r
                     * (ray.extinction.r == 0.0 ? 1.0 : Math.exp(Math.log(ray.extinction.r) * ray.intersection.t));
               final double gTransmission = ray.sampleColor.g
                     * (ray.extinction.g == 0.0 ? 1.0 : Math.exp(Math.log(ray.extinction.g) * ray.intersection.t));
               final double bTransmission = ray.sampleColor.b
                     * (ray.extinction.b == 0.0 ? 1.0 : Math.exp(Math.log(ray.extinction.b) * ray.intersection.t));

               /*
                * Specular and refractive materials do not benefit from direct illuminant sampling, since their
                * relfection/refraction distributions are typically constrained to a small solid angle, they only
                * respond to light coming from directions that will be sampled via bounce rays.
                */
               if (ray.intersection.material.isDiffuse()) {
                  integrateDirectIllumination(sampleColor, geometry, lights, ray, rng);
               }

               /*
                * If we have not reached the maximum recursion depth, generate a new ray for the next path segment.
                */
               if (rayDepth < recursionDepth
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
                  irradianceRay.origin.set(ray.getPointOnRay(ray.intersection.t));
                  irradianceRay.reset();
                  ray.intersection.material.sampleBRDF(irradianceRay, ray.direction, ray.intersection, rng);
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
               final int dst = 3 * (((int) ray.pixelY - rect.y) * rect.width + (int) ray.pixelX - rect.x);
               pixels[dst] += (sampleColor.r) * rTransmission;
               pixels[dst + 1] += (sampleColor.g) * gTransmission;
               pixels[dst + 2] += (sampleColor.b) * bTransmission;
            }
            activeRayCount = outRayCount;
         }
      }
   }

   private static final void integrateDirectIllumination(final Color irradianceOut, final Geometry[] geometry,
         final EmissiveGeometry[] lights, final SampleRay woRay, final Random rng) {
      final Color lightEnergy = new Color(0, 0, 0);
      final GeometryIntersection isect = new GeometryIntersection();
      /*
       * Set the origin of the shadow ray to the hit point, but perturb by a small distance along the surface normal
       * vector to avoid self-intersecting the same point due to round-off error.
       */
      final Ray lightSourceExitantRadianceRay = new Ray(woRay.getPointOnRay(woRay.intersection.t).scaleAdd(woRay.intersection.surfaceNormal, Constants.EPSILON_D), new Vec3());

      for (final EmissiveGeometry light : lights) {
         /*
          * Generate a random sample direction that hits the light
          */
         final double lightDist = light.sampleEmissiveRadiance(lightSourceExitantRadianceRay.direction, lightEnergy, lightSourceExitantRadianceRay.origin, rng);
         /*
          * Cosine of the angle between the geometry surface normal and the shadow ray direction
          */
         final double cosWi = lightSourceExitantRadianceRay.direction.dot(woRay.intersection.surfaceNormal);
         if (cosWi > 0) {
            /*
             * Determine whether the light source is visible from the irradiated point
             */
            isect.hitGeometry = light;
            isect.t = lightDist;
            for (final Geometry geom : geometry) {
               if (geom != light) {
                  if (geom.intersects(lightSourceExitantRadianceRay, isect))
                     break;
               }
            }
            if (isect.hitGeometry == light) {
               /*
                * Compute the reflected spectrum/power by modulating the energy transmitted along the shadow ray with
                * the response of the material...
                */
               woRay.intersection.material.evaluateBRDF(lightEnergy, woRay.direction, lightSourceExitantRadianceRay.direction, woRay.intersection);

               final double diffAngle = (cosWi) / (lightDist * lightDist);
               irradianceOut.scaleAdd(lightEnergy.r, lightEnergy.g, lightEnergy.b, diffAngle);
            }
         }

      }

   }

   /**
    * Cancels rendering for the specified ImageBuffer (that was previously passed to
    * {@link #integrate(ImageBuffer, Camera, Scene, int, int)}).
    * 
    * <p>
    * Any non-started work items are removed from the work queue, but work items already being processed are allowed to
    * finish. Pixel data may still be sent to the specified ImageBuffer until its {@link ImageBuffer#imagingDone()}
    * method is called.
    * 
    * @param target
    */
   @Override
   public void cancel(final ImageBuffer target) {
      final AtomicInteger remaining = active.get(target);
      if (remaining != null) {
         final int prevRemaining = remaining.getAndSet(0);
         if (prevRemaining != 0) {
            active.remove(target);
            target.imagingDone();
         }
      }
   }

}