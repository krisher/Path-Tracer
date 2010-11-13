/**
 * 
 */
package edu.rit.krisher.raytracer;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.TreeSet;
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
 * Non thread-safe Photon Tracer.
 * 
 * @author krisher
 * 
 */
public final class PhotonTracer implements SceneIntegrator {

   private final Timer timer = new Timer("Ray Trace (Thread Timing)");
   private static final int MAX_PHOTON_COLLECTION = 2;
   private static final int MAX_PHOTONS = 10;

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

      final Timer photonTime = new Timer("Build Photon Map").start();
      final PhotonMapNode photonMap = computePhotonMap(scene, recursionDepth);
      photonTime.stop();
      photonTime.print();

      /*
       * Imaging parameters
       */
      final Dimension imageSize = image.getResolution();

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
         IntegratorUtils.threadPool.submit(new PhotonIntegrator(scene, image, blocks, photonMap, pixelSampleRate, recursionDepth, doneSignal));
   }

   /**
    * @param scene
    * @param recursionDepth
    */
   private PhotonMapNode computePhotonMap(final Scene scene, final int recursionDepth) {
      /*
       * TODO: Compute photon map...
       * 
       * Build KD-Tree from hits.
       * 
       * Ray Trace Scene until all rays hit a diffuse surface.
       * 
       * For all hits, compute direct illumination.
       * 
       * For all hits, generate rays that sample hemisphere.
       * 
       * For all sample ray hits, gather k nearest photons in photon map, weight with RBF, and contribute illumination
       * to sample pixel.
       */
      final Random rng = new UnsafePRNG();
      final EmissiveGeometry[] lights = scene.getLightSources();
      final Geometry[] geometry = scene.getGeometry();
      final Photon[] photons = new Photon[MAX_PHOTONS];
      int photonCount = 0;
      final SampleRay[] photonPaths = new SampleRay[Math.max(MAX_PHOTONS/20, 10)];
      for (int i = 0; i < photonPaths.length; ++i) {
         photonPaths[i] = new SampleRay(1.0);
      }

      int totalPaths = 0;
      while (photonCount <= MAX_PHOTONS - photonPaths.length) {
         for (final EmissiveGeometry light : lights) {
            // TODO: probabalistic selection of light source based on total emitted power.
            /*
             * Sample the surface area and conditional emissive direction of the light. (generate at most one ray
             */
            int sampleCount = Math.min(photonPaths.length, MAX_PHOTONS - photonCount); /*
             * Max paths to trace at once
             * bounded by remaining photon
             * budget.
             */
            sampleCount = light.sampleEmission(photonPaths, 0, sampleCount, rng);
            totalPaths += sampleCount;
            /*
             * Trace light rays and store a photon when it hits a diffuse surface.
             */
            IntegratorUtils.processHits(photonPaths, sampleCount, geometry);

            for (int rayDepth = 0; rayDepth < recursionDepth && sampleCount > 0
            && MAX_PHOTONS >= sampleCount + photonCount; ++rayDepth) {
               int outRayCount = 0;
               for (int i = 0; i < sampleCount; ++i) {
                  final SampleRay ray = photonPaths[i];
                  if (ray.intersection.hitGeometry != null) {

                     /*
                      * Save the transmission weights, they may be overwritten if the ray is reused for the next path
                      * segment below.
                      */
                     final double throughputR = ray.throughput.r
                     * (ray.extinction.r == 0.0 ? 1.0 : Math.exp(Math.log(ray.extinction.r) * ray.intersection.t));
                     final double throughputG = ray.throughput.g
                     * (ray.extinction.g == 0.0 ? 1.0 : Math.exp(Math.log(ray.extinction.g) * ray.intersection.t));
                     final double throughputB = ray.throughput.b
                     * (ray.extinction.b == 0.0 ? 1.0 : Math.exp(Math.log(ray.extinction.b) * ray.intersection.t));

                     final Vec3 hitPoint = ray.getPointOnRay(ray.intersection.t);
                     if (ray.intersection.material.isDiffuse()) {
                        /* Store a photon when the light path hits a diffuse surface */
                        final Photon photon = new Photon();
                        photon.x = (float) hitPoint.x;
                        photon.y = (float) hitPoint.y;
                        photon.z = (float) hitPoint.z;
                        photon.dx = (float) ray.direction.x;
                        photon.dy = (float) ray.direction.y;
                        photon.dz = (float) ray.direction.z;

                        photon.powerR = (float) throughputR;
                        photon.powerG = (float) throughputG;
                        photon.powerB = (float) throughputB;
                        photons[photonCount++] = photon;
                     }

                     /*
                      * Compute next bounce.
                      */
                     /*
                      * If we have not reached the maximum recursion depth, generate a new reflection/refraction ray for
                      * the
                      * next path segment.
                      */
                     if (rayDepth < recursionDepth
                           && (rayDepth < 2 || rng.nextFloat() >= Math.min(1.0 / (recursionDepth + 1), 1.0 - ImageUtil.luminance((float) throughputR, (float) throughputG, (float) throughputB)))) {
                        final SampleRay bounceRay = photonPaths[outRayCount];
                        /*
                         * Preserve the current extinction, this is only modified when the ray passes through a
                         * refractive
                         * interface, at which point the extinction is changed in the Material model.
                         */
                        bounceRay.extinction.set(ray.extinction);
                        bounceRay.origin.set(hitPoint);
                        bounceRay.reset();
                        final double pdf = ray.intersection.material.sampleBRDF(bounceRay, ray.direction.inverted(), ray.intersection, rng);
                        if (pdf > 0 && !bounceRay.throughput.isZero()) {
                           // Scale transmission by inverse probability of reaching this depth due to RR.
                           if (rayDepth >= 2)
                              bounceRay.throughput.multiply(1 / (1 - Math.min(1.0 / (recursionDepth + 1), 1.0 - ImageUtil.luminance((float) throughputR, (float) throughputG, (float) throughputB))));
                           bounceRay.throughput.multiply(throughputR, throughputG, throughputB);
                           bounceRay.throughput.multiply(Math.abs(ray.intersection.surfaceNormal.dot(bounceRay.direction))
                                                         / pdf);

                           bounceRay.pixelX = ray.pixelX;
                           bounceRay.pixelY = ray.pixelY;
                           /*
                            * Avoid precision issues when processing the ray for the next intersection.
                            */
                           bounceRay.origin.scaleAdd(bounceRay.direction, Constants.EPSILON_D);
                           ++outRayCount;

                        }
                     }
                  }
               }
               sampleCount = outRayCount;
            }
         }
      }

      /*
       * Build KD Tree
       */
      for (final Photon photon : photons) {
         if (photon == null) break;
         photon.powerR /= totalPaths;
         photon.powerG /= totalPaths;
         photon.powerB /= totalPaths;
      }
      System.out.println("Total photons: " + photonCount);
      return buildPhotonMap(photons, photonCount);
   }

   private static class PhotonMapNode {
      final int splitAxis;
      final PhotonMapNode lessChild;
      final PhotonMapNode geChild;
      final Photon splitPhoton;

      /**
       * @param splitAxis
       * @param splitLocation
       * @param leftChild
       * @param rightChild
       */
      public PhotonMapNode(final int splitAxis, final Photon splitLocation, final PhotonMapNode leftChild,
            final PhotonMapNode rightChild) {
         super();
         this.splitAxis = splitAxis;
         this.splitPhoton = splitLocation;
         this.lessChild = leftChild;
         this.geChild = rightChild;
      }

      public double findPhotons(final PhotonHandler handler, final double[] point, double maxDistSq) {
         final double splitLocation = splitAxis == 0 ? splitPhoton.x : (splitAxis == 1 ? splitPhoton.y : splitPhoton.z);
         /*
          * Distance to split plane...
          */
         final float distToSPSq = (float) ((point[splitAxis] - splitLocation) * (point[splitAxis] - splitLocation));
         if (point[splitAxis] < splitLocation) {
            if (lessChild != null)
               maxDistSq = lessChild.findPhotons(handler, point, maxDistSq);
            if (distToSPSq <= maxDistSq && geChild != null) /*
             * Only traverse >= child if dist from point to split plane
             * is <= maxDist
             */
               maxDistSq = geChild.findPhotons(handler, point, maxDistSq);
         } else {
            if (geChild != null)
               maxDistSq = geChild.findPhotons(handler, point, maxDistSq);
            if (distToSPSq <= maxDistSq && lessChild != null) /*
             * Only traverse < child if dist from point to split plane
             * is <= maxDist
             */
               maxDistSq = lessChild.findPhotons(handler, point, maxDistSq);
         }

         /*
          * If distance to photon is less than max, invoke callback with photon.
          */
         final double dX = point[0] - splitPhoton.x;
         final double dY = point[1] - splitPhoton.y;
         final double dZ = point[2] - splitPhoton.z;
         final double distToPhotonSq = dX * dX + dY * dY + dZ * dZ;
         if (distToPhotonSq < maxDistSq)
            return handler.processPhoton(splitPhoton, point, distToPhotonSq, maxDistSq);
         return maxDistSq;
      }

   }

   private static interface PhotonHandler {
      /**
       * 
       * @param photon
       * @param samplePoint
       * @param dist2
       * @param maxDist2
       * @return A replacement for maxDist2.
       */
      public double processPhoton(Photon photon, double[] samplePoint, double dist2, double maxDist2);
   }

   private static final PhotonMapNode buildPhotonMap(final Photon[] photons, final int count) {
      return buildPhotonNodeRecursive(photons, 0, count);
   }

   private static final PhotonMapNode buildPhotonNodeRecursive(final Photon[] photons, final int offs, final int count) {
      if (count < 1)
         return null;
      if (count == 1)
         return new PhotonMapNode(0, photons[offs], null, null);
      final Vec3 minValues = new Vec3(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
      final Vec3 maxValues = new Vec3(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
      for (int i = 0; i < count; ++i) {
         final Photon p = photons[i + offs];
         if (p == null)
            System.out.println("NULL!");
         minValues.componentMinimums(p.x, p.y, p.z);
         maxValues.componentMaximums(p.x, p.y, p.z);
      }
      maxValues.subtract(minValues);

      int splitAxis;
      if (maxValues.x > maxValues.y) {
         if (maxValues.x > maxValues.z) {
            /* Split x */
            splitAxis = 0;
            // Arrays.sort(photons, offs, offs + count, photonXComparator);
         } else {
            /* Split z */
            splitAxis = 2;
            // Arrays.sort(photons, offs, offs + count, photonZComparator);
         }
      } else {
         if (maxValues.y > maxValues.z) {
            /* Split y */
            splitAxis = 1;
            // Arrays.sort(photons, offs, offs + count, photonYComparator);
         } else {
            /* Split z */
            splitAxis = 2;
            // Arrays.sort(photons, offs, offs + count, photonZComparator);
         }
      }

      final int mid = count / 2;

      return new PhotonMapNode(splitAxis, photons[mid], buildPhotonNodeRecursive(photons, offs, mid), buildPhotonNodeRecursive(photons, offs
                                                                                                                               + mid + 1, count - mid - 1));
   }

   private static final Comparator photonXComparator = new Comparator<Photon>() {

      @Override
      public int compare(final Photon o1, final Photon o2) {
         if (o1.x < o2.x)
            return -1;
         if (o1.x > o2.x)
            return 1;
         return 0;
      }

   };
   private static final Comparator photonYComparator = new Comparator<Photon>() {

      @Override
      public int compare(final Photon o1, final Photon o2) {
         if (o1.y < o2.y)
            return -1;
         if (o1.y > o2.y)
            return 1;
         return 0;
      }

   };
   private static final Comparator photonZComparator = new Comparator<Photon>() {

      @Override
      public int compare(final Photon o1, final Photon o2) {
         if (o1.z < o2.z)
            return -1;
         if (o1.z > o2.z)
            return 1;
         return 0;
      }

   };

   private static final class PhotonIntegrator implements Runnable, PhotonHandler {
      private static final int ILLUMINATION_SAMPLES = 4;
      private static final double gaussFalloffControl = 1;
      private static final double gaussFalloffConstant = Math.exp(-gaussFalloffControl * 0.5 * 0.5);

      /**
       * Overridden to remove thread safety overhead
       */
      private final Random rng = new UnsafePRNG();

      private final int pixelSampleRate;
      private final int recursionDepth;
      private final ImageBuffer imageBuffer;
      private final Scene scene;
      private final Queue<Rectangle> workQueue;
      private final AtomicInteger doneSignal;
      private final SampleRay[] illuminationRays = new SampleRay[ILLUMINATION_SAMPLES];
      private final PhotonMapNode photonMap;

      private static class CollectedPhoton implements Comparable<CollectedPhoton> {
         final Photon photon;
         final double distSq;

         public CollectedPhoton(final Photon photon, final double distSq) {
            this.photon = photon;
            this.distSq = distSq;
         }

         @Override
         public int compareTo(final edu.rit.krisher.raytracer.PhotonTracer.PhotonIntegrator.CollectedPhoton o) {
            if (distSq < o.distSq)
               return -1;
            if (distSq > o.distSq)
               return 1;
            return 0;
         }

      }

      private final TreeSet<CollectedPhoton> photonCollection = new TreeSet<CollectedPhoton>();

      /*
       * Buffer to collect rgb pixel data
       * 
       * Values will always be >= 0, but are unbounded in magnitude.
       */
      private float[] pixels;
      private float[] pixelNormalization;
      private Rectangle rect;

      public PhotonIntegrator(final Scene scene, final ImageBuffer image, final Queue<Rectangle> workQueue,
            final PhotonMapNode photonMap, final int pixelSampleRate, final int recursionDepth,
            final AtomicInteger doneSignal) {
         this.recursionDepth = recursionDepth;
         this.imageBuffer = image;
         this.scene = scene;
         this.doneSignal = doneSignal;
         this.workQueue = workQueue;
         this.pixelSampleRate = pixelSampleRate;
         for (int i = 0; i < illuminationRays.length; ++i) {
            illuminationRays[i] = new SampleRay(1.0);
         }
         this.photonMap = photonMap;
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
                     rays[i].specularBounce = true;
                     rays[i].extinction.clear();
                  }
               }

               /* Generate Eye Rays */
               SamplingUtils.generatePixelSamples(rays, new Rectangle(0, 0, rect.width, rect.height), pixelSampleRate, rng);
               scene.getCamera().sample(rays, imageSize.width, imageSize.height, rect.x, rect.y, rng);

               /*
                * Compute filter normalization constants for each pixel.
                */
               for (final SampleRay ray : rays) {
                  final int dst = (((int) ray.pixelY) * rect.width + (int) ray.pixelX);
                  final double x = ray.pixelX - (int) ray.pixelX - 0.5;
                  final double y = ray.pixelY - (int) ray.pixelY - 0.5;
                  final double filter = 1;
                  // Math.max(0, Math.exp(-gaussFalloffControl * x * x) - gaussFalloffConstant)
                  // * Math.max(0, Math.exp(-gaussFalloffControl * y * y) - gaussFalloffConstant);
                  ray.throughput.set(filter);
                  pixelNormalization[dst] += filter;
               }

               /* Visibility pass */
               IntegratorUtils.processHits(rays, rayCount, scene.getGeometry());

               /* Trace Rays */
               integrateIrradiance(rect, rays, rays.length);

               /* Put results back into image buffer */
               for (int i = 0; i < pixelNormalization.length; ++i) {
                  final int pixOffs = 3 * i;
                  pixels[pixOffs] /= pixelNormalization[i];
                  pixels[pixOffs + 1] /= pixelNormalization[i];
                  pixels[pixOffs + 2] /= pixelNormalization[i];
               }
               imageBuffer.setPixels(rect.x, rect.y, rect.width, rect.height, pixels);
            } catch (final Throwable e) {
               e.printStackTrace();
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

      @Override
      public double processPhoton(final Photon photon, final double[] samplePoint, final double dist2,
            final double maxDist2) {
         photonCollection.add(new CollectedPhoton(photon, dist2));
         if (photonCollection.size() > MAX_PHOTON_COLLECTION) {
            photonCollection.remove(photonCollection.last());
            return photonCollection.last().distSq;
         } else {
            return maxDist2;
         }
      }

      private final void updateImage(final int x, final int y, final double r, final double g, final double b) {
         final int dst = 3 * (y * rect.width + x);
         pixels[dst] += r;
         pixels[dst + 1] += g;
         pixels[dst + 2] += b;
      }

      private final void integrateIrradiance(final Rectangle rect, final SampleRay[] rays, int rayCount) {

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
                  updateImage((int) ray.pixelX, (int) ray.pixelY, bg.r * ray.throughput.r, bg.g * ray.throughput.g, bg.b
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
               if (ray.specularBounce) {
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
                * Sample direct illumination at diffuse intersections.
                * 
                * XXX Merge with PathTracer conflict
                */
               if (ray.intersection.material.isDiffuse()) {
                  /*
                   * Integrate contribution from direct illumination for the first hit only.
                   */
                  if (false && rayDepth == 0) {
                     final int samples = IntegratorUtils.sampleDirectIllumination(illuminationRays, ray.getPointOnRay(ray.intersection.t).scaleAdd(ray.intersection.surfaceNormal, Constants.EPSILON_D), lights, geometry, rng);
                     final float sampleNorm = 1f / samples;

                     for (int i = 0; i < samples; ++i) {
                        final SampleRay illuminationRay = illuminationRays[i];
                        if (illuminationRay.intersection.hitGeometry == null)
                           continue;
                        /*
                         * Cosine of the angle between the geometry surface normal and the shadow ray direction
                         */
                        final double cosWi = illuminationRay.direction.dot(ray.intersection.surfaceNormal);
                        if (cosWi > 0) {
                           /*
                            * Compute the reflected spectrum/power by modulating the energy transmitted along the shadow
                            * ray with the response of the material...
                            */
                           ray.intersection.material.evaluateBRDF(illuminationRay.throughput, ray.direction.inverted(), illuminationRay.direction, ray.intersection);
                           directIllumContribution.scaleAdd(illuminationRays[i].throughput, cosWi * sampleNorm);
                        }
                     }
                  }
                  /*
                   * TODO: Sample photon map for indirect lighting contribution.
                   */
                  photonCollection.clear();
                  photonMap.findPhotons(this, ray.getPointOnRay(ray.intersection.t).get(), Double.POSITIVE_INFINITY);
                  final double maxDistSq = photonCollection.last().distSq;
                  double minDist = Math.sqrt(photonCollection.first().distSq);
                  // System.out.println("Min/Max dist: " + minDist + " / " + Math.sqrt(maxDistSq));

                  //DEBUG...
                  if (minDist < 1) {
                     minDist = (1.0 - minDist / 1.0);
                     updateImage((int) ray.pixelX, (int) ray.pixelY, minDist, minDist, minDist);
                  }
                  //                  final int nPhotons = photonCollection.size();
                  //                  final Color photonColor = new Color(0);
                  //                  for (final CollectedPhoton photon : photonCollection) {
                  //                     final double cosWi = ray.intersection.surfaceNormal.dot(-photon.photon.dx, -photon.photon.dy, -photon.photon.dz);
                  //                     if (cosWi > 0) {
                  //                           /*
                  //                            * Compute the reflected spectrum/power by modulating the energy transmitted along the shadow
                  //                            * ray with the response of the material...
                  //                            */
                  //                           photonColor.set(photon.photon.powerR, photon.photon.powerG, photon.photon.powerB);
                  //                           ray.intersection.material.evaluateBRDF(photonColor, ray.direction.inverted(), new Vec3(-photon.photon.dx, -photon.photon.dy, -photon.photon.dz), ray.intersection);
                  //                           directIllumContribution.scaleAdd(photonColor, cosWi );
                  //                        }
                  //                  }
               }

               /*
                * Add the contribution to the pixel, modulated by the transmission across all previous bounces in this
                * path.
                */
               //               updateImage((int) ray.pixelX, (int) ray.pixelY, throughputR * directIllumContribution.r, throughputG
               //                     * directIllumContribution.g, throughputB * directIllumContribution.b);

               /*
                * If we have not reached the maximum recursion depth, generate a new reflection/refraction ray for the
                * next path segment.
                * 
                * XXX Merge with PathTracer conflict
                */
               if (!ray.intersection.material.isDiffuse()
                     && rayDepth < recursionDepth
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
   }

   private static final class Photon {
      float x;
      float y;
      float z;

      float dx;
      float dy;
      float dz;

      float powerR;
      float powerG;
      float powerB;
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