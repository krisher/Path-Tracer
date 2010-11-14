package edu.rit.krisher.raytracer;

import java.awt.Rectangle;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.NumberFormat;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.rit.krisher.raytracer.rays.IntersectionInfo;
import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.scene.EmissiveGeometry;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.vecmath.Vec3;

/**
 * Utility methods for ray tracing.
 * 
 * @author krisher
 * 
 */
public final class IntegratorUtils {
   public static final class DirectIlluminationSampler {
      private final SampleRay[] sampleRays;
      private final Random rng;

      public DirectIlluminationSampler(final int sampleCount, final Random rng) {
         this.rng = rng;
         sampleRays = new SampleRay[sampleCount];
         for (int i = 0; i < sampleRays.length; ++i) {
            sampleRays[i] = new SampleRay(1.0);
         }
      }

      public final void sampleDirectIllumination(final Vec3 hitPoint, final IntersectionInfo hitInfo, final Vec3 wo,
            final Color directIllumContribution, final EmissiveGeometry[] lights,
            final Geometry[] geometry) {

         // TODO: select a single light based on relative emission power.
         final EmissiveGeometry light = lights[rng.nextInt(lights.length)];
         final int samples = light.sampleIrradiance(sampleRays, hitPoint, rng);
         processObstructions(sampleRays, samples, geometry);

         final float sampleNorm = 1f / samples;

         for (int i = 0; i < samples; ++i) {
            final SampleRay illuminationRay = sampleRays[i];
            if (illuminationRay.hitGeometry == null)
               continue;
            /*
             * Cosine of the angle between the geometry surface normal and the shadow ray direction
             */
            final double cosWi = illuminationRay.direction.dot(hitInfo.surfaceNormal);
            if (cosWi > 0) {
               /*
                * Compute the reflected spectrum/power by modulating the energy transmitted along the shadow ray with
                * the response of the material...
                */
               hitInfo.material.evaluateBRDF(illuminationRay.throughput, wo, illuminationRay.direction, hitInfo);
               directIllumContribution.scaleAdd(sampleRays[i].throughput, cosWi * sampleNorm);
            }
         }
      }
   }

   public static final int DEFAULT_PIXEL_BLOCK_SIZE = 8;
   public static final int threads = Runtime.getRuntime().availableProcessors();
   public static final NumberFormat formatter = NumberFormat.getNumberInstance();
   public static final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

   static {
      Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

         @Override
         public void uncaughtException(final Thread t, final Throwable e) {
            e.printStackTrace();
         }
      });
   }

   private IntegratorUtils() {
      /*
       * Prevent construction.
       */
   }

   public static Rectangle[] chunkRectangle(final int width, final int height, final int blockSize) {
      /*
       * Create ray processors.
       */
      final int xBlocks = (int) Math.ceil((width / (double) blockSize));
      final int yBlocks = (int) Math.ceil((height / (double) blockSize));
      final Rectangle[] result = new Rectangle[xBlocks * yBlocks];
      /*
       * Tiled work distribution...
       */
      for (int j = 0; j < yBlocks; j++) {
         final int blockStartY = j * blockSize;
         for (int i = 0; i < xBlocks; i++) {
            final int blockStartX = i * blockSize;
            result[j * xBlocks + i] = new Rectangle(blockStartX, blockStartY, Math.min(blockSize, width - blockStartX), Math.min(blockSize, height
                                                                                                                                 - blockStartY));
         }
      }
      return result;
   }

   /**
    * Processes intersections of each of the 'count' rays with the specified scene geometry. Upon completion, the
    * {@link IntersectionInfo} for each ray will be updated indicating the geometry that was hit (or null if nothing was
    * hit), the primitiveID of the hit geometry, and the parametric hit location.
    * 
    * @param rays
    *           A non-null array of at least 'count' non-null SampleRays. The origin and direction of the rays must be
    *           initialized prior to this call.
    * @param count
    *           The first 'count' rays in the rays array are processed for intersection.
    * @param geometry
    *           The non-null list of geometry to test for intersection.
    */
   public static void processIntersections(final SampleRay[] rays, final int count, final Geometry[] geometry) {
      for (int i = 0; i < count; ++i) {
         final SampleRay ray = rays[i];
         ray.t = Double.POSITIVE_INFINITY;
         ray.hitGeometry = null;
         for (final Geometry geom : geometry) {
            geom.intersects(ray);
         }
      }
   }

   /**
    * Processes intersections of each of the 'count' rays with the specified scene geometry. Upon completion, the
    * {@link IntersectionInfo} for each ray will be updated indicating the geometry that was hit (or null if nothing was
    * hit), the primitiveID of the hit geometry, and the parametric hit location.
    * <p>
    * This method differs from {@link #processIntersections(SampleRay[], int, Geometry[])} in that when an intersection
    * is found, the material information for each ray is computed and stored in {@link SampleRay#intersection}
    * 
    * @param rays
    *           A non-null array of at least 'count' non-null SampleRays. The origin and direction of the rays must be
    *           initialized prior to this call.
    * @param count
    *           The first 'count' rays in the rays array are processed for intersection.
    * @param geometry
    *           The non-null list of geometry to test for intersection.
    */
   public static void processHits(final SampleRay[] rays, final int count, final Geometry[] geometry) {
      for (int i = 0; i < count; ++i) {
         final SampleRay ray = rays[i];
         ray.t = Double.POSITIVE_INFINITY;
         ray.hitGeometry = null;
         for (final Geometry geom : geometry) {
            geom.intersects(ray);
         }
         if (ray.hitGeometry != null) {
            ray.hitGeometry.getHitData(ray, ray.intersection);
         }
      }
   }

   /**
    * Assuming that all sample rays have been initialized and traced to an intersection with some object. For rays where
    * a closer intersection is found, the ray.intersection.hitGeometry is set to null to indicate an obstruction.
    * 
    * @param rays
    *           A non-null array of at least 'count' non-null SampleRays. The origin and direction of the rays must be
    *           initialized prior to this call.
    * @param count
    *           The first 'count' rays in the rays array are processed for intersection.
    * @param geometry
    *           The non-null list of geometry to test for intersection with any other object in the scene between the
    *           ray origin and intersection point.
    */
   public static void processObstructions(final SampleRay[] rays, final int count,
         final Geometry[] geometry) {
      for (int i = 0; i < count; ++i) {
         final SampleRay ray = rays[i];
         final Geometry original = ray.hitGeometry;
         for (final Geometry geom : geometry) {
            if (geom != original && geom.intersectsP(ray)) {
               ray.hitGeometry = null;
               break;
            }
         }
      }
   }

   /**
    * Samples the contribution of light from emissive sources to a specified point. Upon completion, the SampleRays
    * in the illuminationRays parameter are initialized to rays from the hit point to a sample point on an emissive
    * geometric object. These rays are traced to determine whether the hit point is visible from each light's sample
    * points. Where there is an obstruction, the hitGeometry of the SampleRay is null, otherwise it is the light that
    * was sampled.
    * 
    * @param illuminationRays
    *           A list of 1 or more rays to be initialized to samples of emissive geometry.
    * @param hitPoint
    *           The point to sample for irradiance.
    * @param lights
    *           The list of all emissive geometry to sample.
    * @param geometry
    *           The list of all geometry that may obstruct light.
    * @param rng
    *           A random number generator.
    * @return The number of SampleRays actually initialized.
    */
   public static final int sampleDirectIllumination(final SampleRay[] illuminationRays, final Vec3 hitPoint,
         final EmissiveGeometry[] lights, final Geometry[] geometry, final Random rng) {
      // TODO: select a single light based on relative emission power.
      final EmissiveGeometry light = lights[rng.nextInt(lights.length)];
      final int sampleCount = light.sampleIrradiance(illuminationRays, hitPoint, rng);
      processObstructions(illuminationRays, sampleCount, geometry);
      return sampleCount;
   }
}
