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
import edu.rit.krisher.raytracer.image.ImageUtil;
import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.raytracer.sampling.SamplingUtils;
import edu.rit.krisher.raytracer.sampling.UnsafePRNG;
import edu.rit.krisher.scene.EmissiveGeometry;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.Scene;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.util.Timer;
import edu.rit.krisher.vecmath.Constants;

/**
 * Non thread-safe Photon Tracer.
 * 
 * @author krisher
 * 
 */
public final class PhotonTracer implements SceneIntegrator {

   private final Timer timer = new Timer("Ray Trace (Thread Timing)");

   private static final Map<ImageBuffer, AtomicInteger> active = new ConcurrentHashMap<ImageBuffer, AtomicInteger>();

   /**
    * Creates a new path tracer.
    * 
    */
   public PhotonTracer() {
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
       * TODO: Compute photon map...
       * 
       * Sample all light sources for photon ray initialization.  Generate sample rays in every direction, with energy and count proportional to the total power of the light source.
       * 
       * Trace photons through the scene, deposit at each diffuse hit location.
       * 
       * Build KD-Tree from hits.
       * 
       * Ray Trace Scene until all rays hit a diffuse surface.
       * 
       * For all hits, compute direct illumination.
       * 
       * For all hits, generate rays that sample hemisphere.
       * 
       * For all sample ray hits, gather k nearest photons in photon map, weight with RBF, and contribute illumination to sample pixel.
       */
      final EmissiveGeometry[] lights = scene.getLightSources();
      final int targetPhotonCount = 10000;
      final int photonCount = 0;
//      while (photonCount < targetPhotonCount) {
//         //Balance based on average power of sample rays from each light source (normalized by number of photons).
//      }
      
      
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
      System.out.println("Max Samples: " + IntegratorUtils.formatter.format(rayCount));
      /*
       * WorkItems may begin processing as soon as they are queued, so notify the ImageBuffer that pixels are on their
       * way...
       */
      timer.reset();
      image.imagingStarted();

      /*
       * Tiled work distribution...
       */
      final Rectangle[] imageChunks = IntegratorUtils.chunkRectangle(imageSize.width, imageSize.height, Math.max(2, IntegratorUtils.DEFAULT_PIXEL_BLOCK_SIZE / pixelSampleRate));
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
      for (int i = 0; i < IntegratorUtils.threads; i++)
         IntegratorUtils.threadPool.submit(new PathProcessor(scene, image, blocks, pixelSampleRate, recursionDepth, doneSignal));
   }

   private static final class PathProcessor implements Runnable {
      /**
       * Overridden to remove thread safety overhead
       */
      private final Random rng = new UnsafePRNG();

      private final SampleRay illuminationRay = new SampleRay(0);

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
         SampleRay[] rays = new SampleRay[0];
         Rectangle rect;
         while ((rect = workQueue.poll()) != null) {
            try {
               final int pixelCount = rect.width * rect.height * 3;
               if (pixels == null || pixels.length < pixelCount)
                  pixels = new float[pixelCount];
               else
                  Arrays.fill(pixels, 0);

               final int rayCount = pixelSampleRate * pixelSampleRate * rect.width * rect.height;
               if (rays.length < rayCount) {
                  rays = new SampleRay[rayCount];
                  for (int rayIdx = 0; rayIdx < rayCount; ++rayIdx) {
                     rays[rayIdx] = new SampleRay(sampleWeight);
                  }
               } else {
                  for (int i = 0; i < rayCount; ++i) {
                     rays[i].throughput.set(sampleWeight);
                     rays[i].emissiveResponse = true;
                     rays[i].extinction.clear();
                  }
               }

               /* Generate Eye Rays */
               SamplingUtils.generatePixelSamples(rays, new Rectangle(0, 0, rect.width, rect.height), pixelSampleRate, rng);
               scene.getCamera().sample(rays, imageSize.width, imageSize.height, rect.x, rect.y, rng);

               /* Trace Rays */
               processRays(rect, rays, rays.length);

               /* Put results back into image buffer */
               imageBuffer.setPixels(rect.x, rect.y, rect.width, rect.height, pixels);
            } finally {
               final int remaining = doneSignal.decrementAndGet();
               if (remaining == 0) {
                  imageBuffer.imagingDone();
                  active.remove(imageBuffer);
                  return;
               } else if (remaining < 0) {
                  return; // Process was canceled.
               }
            }
         }
      }

      private final void processRays(final Rectangle rect, final SampleRay[] rays, int rayCount) {

         final Color sampleColor = new Color(0, 0, 0);

         final Geometry[] geometry = scene.getGeometry();
         final EmissiveGeometry[] lights = scene.getLightSources();

         final Color bg = scene.getBackground();

         /*
          * All active rays are at the same depth into the path (# of bounces from the initial eye ray). Process until
          * we reach the maximum depth, or all rays have terminated.
          */
         for (int rayDepth = 0; rayDepth <= recursionDepth && rayCount > 0; rayDepth++) {
            /* Process all active rays for intersection with scene geometry */
            IntegratorUtils.processIntersections(rays, rayCount, geometry);
            /*
             * Number of rays that will be processed in the next iteration
             */
            int outRayCount = 0;
            for (int processRayIdx = 0; processRayIdx < rayCount; processRayIdx++) {
               final SampleRay ray = rays[processRayIdx];
               if (ray.intersection.hitGeometry == null) {
                  /*
                   * No intersection, process for the scene background color.
                   */
                  final int dst = 3 * (((int) ray.pixelY) * rect.width + (int) ray.pixelX);
                  pixels[dst] += bg.r * ray.throughput.r;
                  pixels[dst + 1] += bg.g * ray.throughput.g;
                  pixels[dst + 2] += bg.b * ray.throughput.b;
                  /*
                   * This path is terminated.
                   */
                  continue;
               }

               /*
                * Populate the intersection data...
                */
               ray.intersection.hitGeometry.getHitData(ray, ray.intersection);

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
                  ray.intersection.material.getEmissionColor(sampleColor, ray, ray.intersection);
               } else
                  sampleColor.set(0, 0, 0);

               /*
                * Save the transmission weights, they may be overwritten if the ray is reused for the next path segment
                * below.
                */
               final double rTransmission = ray.throughput.r
               * (ray.extinction.r == 0.0 ? 1.0 : Math.exp(Math.log(ray.extinction.r) * ray.intersection.t));
               final double gTransmission = ray.throughput.g
               * (ray.extinction.g == 0.0 ? 1.0 : Math.exp(Math.log(ray.extinction.g) * ray.intersection.t));
               final double bTransmission = ray.throughput.b
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
                * If we have not reached the maximum recursion depth, generate a new reflection/refraction ray for the
                * next path segment.
                */
               if (rayDepth < recursionDepth
                     && (rayDepth < 2 || rng.nextFloat() >= Math.min(0.2, 1.0 - ImageUtil.luminance((float) rTransmission, (float) gTransmission, (float) bTransmission)))) {
                  final SampleRay bounceRay = rays[outRayCount];
                  /*
                   * Preserve the current extinction, this is only modified when the ray passes through a refractive
                   * interface, at which point the extinction is changed in the Material model.
                   */
                  bounceRay.extinction.set(ray.extinction);
                  bounceRay.origin.set(ray.getPointOnRay(ray.intersection.t));
                  bounceRay.reset();
                  ray.intersection.material.sampleBRDF(bounceRay, ray.direction, ray.intersection, rng);
                  if (!bounceRay.throughput.isZero()) {

                     // Scale transmission by inverse probability of reaching this depth due to RR.
                     if (rayDepth >= 2)
                        bounceRay.throughput.multiply(1 / (1 - Math.min(0.2, 1.0 - ImageUtil.luminance((float) rTransmission, (float) gTransmission, (float) bTransmission))));
                     bounceRay.throughput.multiply(rTransmission, gTransmission, bTransmission);

                     bounceRay.pixelX = ray.pixelX;
                     bounceRay.pixelY = ray.pixelY;
                     /*
                      * Avoid precision issues when processing the ray for the next intersection.
                      */
                     bounceRay.origin.scaleAdd(bounceRay.direction, Constants.EPSILON_D);
                     ++outRayCount;

                  }
               }
               /*
                * Add the contribution to the pixel, modulated by the transmission across all previous bounces in this
                * path.
                */
               final int dst = 3 * (((int) ray.pixelY) * rect.width + (int) ray.pixelX);
               pixels[dst] += (sampleColor.r) * rTransmission;
               pixels[dst + 1] += (sampleColor.g) * gTransmission;
               pixels[dst + 2] += (sampleColor.b) * bTransmission;
            }
            rayCount = outRayCount;
         }
      }

      private final void integrateDirectIllumination(final Color irradianceOut, final Geometry[] geometry,
            final EmissiveGeometry[] lights, final SampleRay woRay, final Random rng) {
         /*
          * Set the origin of the shadow ray to the hit point, but perturb by a small distance along the surface normal
          * vector to avoid self-intersecting the same point due to round-off error.
          */
         illuminationRay.origin.set(woRay.getPointOnRay(woRay.intersection.t).scaleAdd(woRay.intersection.surfaceNormal, Constants.EPSILON_D));

         for (final EmissiveGeometry light : lights) {
            /*
             * Generate a random sample direction that hits the light
             */
            light.sampleEmissiveRadiance(illuminationRay, rng);
            /*
             * Cosine of the angle between the geometry surface normal and the shadow ray direction
             */
            final double cosWi = illuminationRay.direction.dot(woRay.intersection.surfaceNormal);
            if (cosWi > 0) {
               /*
                * Determine whether the light source is visible from the irradiated point
                */
               for (final Geometry geom : geometry) {
                  if (geom != light) {
                     if (geom.intersects(illuminationRay, illuminationRay.intersection))
                        break;
                  }
               }
               if (illuminationRay.intersection.hitGeometry == light) {
                  /*
                   * Compute the reflected spectrum/power by modulating the energy transmitted along the shadow ray with
                   * the response of the material...
                   */
                  woRay.intersection.material.evaluateBRDF(illuminationRay.throughput, woRay.direction.inverted(), illuminationRay.direction, woRay.intersection);
                  irradianceOut.add(illuminationRay.throughput.r, illuminationRay.throughput.g, illuminationRay.throughput.b);
               }
            }

         }

      }
   }

   /**
    * Cancels rendering for the specified ImageBuffer (that was previously passed to
    * {@link #integrate(ImageBuffer, Scene, int, int)}).
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