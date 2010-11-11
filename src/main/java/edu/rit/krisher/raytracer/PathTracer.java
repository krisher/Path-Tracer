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
import edu.rit.krisher.vecmath.Vec3;

/**
 * Non thread-safe Path Tracer.
 * 
 * @author krisher
 * 
 */
public final class PathTracer implements SceneIntegrator {

   private final Timer timer = new Timer("Ray Trace (Thread Timing)");

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
      final Rectangle[] imageChunks = IntegratorUtils.chunkRectangle(imageSize.width, imageSize.height, Math.max(2, IntegratorUtils.DEFAULT_PIXEL_BLOCK_SIZE
            / pixelSampleRate));
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
      private static final int ILLUMINATION_SAMPLES = 2;
      /**
       * Overridden to remove thread safety overhead
       */
      private final Random rng = new UnsafePRNG();

      private static final double gaussFalloffControl = 0.1;
      private static final double gaussFalloffConstant = Math.exp(-gaussFalloffControl * 0.25);


      private final int pixelSampleRate;
      private final int recursionDepth;
      private final ImageBuffer imageBuffer;
      private final Scene scene;
      private final Queue<Rectangle> workQueue;
      private final AtomicInteger doneSignal;
      private final SampleRay[] illuminationRays = new SampleRay[ILLUMINATION_SAMPLES];

      /*
       * Buffer to collect rgb pixel data
       * 
       * Values will always be >= 0, but are unbounded in magnitude.
       */
      private float[] pixels;
      private float[] pixelNormalization;
      private Rectangle rect;

      public PathProcessor(final Scene scene, final ImageBuffer image, final Queue<Rectangle> workQueue,
            final int pixelSampleRate, final int recursionDepth, final AtomicInteger doneSignal) {
         this.recursionDepth = recursionDepth;
         this.imageBuffer = image;
         this.scene = scene;
         this.doneSignal = doneSignal;
         this.workQueue = workQueue;
         this.pixelSampleRate = pixelSampleRate;
         for (int i=0; i < illuminationRays.length; ++i) {
            illuminationRays[i] = new SampleRay(1.0);
         }
      }

      /*
       * @see edu.rit.krisher.raytracer.RayIntegrator#integrate(edu.rit.krisher.raytracer.WorkItem)
       */
      @Override
      public void run() {
         final Dimension imageSize = imageBuffer.getResolution();
         SampleRay[] rays = new SampleRay[0];
         while ((rect = workQueue.poll()) != null) {
            try {
               final int pixelCount = rect.width * rect.height * 3;
               if (pixels == null || pixels.length < pixelCount) {
                  pixels = new float[pixelCount];
                  pixelNormalization = new float[pixelCount / 3];
               } else {
                  Arrays.fill(pixels, 0);
                  Arrays.fill(pixelNormalization, 0);
               }

               final int rayCount = pixelSampleRate * pixelSampleRate * rect.width * rect.height;
               if (rays.length < rayCount) {
                  rays = new SampleRay[rayCount];
                  for (int rayIdx = 0; rayIdx < rayCount; ++rayIdx) {
                     rays[rayIdx] = new SampleRay(1);
                  }
               } else {
                  for (int i = 0; i < rayCount; ++i) {
                     rays[i].throughput.set(1);
                     rays[i].emissiveResponse = true;
                     rays[i].extinction.clear();
                  }
               }

               /* Generate Eye Rays */
               SamplingUtils.generatePixelSamples(rays, new Rectangle(0, 0, rect.width, rect.height), pixelSampleRate, rng);
               scene.getCamera().sample(rays, imageSize.width, imageSize.height, rect.x, rect.y, rng);

               /* Visibility pass */
               IntegratorUtils.processHits(rays, rayCount, scene.getGeometry());

               /* Trace Rays */
               processRays(rect, rays, rays.length);

               /* Put results back into image buffer */
               for (int i=0; i < pixelNormalization.length; ++i) {
                  final int pixOffs = 3 * i;
                  pixels[pixOffs] /= pixelNormalization[i];
                  pixels[pixOffs+1] /= pixelNormalization[i];
                  pixels[pixOffs+2] /= pixelNormalization[i];
               }
               imageBuffer.setPixels(rect.x, rect.y, rect.width, rect.height, pixels);
            } catch (final Throwable e) {
               e.printStackTrace();
            }finally {
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

      private final void updateImage( double x,  double y, final double r, final double g, final double b) {
         final int dst = 3 * (((int) y) * rect.width + (int) x);
         x = x - (int)x - 0.5;
         y = y - (int)y - 0.5;
         
         final double filter = 1;//Math.max(0, Math.exp(-gaussFalloffControl * x * x) - gaussFalloffConstant) * Math.max(0, Math.exp(-gaussFalloffControl * y * y) - gaussFalloffConstant);
         pixels[dst] += r * filter;
         pixels[dst + 1] += g * filter;
         pixels[dst + 2] += b * filter;
         pixelNormalization[dst/3] += filter;
      }

      private final void processRays(final Rectangle rect, final SampleRay[] rays, int rayCount) {

         final Color directIllumContribution = new Color(0, 0, 0);

         final Geometry[] geometry = scene.getGeometry();
         final EmissiveGeometry[] lights = scene.getLightSources();

         final Color bg = scene.getBackground();

         /*
          * All active rays are at the same depth into the path (# of bounces from the initial eye ray). Process until
          * we reach the maximum depth, or all rays have terminated.
          */
         for (int rayDepth = 0; rayDepth <= recursionDepth && rayCount > 0; rayDepth++) {

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
                  updateImage(ray.pixelX, ray.pixelY, bg.r * ray.throughput.r, bg.g * ray.throughput.g, bg.b
                        * ray.throughput.b);
                  /*
                   * This path is terminated.
                   */
                  continue;
               }

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
                  ray.intersection.material.getEmissionColor(directIllumContribution, ray, ray.intersection);
               } else
                  directIllumContribution.set(0, 0, 0);

               /*
                * Save the transmission weights, they may be overwritten if the ray is reused for the next path segment
                * below.
                */
               final double throughputR = ray.throughput.r
                     * (ray.extinction.r == 0.0 ? 1.0 : Math.exp(Math.log(ray.extinction.r) * ray.intersection.t));
               final double throughputG = ray.throughput.g
                     * (ray.extinction.g == 0.0 ? 1.0 : Math.exp(Math.log(ray.extinction.g) * ray.intersection.t));
               final double throughputB = ray.throughput.b
                     * (ray.extinction.b == 0.0 ? 1.0 : Math.exp(Math.log(ray.extinction.b) * ray.intersection.t));

               /*
                * Specular and refractive materials do not benefit from direct illuminant sampling, since their
                * relfection/refraction distributions are typically constrained to a small solid angle, they only
                * respond to light coming from directions that will be sampled via bounce rays.
                */
               if (ray.intersection.material.isDiffuse()) {
                  integrateDirectIllumination(directIllumContribution, geometry, lights, ray, rng);
               }

               /*
                * Add the contribution to the pixel, modulated by the transmission across all previous bounces in this
                * path.
                */
               updateImage(ray.pixelX, ray.pixelY, throughputR * directIllumContribution.r, throughputG
                     * directIllumContribution.g, throughputB * directIllumContribution.b);

               /*
                * If we have not reached the maximum recursion depth, generate a new reflection/refraction ray for the
                * next path segment.
                */
               if (rayDepth < recursionDepth
                     && (rayDepth < 2 || rng.nextFloat() >= Math.min(0.2, 1.0 - ImageUtil.luminance((float) throughputR, (float) throughputG, (float) throughputB)))) {
                  final SampleRay irradSampleRay = rays[outRayCount];
                  /*
                   * Preserve the current extinction, this is only modified when the ray passes through a refractive
                   * interface, at which point the extinction is changed in the Material model.
                   */
                  irradSampleRay.extinction.set(ray.extinction);
                  irradSampleRay.origin.set(ray.getPointOnRay(ray.intersection.t));
                  irradSampleRay.reset();
                  final double pdf = ray.intersection.material.sampleBRDF(irradSampleRay, ray.direction.inverted(), ray.intersection, rng);
                  if (pdf > 0 && !irradSampleRay.throughput.isZero()) {
                     // Scale transmission by inverse probability of reaching this depth due to RR.
                     if (rayDepth >= 2)
                        irradSampleRay.throughput.multiply(1 / (1 - Math.min(0.2, 1.0 - ImageUtil.luminance((float) throughputR, (float) throughputG, (float) throughputB))));
                     irradSampleRay.throughput.multiply(throughputR, throughputG, throughputB);
                     irradSampleRay.throughput.multiply(Math.abs(ray.intersection.surfaceNormal.dot(irradSampleRay.direction))
                           / pdf);

                     irradSampleRay.pixelX = ray.pixelX;
                     irradSampleRay.pixelY = ray.pixelY;
                     /*
                      * Avoid precision issues when processing the ray for the next intersection.
                      */
                     irradSampleRay.origin.scaleAdd(irradSampleRay.direction, Constants.EPSILON_D);
                     ++outRayCount;

                  }
               }

            }
            rayCount = outRayCount;
            /* Process all active rays for intersection with scene geometry */
            IntegratorUtils.processHits(rays, rayCount, geometry);
         }
      }

      private final void integrateDirectIllumination(final Color irradianceOut, final Geometry[] geometry,
            final EmissiveGeometry[] lights, final SampleRay sample, final Random rng) {
         /*
          * Set the origin of the shadow ray to the hit point, but perturb by a small distance along the surface normal
          * vector to avoid self-intersecting the same point due to round-off error.
          */
         final Vec3 hitPoint = sample.getPointOnRay(sample.intersection.t).scaleAdd(sample.intersection.surfaceNormal, Constants.EPSILON_D);

         for (final EmissiveGeometry light : lights) { //TODO: select a single light based on relative emission power.
            for (int i = 0; i < illuminationRays.length; ++i) {
               illuminationRays[i].origin.set(hitPoint);
               /*
                * Generate a random sample direction that hits the light.
                * 
                * TODO: Stratified sampling...
                */
               light.sampleIrradiance(illuminationRays[i], rng);
            }
            IntegratorUtils.processHits(illuminationRays, ILLUMINATION_SAMPLES, geometry);
            for (final SampleRay illuminationRay : illuminationRays) {
               /*
                * Cosine of the angle between the geometry surface normal and the shadow ray direction
                */
               final double cosWi = illuminationRay.direction.dot(sample.intersection.surfaceNormal);
               if (cosWi > 0 && illuminationRay.intersection.hitGeometry == light) {
                  /*
                   * Compute the reflected spectrum/power by modulating the energy transmitted along the shadow ray with
                   * the response of the material...
                   */
                  sample.intersection.material.evaluateBRDF(illuminationRay.throughput, sample.direction.inverted(), illuminationRay.direction, sample.intersection);
                  irradianceOut.scaleAdd(illuminationRay.throughput.r, illuminationRay.throughput.g, illuminationRay.throughput.b, cosWi
                        / illuminationRays.length);
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