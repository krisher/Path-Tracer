package edu.rit.krisher.raytracer;

import java.awt.Rectangle;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.NumberFormat;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.rit.krisher.raytracer.rays.IntersectionInfo;
import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.scene.Geometry;

/**
 * Utility methods for ray tracing.
 * 
 * @author krisher
 * 
 */
public final class IntegratorUtils  {
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
    * Processes intersections of each of the 'count' rays with the specified scene geometry. Upon completion,
    * the {@link IntersectionInfo} for each ray will be updated indicating the geometry that was hit (or null if nothing
    * was hit),
    * the primitiveID of the hit geometry, and the parametric hit location.
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
         ray.intersection.t = Double.POSITIVE_INFINITY;
         ray.intersection.hitGeometry = null;
         for (final Geometry geom : geometry) {
            geom.intersects(ray, ray.intersection);
         }
      }
   }
   
   /**
    * Processes intersections of each of the 'count' rays with the specified scene geometry. Upon completion,
    * the {@link IntersectionInfo} for each ray will be updated indicating the geometry that was hit (or null if nothing
    * was hit), the primitiveID of the hit geometry, and the parametric hit location.
    * <p>
    * This method differs from {@link #processIntersections(SampleRay[], int, Geometry[])} in that when an intersection is found, the 
    * material information for each ray is computed and stored in {@link SampleRay#intersection}
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
         ray.intersection.t = Double.POSITIVE_INFINITY;
         ray.intersection.hitGeometry = null;
         for (final Geometry geom : geometry) {
            geom.intersects(ray, ray.intersection);
         }
         if (ray.intersection.hitGeometry != null) {
            ray.intersection.hitGeometry.getHitData(ray, ray.intersection);
         }
      }
   }

}
